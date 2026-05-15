package com.recepoztrk.xmlworkflowsearchbenchmark.workflow.service;

import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.entity.WorkflowDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.repository.WorkflowDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * PostgreSQL'e sentetik workflow XML verisi basar.
 *
 * Bu servis ileride benchmark dataset hazırlama sürecinin ilk adımı olacak.
 */
@Service
@RequiredArgsConstructor
public class WorkflowSeedService {

    private final WorkflowDocumentRepository repository;
    private final WorkflowXmlGenerator xmlGenerator;

    @Transactional
    public long generateSampleWorkflows(int count, int screenCountPerWorkflow) {
        repository.deleteAll();

        List<String> domains = List.of(
                "Billing",
                "Customer",
                "TechnicalSupport",
                "Subscription",
                "Payment"
        );

        for (int i = 1; i <= count; i++) {
            String domain = domains.get((i - 1) % domains.size());
            String workflowCode = "WF_" + domain.toUpperCase(Locale.ROOT) + "_" + i;
            String workflowName = generateWorkflowName(domain, i);

            String xml = xmlGenerator.generateWorkflowXml(
                    workflowCode,
                    workflowName,
                    domain,
                    1,
                    screenCountPerWorkflow
            );

            WorkflowDocument document = WorkflowDocument.builder()
                    .workflowCode(workflowCode)
                    .workflowName(workflowName)
                    .version(1)
                    .status("ACTIVE")
                    .domain(domain)
                    .xmlContent(xml)
                    .xmlSizeKb(calculateSizeKb(xml))
                    .build();

            repository.save(document);
        }

        return repository.count();
    }

    public long count() {
        return repository.count();
    }

    private String generateWorkflowName(String domain, int index) {
        return switch (domain) {
            case "Billing" -> "Fatura İtiraz Süreci " + index;
            case "Customer" -> "Müşteri Bilgileri Güncelleme Süreci " + index;
            case "TechnicalSupport" -> "Teknik Arıza Kaydı Süreci " + index;
            case "Subscription" -> "Abonelik İptal ve Değişiklik Süreci " + index;
            case "Payment" -> "Ödeme Durumu Kontrol Süreci " + index;
            default -> "Genel Müşteri İşlem Süreci " + index;
        };
    }

    private int calculateSizeKb(String content) {
        return content.getBytes(StandardCharsets.UTF_8).length / 1024;
    }
}