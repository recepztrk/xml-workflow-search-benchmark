package com.recepoztrk.xmlworkflowsearchbenchmark.search.opensearch;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * OpenSearch işlemleri için REST endpointleri.
 *
 * Bu controller OpenSearch tarafını manuel test etmek için kullanılır.
 * Ortak benchmark testleri için /api/benchmark endpointleri kullanılmalıdır.
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
     * PostgreSQL'deki workflow kayıtlarını seçilen moda göre OpenSearch'e yeniden indexler.
     *
     * Mode seçenekleri:
     * - RAW_XML: XML parse edilmeden xmlContent alanı üzerinden indexlenir.
     * - EXTRACTED_DOCUMENT: XML parse edilerek SearchDocument alanları üzerinden indexlenir.
     *
     * Örnek:
     * POST /api/opensearch/reindex?mode=RAW_XML
     * POST /api/opensearch/reindex?mode=EXTRACTED_DOCUMENT
     */
    @PostMapping("/reindex")
    public IndexOperationResult reindex(
            @RequestParam(defaultValue = "EXTRACTED_DOCUMENT") SearchMode mode
    ) {
        return openSearchService.reindexAll(mode);
    }

    /**
     * OpenSearch üzerinde seçilen moda göre arama yapar.
     *
     * Örnek:
     * GET /api/opensearch/search?q=fatura itiraz&limit=10&mode=RAW_XML
     * GET /api/opensearch/search?q=fatura itiraz&limit=10&mode=EXTRACTED_DOCUMENT
     */
    @GetMapping("/search")
    public SearchEngineResult search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "EXTRACTED_DOCUMENT") SearchMode mode
    ) {
        return openSearchService.search(q, limit, mode);
    }
}