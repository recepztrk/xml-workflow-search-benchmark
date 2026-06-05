package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.service;

import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkMeasurementResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkReindexResponse;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkRunRequest;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkRunResponse;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.client.SearchEngineClient;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch, OpenSearch ve Solr için ortak benchmark runner.
 *
 * Bu servis:
 * - Search engine client'larını ortak interface üzerinden yönetir.
 * - Aynı query setini seçilen engine'lere gönderir.
 * - Warm-up iteration çalıştırır.
 * - Measurement iteration çalıştırır.
 * - avg, min, max, p50, p95, p99 değerlerini hesaplar.
 */
@Service
@RequiredArgsConstructor
public class BenchmarkService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int DEFAULT_WARMUP_ITERATIONS = 5;
    private static final int DEFAULT_MEASUREMENT_ITERATIONS = 30;
    private static final SearchMode DEFAULT_SEARCH_MODE = SearchMode.RAW_XML;

    private static final List<String> DEFAULT_QUERIES = List.of(
            "fatura itiraz",
            "müşteri bilgileri",
            "ödeme durumu",
            "abonelik iptal",
            "arıza kaydı"
    );

    private final List<SearchEngineClient> searchEngineClients;

    public List<String> getAvailableEngines() {
        return searchEngineClients.stream()
                .map(SearchEngineClient::engineName)
                .sorted()
                .toList();
    }

    public BenchmarkReindexResponse reindexAll() {
        return reindexAll(DEFAULT_SEARCH_MODE);
    }

    public BenchmarkReindexResponse reindexAll(SearchMode mode) {
        SearchMode effectiveMode = mode == null ? DEFAULT_SEARCH_MODE : mode;

        List<IndexOperationResult> results = searchEngineClients.stream()
                .map(client -> client.reindexAll(effectiveMode))
                .toList();

        return new BenchmarkReindexResponse(
                LocalDateTime.now(),
                results
        );
    }

    public BenchmarkRunResponse runBenchmark(BenchmarkRunRequest request) {
        BenchmarkRunRequest normalizedRequest = normalizeRequest(request);

        int limit = normalizedRequest.limit();
        int warmupIterations = normalizedRequest.warmupIterations();
        int measurementIterations = normalizedRequest.measurementIterations();

        List<SearchEngineClient> selectedClients = resolveClients(normalizedRequest.engines());

        List<BenchmarkMeasurementResult> results = new ArrayList<>();

        for (SearchEngineClient client : selectedClients) {
            for (String query : normalizedRequest.queries()) {
                BenchmarkMeasurementResult measurementResult = measureSingleQuery(
                        client,
                        query,
                        limit,
                        warmupIterations,
                        measurementIterations,
                        normalizedRequest.mode()
                );

                results.add(measurementResult);
            }
        }

        return new BenchmarkRunResponse(
                LocalDateTime.now(),
                selectedClients.stream().map(SearchEngineClient::engineName).toList(),
                normalizedRequest.queries(),
                limit,
                warmupIterations,
                measurementIterations,
                normalizedRequest.mode(),
                results
        );
    }

    private BenchmarkMeasurementResult measureSingleQuery(
            SearchEngineClient client,
            String query,
            int limit,
            int warmupIterations,
            int measurementIterations,
            SearchMode mode
    ) {
        /*
         * Warm-up:
         * İlk sorgular JVM, cache ve search engine internal warming etkisi taşıyabilir.
         * Bu yüzden warm-up sonuçlarını ölçüme dahil etmiyoruz.
         */
        for (int i = 0; i < warmupIterations; i++) {
            try {
                client.search(query, limit, mode);
            } catch (Exception ignored) {
                // Warm-up hataları ölçüme dahil edilmiyor.
            }
        }

        List<Long> samples = new ArrayList<>();
        int errorCount = 0;
        int lastHitCount = 0;
        String lastErrorMessage = null;

        for (int i = 0; i < measurementIterations; i++) {
            try {
                SearchEngineResult result = client.search(query, limit, mode);
                samples.add(result.tookMs());
                lastHitCount = result.hitCount();
            } catch (Exception exception) {
                errorCount++;
                lastErrorMessage = extractErrorMessage(exception);
            }
        }

        if (samples.isEmpty()) {
            return new BenchmarkMeasurementResult(
                    client.engineName(),
                    query,
                    limit,
                    warmupIterations,
                    measurementIterations,
                    0,
                    errorCount,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    lastErrorMessage,
                    List.of()
            );
        }

        List<Long> sortedSamples = samples.stream()
                .sorted()
                .toList();

        double avg = samples.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        long min = sortedSamples.getFirst();
        long max = sortedSamples.getLast();

        return new BenchmarkMeasurementResult(
                client.engineName(),
                query,
                limit,
                warmupIterations,
                measurementIterations,
                samples.size(),
                errorCount,
                lastHitCount,
                round(avg),
                min,
                max,
                percentile(sortedSamples, 50),
                percentile(sortedSamples, 95),
                percentile(sortedSamples, 99),
                lastErrorMessage,
                samples
        );
    }

    private BenchmarkRunRequest normalizeRequest(BenchmarkRunRequest request) {
        if (request == null) {
            return new BenchmarkRunRequest(
                    null,
                    DEFAULT_QUERIES,
                    DEFAULT_LIMIT,
                    DEFAULT_WARMUP_ITERATIONS,
                    DEFAULT_MEASUREMENT_ITERATIONS,
                    DEFAULT_SEARCH_MODE
            );
        }

        List<String> engines = request.engines();
        List<String> queries = request.queries();

        if (queries == null || queries.isEmpty()) {
            queries = DEFAULT_QUERIES;
        }

        int limit = request.limit() == null || request.limit() <= 0
                ? DEFAULT_LIMIT
                : request.limit();

        int warmupIterations = request.warmupIterations() == null || request.warmupIterations() < 0
                ? DEFAULT_WARMUP_ITERATIONS
                : request.warmupIterations();

        int measurementIterations = request.measurementIterations() == null || request.measurementIterations() <= 0
                ? DEFAULT_MEASUREMENT_ITERATIONS
                : request.measurementIterations();

        SearchMode mode = request.mode() == null
                ? DEFAULT_SEARCH_MODE
                : request.mode();

        return new BenchmarkRunRequest(
                engines,
                queries,
                limit,
                warmupIterations,
                measurementIterations,
                mode
        );
    }

    private List<SearchEngineClient> resolveClients(List<String> requestedEngines) {
        Map<String, SearchEngineClient> clientMap = searchEngineClients.stream()
                .collect(Collectors.toMap(
                        client -> normalizeEngineName(client.engineName()),
                        client -> client
                ));

        if (requestedEngines == null || requestedEngines.isEmpty()) {
            return searchEngineClients.stream()
                    .sorted(Comparator.comparing(SearchEngineClient::engineName))
                    .toList();
        }

        List<SearchEngineClient> selectedClients = new ArrayList<>();

        for (String requestedEngine : requestedEngines) {
            String normalizedName = normalizeEngineName(requestedEngine);
            SearchEngineClient client = clientMap.get(normalizedName);

            if (client == null) {
                throw new IllegalArgumentException(
                        "Bilinmeyen search engine: " + requestedEngine +
                                ". Kullanılabilir engine'ler: " + clientMap.keySet()
                );
            }

            selectedClients.add(client);
        }

        return selectedClients;
    }

    private String normalizeEngineName(String engineName) {
        return engineName == null
                ? ""
                : engineName.trim().toLowerCase(Locale.ROOT);
    }

    private String extractErrorMessage(Exception exception) {
        if (exception == null) {
            return null;
        }

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        /*
         * Search engine hataları bazen çok uzun JSON payload döndürebilir.
         * Benchmark response'unun okunabilir kalması için mesajı kısaltıyoruz.
         */
        int maxLength = 1000;

        if (message.length() > maxLength) {
            return message.substring(0, maxLength) + "...";
        }

        return message;
    }

    /**
     * Nearest-rank percentile hesabı.
     *
     * Örnek:
     * p95 için sorted listede ceil(0.95 * n) - 1 indexi alınır.
     */
    private double percentile(List<Long> sortedSamples, double percentile) {
        if (sortedSamples.isEmpty()) {
            return 0;
        }

        int index = (int) Math.ceil((percentile / 100.0) * sortedSamples.size()) - 1;
        index = Math.max(0, Math.min(index, sortedSamples.size() - 1));

        return sortedSamples.get(index);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
