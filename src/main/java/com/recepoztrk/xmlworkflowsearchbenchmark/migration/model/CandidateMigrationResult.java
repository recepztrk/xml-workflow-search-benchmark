package com.recepoztrk.xmlworkflowsearchbenchmark.migration.model;

import java.util.List;

/**
 * Bir aday search engine'in Elasticsearch baseline'ına göre migration uyumluluk sonucunu temsil eder.
 */
public record CandidateMigrationResult(
        String engine,
        double averageTopKOverlap,
        double top1MatchRate,
        double averageRankShift,
        double averageMissingBaselineRate,
        double baselineAverageLatencyMs,
        double candidateAverageLatencyMs,
        double latencyRatioToBaseline,
        int queryCount,
        int errorCount,
        double migrationCompatibilityScore,
        String decision,
        List<QueryParityResult> queryResults
) {
}
