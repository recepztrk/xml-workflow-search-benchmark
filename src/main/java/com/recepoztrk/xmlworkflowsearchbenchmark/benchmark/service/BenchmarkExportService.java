package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.service;

import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkExportResponse;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkMeasurementResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkRunResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Benchmark sonuçlarını JSON ve CSV dosyası olarak dışa aktarır.
 */
@Service
public class BenchmarkExportService {

    private static final Path EXPORT_DIRECTORY = Path.of("benchmark-results");

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final JsonMapper jsonMapper;

    public BenchmarkExportService() {
        this.jsonMapper = JsonMapper.builder().build();
    }

    public BenchmarkExportResponse export(BenchmarkRunResponse benchmarkResult) {
        try {
            Files.createDirectories(EXPORT_DIRECTORY);

            LocalDateTime exportedAt = LocalDateTime.now();
            String baseFileName = createBaseFileName(benchmarkResult, exportedAt);

            Path jsonFilePath = EXPORT_DIRECTORY.resolve(baseFileName + ".json");
            Path csvFilePath = EXPORT_DIRECTORY.resolve(baseFileName + ".csv");

            writeJson(jsonFilePath, benchmarkResult);
            writeCsv(csvFilePath, benchmarkResult);

            return new BenchmarkExportResponse(
                    exportedAt,
                    jsonFilePath.toString(),
                    csvFilePath.toString(),
                    benchmarkResult
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Benchmark sonucu export edilemedi.", exception);
        }
    }

    private String createBaseFileName(BenchmarkRunResponse benchmarkResult, LocalDateTime exportedAt) {
        String mode = benchmarkResult.mode() == null
                ? "unknown_mode"
                : benchmarkResult.mode().name().toLowerCase(Locale.ROOT);

        String responseMode = benchmarkResult.responseMode() == null
                ? "unknown_response"
                : benchmarkResult.responseMode().name().toLowerCase(Locale.ROOT);

        String timestamp = exportedAt.format(FILE_TIMESTAMP_FORMATTER);

        return mode + "_" + responseMode + "_" + timestamp;
    }

    private void writeJson(Path jsonFilePath, BenchmarkRunResponse benchmarkResult) throws IOException {
        String prettyJson = jsonMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(benchmarkResult);

        Files.writeString(jsonFilePath, prettyJson, StandardCharsets.UTF_8);
    }

    private void writeCsv(Path csvFilePath, BenchmarkRunResponse benchmarkResult) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(csvFilePath, StandardCharsets.UTF_8)) {
            writer.write(csvHeader());
            writer.newLine();

            for (BenchmarkMeasurementResult result : benchmarkResult.results()) {
                writer.write(csvRow(result));
                writer.newLine();
            }
        }
    }

    private String csvHeader() {
        return String.join(",",
                "engine",
                "query",
                "searchMode",
                "responseMode",
                "limit",
                "warmupIterations",
                "measurementIterations",
                "successCount",
                "errorCount",
                "lastHitCount",
                "lastResponseSizeKb",
                "avgMs",
                "minMs",
                "maxMs",
                "p50Ms",
                "p95Ms",
                "p99Ms",
                "lastErrorMessage"
        );
    }

    private String csvRow(BenchmarkMeasurementResult result) {
        return String.join(",",
                escapeCsv(result.engine()),
                escapeCsv(result.query()),
                valueOf(result.searchMode()),
                valueOf(result.responseMode()),
                String.valueOf(result.limit()),
                String.valueOf(result.warmupIterations()),
                String.valueOf(result.measurementIterations()),
                String.valueOf(result.successCount()),
                String.valueOf(result.errorCount()),
                String.valueOf(result.lastHitCount()),
                valueOf(result.lastResponseSizeKb()),
                String.valueOf(result.avgMs()),
                String.valueOf(result.minMs()),
                String.valueOf(result.maxMs()),
                String.valueOf(result.p50Ms()),
                String.valueOf(result.p95Ms()),
                String.valueOf(result.p99Ms()),
                escapeCsv(result.lastErrorMessage())
        );
    }

    private String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        boolean mustQuote = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");

        String escaped = value.replace("\"", "\"\"");

        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }
}
