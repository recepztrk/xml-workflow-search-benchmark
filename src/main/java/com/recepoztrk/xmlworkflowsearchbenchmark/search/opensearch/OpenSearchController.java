package com.recepoztrk.xmlworkflowsearchbenchmark.search.opensearch;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OpenSearch işlemleri için REST endpointleri.
 *
 * Bu controller şimdilik OpenSearch alternatifini manuel test etmek için kullanılır.
 * Ortak benchmark controller daha sonra yazılacaktır.
 */
@RestController
@RequestMapping("/api/opensearch")
@RequiredArgsConstructor
public class OpenSearchController {

    private final OpenSearchService openSearchService;

    /**
     * OpenSearch bağlantı kontrolü.
     *
     * Örnek:
     * GET /api/opensearch/health
     */
    @GetMapping("/health")
    public String health() {
        return openSearchService.health();
    }

    /**
     * PostgreSQL'deki workflow kayıtlarını OpenSearch'e yeniden indexler.
     *
     * Örnek:
     * POST /api/opensearch/reindex
     */
    @PostMapping("/reindex")
    public IndexOperationResult reindex() {
        return openSearchService.reindexAll();
    }

    /**
     * OpenSearch üzerinde extracted SearchDocument alanlarıyla arama yapar.
     *
     * Örnek:
     * GET /api/opensearch/search?q=fatura itiraz&limit=10
     */
    @GetMapping("/search")
    public SearchEngineResult search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return openSearchService.search(q, limit);
    }
}
