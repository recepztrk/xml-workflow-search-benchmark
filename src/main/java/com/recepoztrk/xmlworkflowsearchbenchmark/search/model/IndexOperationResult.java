package com.recepoztrk.xmlworkflowsearchbenchmark.search.model;

/**
 * Indexleme operasyonunun özet sonucunu temsil eder.
 */
public record IndexOperationResult(
        String engine,
        String indexName,
        long indexedDocumentCount,
        String message
) {
}
