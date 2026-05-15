package com.recepoztrk.xmlworkflowsearchbenchmark.search.model;

import java.util.List;

/**
 * Search engine'lere gönderilecek normalize edilmiş arama dokümanı.
 *
 * Bu model PostgreSQL entity'si değildir.
 * Elasticsearch, OpenSearch ve Solr'a gönderilecek ortak veri formatını temsil eder.
 */
public record SearchDocument(
        String id,
        Long databaseId,
        String workflowCode,
        String workflowName,
        String status,
        String domain,
        List<String> screenTitles,
        List<String> screenDescriptions,
        List<String> actionTexts,
        List<String> technicalTokens,
        String searchText,
        String xmlContent,
        Integer xmlSizeKb
) {
}