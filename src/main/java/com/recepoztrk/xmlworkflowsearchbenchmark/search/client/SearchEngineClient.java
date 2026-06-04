package com.recepoztrk.xmlworkflowsearchbenchmark.search.client;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;

/**
 * Elasticsearch, OpenSearch ve Solr gibi search engine servisleri için ortak contract.
 *
 * Benchmark runner bu interface üzerinden çalışır.
 * Böylece hangi search engine çağrılırsa çağrılsın aynı operasyonlar kullanılabilir:
 *
 * - engineName()
 * - reindexAll()
 * - search(query, limit)
 */
public interface SearchEngineClient {

    String engineName();

    IndexOperationResult reindexAll();

    SearchEngineResult search(String query, int limit);
}