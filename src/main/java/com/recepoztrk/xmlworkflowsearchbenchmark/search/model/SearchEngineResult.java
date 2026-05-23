package com.recepoztrk.xmlworkflowsearchbenchmark.search.model;

import java.util.List;

/**
 * Bir search engine'in belirli bir query için döndürdüğü sonucu temsil eder.
 */
public record SearchEngineResult(
        String engine,
        String query,
        long tookMs,
        int hitCount,
        List<SearchHitDto> hits
) {
}