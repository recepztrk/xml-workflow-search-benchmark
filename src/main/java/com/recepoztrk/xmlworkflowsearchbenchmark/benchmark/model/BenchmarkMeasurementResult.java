package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model;

import java.util.List;

/**
 * Tek bir engine + tek bir query için benchmark ölçüm sonucu.
 */
public record BenchmarkMeasurementResult(
        String engine,
        String query,
        int limit,
        int warmupIterations,
        int measurementIterations,
        int successCount,
        int errorCount,
        int lastHitCount,
        double avgMs,
        long minMs,
        long maxMs,
        double p50Ms,
        double p95Ms,
        double p99Ms,
        List<Long> samplesMs
) {
}