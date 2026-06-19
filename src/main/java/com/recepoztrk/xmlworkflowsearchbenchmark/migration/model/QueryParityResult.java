package com.recepoztrk.xmlworkflowsearchbenchmark.migration.model;

import java.util.List;

/**
 * Tek bir query için baseline engine ile aday engine arasındaki sonuç benzerliğini temsil eder.
 */
public record QueryParityResult(
        String query,
        List<String> baselineWorkflowCodes,
        List<String> candidateWorkflowCodes,
        double topKOverlap,
        boolean top1Match,
        double averageRankShift,
        int missingBaselineCount,
        double missingBaselineRate,
        double baselineTookMs,
        double candidateTookMs,
        String errorMessage
) {
}
