package com.recepoztrk.xmlworkflowsearchbenchmark.search.solr;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Apache Solr işlemleri için REST endpointleri.
 *
 * Bu controller şimdilik Solr entegrasyonunu manuel test etmek için kullanılır.
 * Ortak benchmark controller daha sonra yazılacaktır.
 */
@RestController
@RequestMapping("/api/solr")
@RequiredArgsConstructor
public class SolrController {

    private final SolrService solrService;

    /**
     * Solr bağlantı/core kontrolü.
     *
     * Örnek:
     * GET /api/solr/health
     */
    @GetMapping("/health")
    public String health() {
        return solrService.health();
    }

    /**
     * PostgreSQL'deki workflow kayıtlarını Solr'a yeniden indexler.
     *
     * Örnek:
     * POST /api/solr/reindex
     */
    @PostMapping("/reindex")
    public IndexOperationResult reindex() {
        return solrService.reindexAll();
    }

    /**
     * Solr üzerinde extracted SearchDocument alanlarıyla arama yapar.
     *
     * Örnek:
     * GET /api/solr/search?q=fatura itiraz&limit=10
     */
    @GetMapping("/search")
    public SearchEngineResult search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return solrService.search(q, limit);
    }
}
