package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.controller;

import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkExportResponse;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkReindexResponse;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkRunRequest;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model.BenchmarkRunResponse;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.service.BenchmarkExportService;
import com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.service.BenchmarkService;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Elasticsearch, OpenSearch ve Solr için ortak benchmark endpointleri.
 */
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final BenchmarkService benchmarkService;
    private final BenchmarkExportService benchmarkExportService;

    /**
     * Sisteme kayıtlı search engine'leri listeler.
     * <p>
     * GET /api/benchmark/engines
     */
    @GetMapping("/engines")
    public List<String> getAvailableEngines() {
        return benchmarkService.getAvailableEngines();
    }

    /**
     * Tüm search engine'lerde reindex işlemi çalıştırır.
     * <p>
     * POST /api/benchmark/reindex-all
     */
    @PostMapping("/reindex-all")
    public BenchmarkReindexResponse reindexAll(
            @RequestParam(defaultValue = "RAW_XML") SearchMode mode
    ) {
        return benchmarkService.reindexAll(mode);
    }

    /**
     * Seçilen engine'ler ve query seti için benchmark çalıştırır.
     * <p>
     * POST /api/benchmark/run
     */
    @PostMapping("/run")
    public BenchmarkRunResponse runBenchmark(
            @RequestBody(required = false) BenchmarkRunRequest request
    ) {
        return benchmarkService.runBenchmark(request);
    }

    /**
     * Benchmark çalıştırır ve sonucu JSON + CSV olarak dışa aktarır.
     * <p>
     * POST /api/benchmark/run-and-export
     */
    @PostMapping("/run-and-export")
    public BenchmarkExportResponse runBenchmarkAndExport(
            @RequestBody(required = false) BenchmarkRunRequest request
    ) {
        BenchmarkRunResponse benchmarkResult = benchmarkService.runBenchmark(request);
        return benchmarkExportService.export(benchmarkResult);
    }
}