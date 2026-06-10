package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.ResponseMode;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;

import java.util.List;

/**
 * Tek bir engine + tek bir query için benchmark ölçüm sonucu.
 */
public record BenchmarkMeasurementResult(
        String engine,
        String query,

        /**
         * Aramanın hangi doküman stratejisiyle yapıldığını gösterir.
         * RAW_XML veya EXTRACTED_DOCUMENT.
         */
        SearchMode searchMode,

        /**
         * Search sonucunda sadece metadata mı yoksa full XML de mi döndürüldüğünü gösterir.
         */
        ResponseMode responseMode,

        int limit,
        int warmupIterations,
        int measurementIterations,
        int successCount,
        int errorCount,
        int lastHitCount,

        /**
         * Son başarılı search response'unun yaklaşık toplam payload boyutu.
         * METADATA_ONLY modunda düşük, FULL_XML_RESPONSE modunda XML boyutuna bağlı yüksek olur.
         */
        Integer lastResponseSizeKb,

        double avgMs,
        long minMs,
        long maxMs,
        double p50Ms,
        double p95Ms,
        double p99Ms,
        String lastErrorMessage,
        List<Long> samplesMs
) {
}