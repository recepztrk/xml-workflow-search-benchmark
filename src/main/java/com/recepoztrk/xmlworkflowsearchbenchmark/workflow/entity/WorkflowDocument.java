package com.recepoztrk.xmlworkflowsearchbenchmark.workflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * PostgreSQL üzerinde tutulan XML tabanlı workflow dokümanını temsil eder.
 *
 * Bu entity, gerçek sistemdeki "XML hem veritabanında hem search engine'de var"
 * yapısının PostgreSQL tarafını modellemek için oluşturulmuştur.
 */
@Entity
@Table(name = "workflow_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Workflow'un teknik kodu.
     * Örnek: WF_BILLING_DISPUTE
     */
    @Column(name = "workflow_code", nullable = false, length = 100)
    private String workflowCode;

    /**
     * Workflow'un insan tarafından okunabilir adı.
     * Örnek: Fatura İtiraz Süreci
     */
    @Column(name = "workflow_name", nullable = false)
    private String workflowName;

    /**
     * Workflow versiyonu.
     */
    @Column(name = "version")
    private Integer version;

    /**
     * ACTIVE, PASSIVE, DRAFT gibi durum bilgisi.
     */
    @Column(name = "status", length = 30)
    private String status;

    /**
     * İş alanı.
     * Örnek: Billing, Customer, TechnicalSupport
     */
    @Column(name = "domain", length = 100)
    private String domain;

    /**
     * XML içeriği.
     *
     * Şimdilik TEXT olarak tutuyoruz.
     * Sebep:
     * - Büyük XML stringleriyle çalışmak kolay.
     * - Search engine'lere aktarımı pratik.
     * - Benchmark PoC için yeterli.
     */
    @Column(name = "xml_content", nullable = false, columnDefinition = "TEXT")
    private String xmlContent;

    /**
     * XML boyutunu KB cinsinden tutuyoruz.
     * Benchmarkta veri boyutu ile performans ilişkisini görmek için faydalı olacak.
     */
    @Column(name = "xml_size_kb")
    private Integer xmlSizeKb;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
