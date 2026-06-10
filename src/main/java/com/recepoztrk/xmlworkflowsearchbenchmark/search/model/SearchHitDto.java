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
        Integer xmlSizeKb,

        /**
         * Sadece FULL_XML_RESPONSE modunda dolar.
         * METADATA_ONLY modunda null döner.
         */
        String xmlContent,

        /**
         * Bu hit'in yaklaşık response payload boyutu.
         * METADATA_ONLY için küçük, FULL_XML_RESPONSE için xmlContent'e bağlı büyük olur.
         */
        Integer responseSizeKb
) {
}
