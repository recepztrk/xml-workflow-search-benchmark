package com.recepoztrk.xmlworkflowsearchbenchmark.search.client;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.ResponseMode;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;

/**
 * Elasticsearch, OpenSearch ve Solr gibi search engine servisleri için ortak contract.
 */
public interface SearchEngineClient {

    String engineName();

    /**
     * Eski controller davranışını korur.
     * Direkt /api/elasticsearch/reindex gibi endpointler varsayılan olarak
     * parse edilmiş SearchDocument yaklaşımıyla çalışır.
     */
    default IndexOperationResult reindexAll() {
        return reindexAll(SearchMode.EXTRACTED_DOCUMENT);
    }

    IndexOperationResult reindexAll(SearchMode mode);

    /**
     * Eski controller davranışını korur.
     * Direkt /api/elasticsearch/search gibi endpointler varsayılan olarak
     * parse edilmiş SearchDocument yaklaşımıyla çalışır.
     */
    default SearchEngineResult search(String query, int limit) {
        return search(query, limit, SearchMode.EXTRACTED_DOCUMENT, ResponseMode.METADATA_ONLY);
    }

    /**
     * Eski üç parametreli çağrıları bozmamak için responseMode varsayılan olarak METADATA_ONLY alınır.
     */
    default SearchEngineResult search(String query, int limit, SearchMode mode) {
        return search(query, limit, mode, ResponseMode.METADATA_ONLY);
    }

    /**
     * mode:
     * - RAW_XML
     * - EXTRACTED_DOCUMENT
     *
     * responseMode:
     * - METADATA_ONLY
     * - FULL_XML_RESPONSE
     */
    SearchEngineResult search(String query, int limit, SearchMode mode, ResponseMode responseMode);
}