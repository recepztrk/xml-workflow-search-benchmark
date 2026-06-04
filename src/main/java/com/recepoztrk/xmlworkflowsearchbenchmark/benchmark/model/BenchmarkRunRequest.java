package com.recepoztrk.xmlworkflowsearchbenchmark.benchmark.model;

import java.util.List;

/**
 * Benchmark çalıştırma isteği.
 *
 * engines boş verilirse tüm engine'ler çalıştırılır.
 * queries boş verilirse default query seti kullanılır.
 */
public record BenchmarkRunRequest(
        List<String> engines,
        List<String> queries,
        Integer limit,
        Integer warmupIterations,
        Integer measurementIterations
) {
}
