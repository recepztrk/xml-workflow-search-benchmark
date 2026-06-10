package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.ResponseMode;

import java.util.List;

/**
 * Benchmark çalıştırma isteği.
 *
 * mode:
 * - RAW_XML: Mevcut sistem yaklaşımı. XML parse edilmeden xmlContent üzerinde arama yapılır.
 * - EXTRACTED_DOCUMENT: XML parse edilerek oluşturulan SearchDocument alanlarında arama yapılır.
 *
 * responseMode:
 * - METADATA_ONLY: Search sonucunda sadece metadata döner.
 * - FULL_XML_RESPONSE: Search sonucunda metadata + xmlContent döner.
 */
public record BenchmarkRunRequest(
        List<String> engines,
        List<String> queries,
        Integer limit,
        Integer warmupIterations,
        Integer measurementIterations,
        SearchMode mode,
        ResponseMode responseMode
) {
}