package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.service;

import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkExportResponse;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkRunResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Benchmark sonuçlarını JSON dosyası olarak dışa aktarır.
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

            writeJson(jsonFilePath, benchmarkResult);

            return new BenchmarkExportResponse(
                    exportedAt,
                    jsonFilePath.toString(),
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
}