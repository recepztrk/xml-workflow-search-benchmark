package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Benchmark çalıştırmasının genel cevabı.
 */
public record BenchmarkRunResponse(
        LocalDateTime executedAt,
        List<String> engines,
        List<String> queries,
        int limit,
        int warmupIterations,
        int measurementIterations,
        List<BenchmarkMeasurementResult> results
) {
}
