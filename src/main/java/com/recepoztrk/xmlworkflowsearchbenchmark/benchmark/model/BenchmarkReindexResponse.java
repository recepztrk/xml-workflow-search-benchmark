package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tüm search engine'lerde reindex işleminin sonucu.
 */
public record BenchmarkReindexResponse(
        LocalDateTime executedAt,
        List<IndexOperationResult> results
) {
}
