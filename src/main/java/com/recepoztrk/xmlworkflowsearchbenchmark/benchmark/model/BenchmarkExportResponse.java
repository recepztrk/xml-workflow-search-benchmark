package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model;

import java.time.LocalDateTime;

/**
 * Benchmark sonucunun dosyaya export edilmesi sonrası dönen cevap modeli.
 */
public record BenchmarkExportResponse(
        LocalDateTime exportedAt,
        String jsonFilePath,
        String csvFilePath,
        BenchmarkRunResponse benchmarkResult
) {
}
