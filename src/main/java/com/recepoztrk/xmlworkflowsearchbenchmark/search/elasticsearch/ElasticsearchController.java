package com.recepoztrk.xmlworkflowsearchbenchmark.search.elasticsearch;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Elasticsearch baseline işlemleri için REST endpointleri.
 *
 * Bu controller şimdilik sadece Elasticsearch tarafını test etmek içindir.
 * İleride ortak benchmark controller ayrıca yazılacaktır.
 */
@RestController
@RequestMapping("/api/elasticsearch")
@RequiredArgsConstructor
public class ElasticsearchController {

    private final ElasticsearchService elasticsearchService;

    /**
     * Elasticsearch bağlantı kontrolü.
     *
     * Örnek:
     * GET /api/elasticsearch/health
     */
    @GetMapping("/health")
    public String health() {
        return elasticsearchService.health();
    }

    /**
     * PostgreSQL'deki workflow kayıtlarını Elasticsearch'e yeniden indexler.
     *
     * Örnek:
     * POST /api/elasticsearch/reindex
     */
    @PostMapping("/reindex")
    public IndexOperationResult reindex() {
        return elasticsearchService.reindexAll();
    }

    /**
     * Elasticsearch üzerinde extracted SearchDocument alanlarıyla arama yapar.
     *
     * Örnek:
     * GET /api/elasticsearch/search?q=fatura itiraz&limit=10
     */
    @GetMapping("/search")
    public SearchEngineResult search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return elasticsearchService.search(q, limit);
    }
}