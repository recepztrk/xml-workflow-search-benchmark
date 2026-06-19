package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model;

import java.time.LocalDateTime;

/**
 * Benchmark sonucunun JSON dosyasına export edilmesi sonrası dönen cevap modeli.
 */
public record BenchmarkExportResponse(
        LocalDateTime exportedAt,
        String jsonFilePath,
        BenchmarkRunResponse benchmarkResult
) {
}