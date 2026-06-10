package com.recepoztrk.xmlworkflowsearchbenchmark.search.model;

/**
 * Search sonucunda döndürülecek response içeriğini belirler.
 */
public enum ResponseMode {

    /**
     * Sadece küçük metadata alanları döndürülür.
     * Amaç search engine arama latency'sini payload maliyetinden ayırmaktır.
     */
    METADATA_ONLY,

    /**
     * Metadata alanlarına ek olarak xmlContent de response'a dahil edilir.
     * Amaç büyük XML payload taşıma maliyetini ölçmektir.
     */
    FULL_XML_RESPONSE
}