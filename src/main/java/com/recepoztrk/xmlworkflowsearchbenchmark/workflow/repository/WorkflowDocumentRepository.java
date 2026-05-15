package com.recepoztrk.xmlworkflowsearchbenchmark.workflow.repository;

import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.entity.WorkflowDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowDocumentRepository extends JpaRepository<WorkflowDocument, Long> {

    Optional<WorkflowDocument> findByWorkflowCode(String workflowCode);
}
