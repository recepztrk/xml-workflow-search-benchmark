package com.recepoztrk.xmlworkflowsearchbenchmark.migration.service;

import com.recepoztrk.xmlworkflowsearchbenchmark.migration.model.CandidateMigrationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.migration.model.MigrationEvaluationRequest;
import com.recepoztrk.xmlworkflowsearchbenchmark.migration.model.MigrationEvaluationResponse;
import com.recepoztrk.xmlworkflowsearchbenchmark.migration.model.QueryParityResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.client.SearchEngineClient;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.ResponseMode;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchHitDto;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/**
 * Elasticsearch baseline'ına göre aday search engine'lerin migration uyumluluğunu ölçer.
 * <p>
 * Bu servis latency benchmarkından farklı olarak şunu cevaplar:
 * "Aday engine Elasticsearch yerine geçerse aynı/similar workflow sonuçlarını döndürüyor mu?"
 */
@Service
@RequiredArgsConstructor
public class MigrationEvaluationService {

    private static final String DEFAULT_BASELINE_ENGINE = "elasticsearch";
    private static final int DEFAULT_LIMIT = 5;
    private static final SearchMode DEFAULT_SEARCH_MODE = SearchMode.RAW_XML;
    private static final ResponseMode DEFAULT_RESPONSE_MODE = ResponseMode.METADATA_ONLY;

    private static final List<String> DEFAULT_QUERIES = List.of(
            "fatura itiraz",
            "müşteri bilgileri",
            "ödeme durumu",
            "abonelik iptal",
            "arıza kaydı"
    );

    private final List<SearchEngineClient> searchEngineClients;

    public MigrationEvaluationResponse evaluate(MigrationEvaluationRequest request) {
        NormalizedRequest normalizedRequest = normalizeRequest(request);

        SearchEngineClient baselineClient = resolveClient(normalizedRequest.baselineEngine());
        List<SearchEngineClient> candidateClients = normalizedRequest.candidateEngines().stream()
                .map(this::resolveClient)
                .toList();

        Map<String, BaselineQueryResult> baselineResults = runBaselineQueries(
                baselineClient,
                normalizedRequest.queries(),
                normalizedRequest.limit(),
                normalizedRequest.mode(),
                normalizedRequest.responseMode()
        );

        List<CandidateMigrationResult> candidateResults = candidateClients.stream()
                .map(candidateClient -> evaluateCandidate(
                        candidateClient,
                        baselineResults,
                        normalizedRequest.queries(),
                        normalizedRequest.limit(),
                        normalizedRequest.mode(),
                        normalizedRequest.responseMode()
                ))
                .toList();

        return new MigrationEvaluationResponse(
                LocalDateTime.now(),
                baselineClient.engineName(),
                candidateClients.stream().map(SearchEngineClient::engineName).toList(),
                normalizedRequest.queries(),
                normalizedRequest.limit(),
                normalizedRequest.mode(),
                normalizedRequest.responseMode(),
                candidateResults
        );
    }

    private Map<String, BaselineQueryResult> runBaselineQueries(
            SearchEngineClient baselineClient,
            List<String> queries,
            int limit,
            SearchMode mode,
            ResponseMode responseMode
    ) {
        Map<String, BaselineQueryResult> results = new LinkedHashMap<>();

        for (String query : queries) {
            try {
                SearchEngineResult result = baselineClient.search(query, limit, mode, responseMode);
                results.put(query, new BaselineQueryResult(result, null));
            } catch (Exception exception) {
                results.put(query, new BaselineQueryResult(null, extractErrorMessage(exception)));
            }
        }

        return results;
    }

    private CandidateMigrationResult evaluateCandidate(
            SearchEngineClient candidateClient,
            Map<String, BaselineQueryResult> baselineResults,
            List<String> queries,
            int limit,
            SearchMode mode,
            ResponseMode responseMode
    ) {
        List<QueryParityResult> queryResults = new ArrayList<>();

        for (String query : queries) {
            BaselineQueryResult baselineQueryResult = baselineResults.get(query);

            if (baselineQueryResult == null || baselineQueryResult.errorMessage() != null) {
                queryResults.add(createBaselineErrorResult(query, baselineQueryResult, limit));
                continue;
            }

            try {
                SearchEngineResult candidateResult = candidateClient.search(query, limit, mode, responseMode);
                queryResults.add(calculateQueryParity(query, baselineQueryResult.result(), candidateResult, limit, null));
            } catch (Exception exception) {
                queryResults.add(calculateQueryParity(
                        query,
                        baselineQueryResult.result(),
                        null,
                        limit,
                        extractErrorMessage(exception)
                ));
            }
        }

        return aggregateCandidateResult(candidateClient.engineName(), queryResults, limit);
    }

    private QueryParityResult createBaselineErrorResult(
            String query,
            BaselineQueryResult baselineQueryResult,
            int limit
    ) {
        String errorMessage = baselineQueryResult == null
                ? "Baseline result not found"
                : "Baseline engine failed: " + baselineQueryResult.errorMessage();

        return new QueryParityResult(
                query,
                List.of(),
                List.of(),
                0,
                false,
                limit,
                limit,
                1,
                0,
                0,
                errorMessage
        );
    }

    private QueryParityResult calculateQueryParity(
            String query,
            SearchEngineResult baselineResult,
            SearchEngineResult candidateResult,
            int limit,
            String errorMessage
    ) {
        List<String> baselineWorkflowCodes = extractWorkflowCodes(baselineResult);
        List<String> candidateWorkflowCodes = extractWorkflowCodes(candidateResult);

        Set<String> candidateSet = new LinkedHashSet<>(candidateWorkflowCodes);
        long overlapCount = baselineWorkflowCodes.stream()
                .filter(candidateSet::contains)
                .count();

        double topKOverlap = calculateTopKOverlap(baselineWorkflowCodes, candidateWorkflowCodes, overlapCount);
        boolean top1Match = calculateTop1Match(baselineWorkflowCodes, candidateWorkflowCodes);
        double averageRankShift = calculateAverageRankShift(baselineWorkflowCodes, candidateWorkflowCodes, limit);

        int missingBaselineCount = Math.max(0, baselineWorkflowCodes.size() - (int) overlapCount);
        double missingBaselineRate = baselineWorkflowCodes.isEmpty()
                ? 0
                : (double) missingBaselineCount / baselineWorkflowCodes.size();

        return new QueryParityResult(
                query,
                baselineWorkflowCodes,
                candidateWorkflowCodes,
                round(topKOverlap),
                top1Match,
                round(averageRankShift),
                missingBaselineCount,
                round(missingBaselineRate),
                baselineResult == null ? 0 : round(baselineResult.tookMs()),
                candidateResult == null ? 0 : round(candidateResult.tookMs()),
                errorMessage
        );
    }

    private CandidateMigrationResult aggregateCandidateResult(
            String engineName,
            List<QueryParityResult> queryResults,
            int limit
    ) {
        int queryCount = queryResults.size();
        int errorCount = (int) queryResults.stream()
                .filter(result -> result.errorMessage() != null)
                .count();

        double averageTopKOverlap = average(queryResults, QueryParityResult::topKOverlap);
        double top1MatchRate = queryCount == 0
                ? 0
                : (double) queryResults.stream().filter(QueryParityResult::top1Match).count() / queryCount;
        double averageRankShift = average(queryResults, QueryParityResult::averageRankShift);
        double averageMissingBaselineRate = average(queryResults, QueryParityResult::missingBaselineRate);
        double baselineAverageLatencyMs = averagePositive(queryResults, QueryParityResult::baselineTookMs);
        double candidateAverageLatencyMs = averagePositive(queryResults, QueryParityResult::candidateTookMs);

        double latencyRatioToBaseline = baselineAverageLatencyMs <= 0
                ? 0
                : candidateAverageLatencyMs / baselineAverageLatencyMs;

        double migrationCompatibilityScore = calculateMigrationCompatibilityScore(
                averageTopKOverlap,
                top1MatchRate,
                averageRankShift,
                averageMissingBaselineRate,
                latencyRatioToBaseline,
                errorCount,
                queryCount,
                limit
        );

        return new CandidateMigrationResult(
                engineName,
                round(averageTopKOverlap),
                round(top1MatchRate),
                round(averageRankShift),
                round(averageMissingBaselineRate),
                round(baselineAverageLatencyMs),
                round(candidateAverageLatencyMs),
                round(latencyRatioToBaseline),
                queryCount,
                errorCount,
                round(migrationCompatibilityScore),
                decide(migrationCompatibilityScore, averageTopKOverlap, latencyRatioToBaseline, errorCount, queryCount),
                queryResults
        );
    }

    private double calculateMigrationCompatibilityScore(
            double averageTopKOverlap,
            double top1MatchRate,
            double averageRankShift,
            double averageMissingBaselineRate,
            double latencyRatioToBaseline,
            int errorCount,
            int queryCount,
            int limit
    ) {
        double rankShiftScore = 1 - Math.min(averageRankShift / Math.max(limit - 1, 1), 1);
        double missingScore = 1 - averageMissingBaselineRate;
        double latencyScore = calculateLatencyScore(latencyRatioToBaseline);
        double errorRate = queryCount == 0 ? 1 : (double) errorCount / queryCount;

        double rawScore = 100 * (
                0.40 * averageTopKOverlap +
                0.20 * top1MatchRate +
                0.20 * rankShiftScore +
                0.10 * missingScore +
                0.10 * latencyScore
        );

        return rawScore * (1 - errorRate);
    }

    private double calculateLatencyScore(double latencyRatioToBaseline) {
        if (latencyRatioToBaseline <= 0) {
            return 0;
        }

        if (latencyRatioToBaseline <= 1) {
            return 1;
        }

        return Math.max(0, 1 - ((latencyRatioToBaseline - 1) / 2));
    }

    private String decide(
            double score,
            double averageTopKOverlap,
            double latencyRatioToBaseline,
            int errorCount,
            int queryCount
    ) {
        double errorRate = queryCount == 0 ? 1 : (double) errorCount / queryCount;

        if (errorRate > 0.20) {
            return "LOW_COMPATIBILITY_HIGH_ERROR_RATE";
        }

        if (score >= 85) {
            return "HIGH_COMPATIBILITY";
        }

        if (score >= 70) {
            return "COMPATIBLE_WITH_REVIEW";
        }

        if (latencyRatioToBaseline > 0 && latencyRatioToBaseline < 0.50 && averageTopKOverlap >= 0.50) {
            return "FAST_BUT_RELEVANCE_REVIEW";
        }

        if (score >= 55) {
            return "REVIEW_REQUIRED";
        }

        return "LOW_COMPATIBILITY";
    }

    private double calculateTopKOverlap(
            List<String> baselineWorkflowCodes,
            List<String> candidateWorkflowCodes,
            long overlapCount
    ) {
        if (baselineWorkflowCodes.isEmpty() && candidateWorkflowCodes.isEmpty()) {
            return 1;
        }

        if (baselineWorkflowCodes.isEmpty()) {
            return 0;
        }

        return (double) overlapCount / baselineWorkflowCodes.size();
    }

    private boolean calculateTop1Match(
            List<String> baselineWorkflowCodes,
            List<String> candidateWorkflowCodes
    ) {
        if (baselineWorkflowCodes.isEmpty() && candidateWorkflowCodes.isEmpty()) {
            return true;
        }

        if (baselineWorkflowCodes.isEmpty() || candidateWorkflowCodes.isEmpty()) {
            return false;
        }

        return Objects.equals(baselineWorkflowCodes.get(0), candidateWorkflowCodes.get(0));
    }

    private double calculateAverageRankShift(
            List<String> baselineWorkflowCodes,
            List<String> candidateWorkflowCodes,
            int limit
    ) {
        if (baselineWorkflowCodes.isEmpty()) {
            return 0;
        }

        Map<String, Integer> candidateRankMap = new HashMap<>();
        for (int i = 0; i < candidateWorkflowCodes.size(); i++) {
            candidateRankMap.put(candidateWorkflowCodes.get(i), i);
        }

        List<Integer> shifts = new ArrayList<>();
        for (int baselineRank = 0; baselineRank < baselineWorkflowCodes.size(); baselineRank++) {
            Integer candidateRank = candidateRankMap.get(baselineWorkflowCodes.get(baselineRank));
            if (candidateRank != null) {
                shifts.add(Math.abs(baselineRank - candidateRank));
            }
        }

        if (shifts.isEmpty()) {
            return limit;
        }

        return shifts.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(limit);
    }

    private List<String> extractWorkflowCodes(SearchEngineResult result) {
        if (result == null || result.hits() == null) {
            return List.of();
        }

        return result.hits().stream()
                .map(this::extractWorkflowIdentifier)
                .filter(identifier -> identifier != null && !identifier.isBlank())
                .toList();
    }

    private String extractWorkflowIdentifier(SearchHitDto hit) {
        if (hit == null) {
            return null;
        }

        if (hit.workflowCode() != null && !hit.workflowCode().isBlank()) {
            return hit.workflowCode();
        }

        return hit.id();
    }

    private NormalizedRequest normalizeRequest(MigrationEvaluationRequest request) {
        String baselineEngine = request == null || request.baselineEngine() == null || request.baselineEngine().isBlank()
                ? DEFAULT_BASELINE_ENGINE
                : request.baselineEngine();

        List<String> queries = request == null || request.queries() == null || request.queries().isEmpty()
                ? DEFAULT_QUERIES
                : request.queries();

        int limit = request == null || request.limit() == null || request.limit() <= 0
                ? DEFAULT_LIMIT
                : request.limit();

        SearchMode mode = request == null || request.mode() == null
                ? DEFAULT_SEARCH_MODE
                : request.mode();

        ResponseMode responseMode = request == null || request.responseMode() == null
                ? DEFAULT_RESPONSE_MODE
                : request.responseMode();

        if (responseMode != ResponseMode.METADATA_ONLY) {
            throw new IllegalArgumentException("Migration evaluation only supports METADATA_ONLY response mode.");
        }

        List<String> candidateEngines = normalizeCandidateEngines(request, baselineEngine);

        return new NormalizedRequest(
                normalizeEngineName(baselineEngine),
                candidateEngines,
                queries,
                limit,
                mode,
                responseMode
        );
    }

    private List<String> normalizeCandidateEngines(MigrationEvaluationRequest request, String baselineEngine) {
        String normalizedBaselineEngine = normalizeEngineName(baselineEngine);

        List<String> requestedCandidates = request == null || request.candidateEngines() == null || request.candidateEngines().isEmpty()
                ? searchEngineClients.stream()
                .map(SearchEngineClient::engineName)
                .toList()
                : request.candidateEngines();

        List<String> candidateEngines = requestedCandidates.stream()
                .map(this::normalizeEngineName)
                .filter(engine -> !engine.isBlank())
                .filter(engine -> !engine.equals(normalizedBaselineEngine))
                .distinct()
                .toList();

        if (candidateEngines.isEmpty()) {
            throw new IllegalArgumentException("At least one candidate engine must be provided.");
        }

        return candidateEngines;
    }

    private SearchEngineClient resolveClient(String requestedEngine) {
        Map<String, SearchEngineClient> clientMap = searchEngineClients.stream()
                .sorted(Comparator.comparing(SearchEngineClient::engineName))
                .collect(Collectors.toMap(
                        client -> normalizeEngineName(client.engineName()),
                        client -> client,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        SearchEngineClient client = clientMap.get(normalizeEngineName(requestedEngine));

        if (client == null) {
            throw new IllegalArgumentException(
                    "Unknown search engine: " + requestedEngine +
                            ". Available engines: " + clientMap.keySet()
            );
        }

        return client;
    }

    private String normalizeEngineName(String engineName) {
        return engineName == null
                ? ""
                : engineName.trim().toLowerCase(Locale.ROOT);
    }

    private double average(List<QueryParityResult> results, ToDoubleFunction<QueryParityResult> mapper) {
        return results.stream()
                .mapToDouble(mapper)
                .average()
                .orElse(0);
    }

    private double averagePositive(List<QueryParityResult> results, ToDoubleFunction<QueryParityResult> mapper) {
        return results.stream()
                .mapToDouble(mapper)
                .filter(value -> value > 0)
                .average()
                .orElse(0);
    }

    private String extractErrorMessage(Exception exception) {
        if (exception == null) {
            return null;
        }

        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        int maxLength = 1000;
        if (message.length() > maxLength) {
            return message.substring(0, maxLength) + "...";
        }

        return message;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record BaselineQueryResult(
            SearchEngineResult result,
            String errorMessage
    ) {
    }

    private record NormalizedRequest(
            String baselineEngine,
            List<String> candidateEngines,
            List<String> queries,
            int limit,
            SearchMode mode,
            ResponseMode responseMode
    ) {
    }
}
