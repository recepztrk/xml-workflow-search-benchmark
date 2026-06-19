package com.recepoztrk.xmlworkflowsearchbenchmark.migration.controller;

import com.recepoztrk.xmlworkflowsearchbenchmark.migration.model.MigrationEvaluationRequest;
import com.recepoztrk.xmlworkflowsearchbenchmark.migration.model.MigrationEvaluationResponse;
import com.recepoztrk.xmlworkflowsearchbenchmark.migration.service.MigrationEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Elasticsearch baseline'ına göre OpenSearch/Solr gibi aday engine'lerin migration uyumluluğunu ölçer.
 */
@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class MigrationEvaluationController {

    private final MigrationEvaluationService migrationEvaluationService;

    /**
     * Aday search engine'lerin Elasticsearch baseline sonuçlarına ne kadar yakın olduğunu ölçer.
     * <p>
     * POST /api/migration/evaluate
     */
    @PostMapping("/evaluate")
    public MigrationEvaluationResponse evaluate(
            @RequestBody(required = false) MigrationEvaluationRequest request
    ) {
        return migrationEvaluationService.evaluate(request);
    }
}
