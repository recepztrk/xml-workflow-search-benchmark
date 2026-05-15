package com.recepoztrk.xmlworkflowsearchbenchmark.workflow.controller;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.entity.WorkflowDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.repository.WorkflowDocumentRepository;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.service.WorkflowSeedService;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.service.WorkflowXmlParser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Workflow XML verilerini üretmek, listelemek ve SearchDocument dönüşümünü kontrol etmek
 * için kullanılan REST endpointlerini içerir.
 *
 * Bu controller geliştirme ve benchmark hazırlık aşaması için kullanılır.
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowSeedService seedService;
    private final WorkflowDocumentRepository repository;
    private final WorkflowXmlParser xmlParser;

    /**
     * Sentetik XML workflow dataset üretir.
     *
     * Örnek:
     * POST /api/workflows/generate?count=10&screenCount=20
     */
    @PostMapping("/generate")
    public GenerateWorkflowResponse generate(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "20") int screenCount
    ) {
        long total = seedService.generateSampleWorkflows(count, screenCount);

        return new GenerateWorkflowResponse(
                "Workflow XML dataset generated successfully.",
                total
        );
    }

    /**
     * PostgreSQL'deki workflow kayıt sayısını döndürür.
     *
     * Örnek:
     * GET /api/workflows/count
     */
    @GetMapping("/count")
    public CountResponse count() {
        return new CountResponse(repository.count());
    }

    /**
     * PostgreSQL'deki workflow kayıtlarını özet olarak listeler.
     *
     * XML'in tamamı burada döndürülmez.
     * Çünkü XML büyüdükçe response gereksiz şekilde şişer.
     *
     * Örnek:
     * GET /api/workflows
     */
    @GetMapping
    public List<WorkflowSummaryResponse> findAll() {
        return repository.findAll()
                .stream()
                .map(WorkflowSummaryResponse::fromEntity)
                .toList();
    }

    /**
     * Belirli bir workflow kaydını SearchDocument modeline dönüştürür
     * ve özet/preview olarak döndürür.
     *
     * Bu endpoint, XML parser'ın doğru çalışıp çalışmadığını görmek için kullanılır.
     *
     * Örnek:
     * GET /api/workflows/1/search-document
     */
    @GetMapping("/{id}/search-document")
    public SearchDocumentPreviewResponse getSearchDocument(@PathVariable Long id) {
        WorkflowDocument document = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Workflow document bulunamadı. id=" + id
                ));
        
        SearchDocument searchDocument = xmlParser.parse(document);

        return SearchDocumentPreviewResponse.fromSearchDocument(searchDocument);
    }

    /**
     * İlk N workflow kaydı için SearchDocument preview döndürür.
     *
     * Örnek:
     * GET /api/workflows/search-documents?limit=5
     */
    @GetMapping("/search-documents")
    public List<SearchDocumentPreviewResponse> getSearchDocuments(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return repository.findAll(PageRequest.of(0, limit))
                .stream()
                .map(xmlParser::parse)
                .map(SearchDocumentPreviewResponse::fromSearchDocument)
                .toList();
    }

    public record GenerateWorkflowResponse(
            String message,
            long totalCount
    ) {
    }

    public record CountResponse(
            long totalCount
    ) {
    }

    public record WorkflowSummaryResponse(
            Long id,
            String workflowCode,
            String workflowName,
            String status,
            String domain,
            Integer xmlSizeKb
    ) {
        public static WorkflowSummaryResponse fromEntity(WorkflowDocument document) {
            return new WorkflowSummaryResponse(
                    document.getId(),
                    document.getWorkflowCode(),
                    document.getWorkflowName(),
                    document.getStatus(),
                    document.getDomain(),
                    document.getXmlSizeKb()
            );
        }
    }

    public record SearchDocumentPreviewResponse(
            String id,
            Long databaseId,
            String workflowCode,
            String workflowName,
            String status,
            String domain,
            int screenTitleCount,
            int screenDescriptionCount,
            int actionTextCount,
            int technicalTokenCount,
            int searchTextLength,
            String searchTextPreview,
            Integer xmlSizeKb
    ) {
        public static SearchDocumentPreviewResponse fromSearchDocument(SearchDocument document) {
            return new SearchDocumentPreviewResponse(
                    document.id(),
                    document.databaseId(),
                    document.workflowCode(),
                    document.workflowName(),
                    document.status(),
                    document.domain(),
                    document.screenTitles().size(),
                    document.screenDescriptions().size(),
                    document.actionTexts().size(),
                    document.technicalTokens().size(),
                    document.searchText().length(),
                    abbreviate(document.searchText(), 500),
                    document.xmlSizeKb()
            );
        }

        private static String abbreviate(String value, int maxLength) {
            if (value == null || value.isBlank()) {
                return "";
            }

            if (value.length() <= maxLength) {
                return value;
            }

            return value.substring(0, maxLength) + "...";
        }
    }
}