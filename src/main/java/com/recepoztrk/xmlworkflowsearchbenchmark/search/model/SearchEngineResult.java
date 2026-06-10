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

        /**
         * Search response içinde dönen toplam yaklaşık payload boyutu.
         * METADATA_ONLY modunda düşük, FULL_XML_RESPONSE modunda XML boyutuna bağlı yüksek olur.
         */
        Integer responseSizeKb,
        List<SearchHitDto> hits
) {
}