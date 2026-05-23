package com.recepoztrk.xmlworkflowsearchbenchmark.search.model;

/**
 * Search engine'den dönen tek bir arama sonucunu temsil eder.
 */
public record SearchHitDto(
        String id,
        Double score,
        String workflowCode,
        String workflowName,
        String status,
        String domain,
        Integer xmlSizeKb
) {
}
