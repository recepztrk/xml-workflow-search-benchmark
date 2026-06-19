package com.recepoztrk.xmlworkflowsearchbenchmark.migration.model;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.ResponseMode;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Elasticsearch baseline'ı ile aday search engine sonuçlarını karşılaştıran migration değerlendirme çıktısı.
 */
public record MigrationEvaluationResponse(
        LocalDateTime executedAt,
        String baselineEngine,
        List<String> candidateEngines,
        List<String> queries,
        int limit,
        SearchMode mode,
        ResponseMode responseMode,
        List<CandidateMigrationResult> candidateResults
) {
}
