package com.recepoztrk.xmlworkflowsearchbenchmark.search.solr;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Apache Solr işlemleri için REST endpointleri.
 *
 * Bu controller Solr tarafını manuel test etmek için kullanılır.
 * Ortak benchmark testleri için /api/benchmark endpointleri kullanılmalıdır.
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
     * PostgreSQL'deki workflow kayıtlarını seçilen moda göre Solr'a yeniden indexler.
     *
     * Mode seçenekleri:
     * - RAW_XML: XML parse edilmeden xmlContent_txt alanı üzerinden indexlenir.
     * - EXTRACTED_DOCUMENT: XML parse edilerek SearchDocument alanları üzerinden indexlenir.
     *
     * Örnek:
     * POST /api/solr/reindex?mode=RAW_XML
     * POST /api/solr/reindex?mode=EXTRACTED_DOCUMENT
     */
    @PostMapping("/reindex")
    public IndexOperationResult reindex(
            @RequestParam(defaultValue = "EXTRACTED_DOCUMENT") SearchMode mode
    ) {
        return solrService.reindexAll(mode);
    }

    /**
     * Solr üzerinde seçilen moda göre arama yapar.
     *
     * Örnek:
     * GET /api/solr/search?q=fatura itiraz&limit=10&mode=RAW_XML
     * GET /api/solr/search?q=fatura itiraz&limit=10&mode=EXTRACTED_DOCUMENT
     */
    @GetMapping("/search")
    public SearchEngineResult search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "EXTRACTED_DOCUMENT") SearchMode mode
    ) {
        return solrService.search(q, limit, mode);
    }
}