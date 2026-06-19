package com.recepoztrk.xmlworkflowsearchbenchmark.migration.model;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.ResponseMode;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;

import java.util.List;

/**
 * Elasticsearch baseline'ına göre aday search engine'lerin sonuç uyumluluğunu
 * ölçmek için kullanılan istek modeli.
 */
public record MigrationEvaluationRequest(
        String baselineEngine,
        List<String> candidateEngines,
        List<String> queries,
        Integer limit,
        SearchMode mode,
        ResponseMode responseMode
) {
}
