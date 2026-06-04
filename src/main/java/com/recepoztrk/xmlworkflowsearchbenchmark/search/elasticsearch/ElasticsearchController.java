package com.recepoztrk.xmlworkflowsearchbenchmark.search.elasticsearch;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Elasticsearch baseline işlemleri için REST endpointleri.
 *
 * Bu controller Elasticsearch tarafını manuel test etmek için kullanılır.
 * Ortak benchmark testleri için /api/benchmark endpointleri kullanılmalıdır.
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
     * PostgreSQL'deki workflow kayıtlarını seçilen moda göre Elasticsearch'e yeniden indexler.
     *
     * Mode seçenekleri:
     * - RAW_XML: XML parse edilmeden xmlContent alanı üzerinden indexlenir.
     * - EXTRACTED_DOCUMENT: XML parse edilerek SearchDocument alanları üzerinden indexlenir.
     *
     * Örnek:
     * POST /api/elasticsearch/reindex?mode=RAW_XML
     * POST /api/elasticsearch/reindex?mode=EXTRACTED_DOCUMENT
     */
    @PostMapping("/reindex")
    public IndexOperationResult reindex(
            @RequestParam(defaultValue = "EXTRACTED_DOCUMENT") SearchMode mode
    ) {
        return elasticsearchService.reindexAll(mode);
    }

    /**
     * Elasticsearch üzerinde seçilen moda göre arama yapar.
     *
     * Örnek:
     * GET /api/elasticsearch/search?q=fatura itiraz&limit=10&mode=RAW_XML
     * GET /api/elasticsearch/search?q=fatura itiraz&limit=10&mode=EXTRACTED_DOCUMENT
     */
    @GetMapping("/search")
    public SearchEngineResult search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "EXTRACTED_DOCUMENT") SearchMode mode
    ) {
        return elasticsearchService.search(q, limit, mode);
    }
}