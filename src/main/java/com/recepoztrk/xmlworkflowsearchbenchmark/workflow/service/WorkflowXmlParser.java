package com.recepoztrk.xmlworkflowsearchbenchmark.workflow.service;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.entity.WorkflowDocument;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * PostgreSQL'de tutulan ham XML workflow içeriğini SearchDocument modeline dönüştürür.
 *
 * Bu parser'ın amacı:
 * - XML içindeki ekran başlıklarını çıkarmak
 * - Açıklama metinlerini çıkarmak
 * - Aksiyon metinlerini çıkarmak
 * - Teknik tokenları çıkarmak
 * - Search engine'ler için birleşik searchText alanı üretmek
 */
@Component
public class WorkflowXmlParser {

    private final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

    public SearchDocument parse(WorkflowDocument document) {
        List<String> screenTitles = new ArrayList<>();
        List<String> screenDescriptions = new ArrayList<>();
        List<String> actionTexts = new ArrayList<>();
        Set<String> technicalTokens = new LinkedHashSet<>();

        String xmlContent = document.getXmlContent();

        try {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(xmlContent));

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String elementName = reader.getLocalName();

                    switch (elementName) {
                        case "workflow" -> extractWorkflowAttributes(reader, technicalTokens);
                        case "screen" -> extractScreenAttributes(reader, technicalTokens);
                        case "field" -> extractFieldAttributes(reader, technicalTokens);
                        case "action" -> extractAction(reader, actionTexts, technicalTokens);
                        case "transition" -> extractTransitionAttributes(reader, technicalTokens);
                        case "rule" -> extractRuleAttributes(reader, technicalTokens);
                        case "title" -> addText(reader.getElementText(), screenTitles);
                        case "description" -> addText(reader.getElementText(), screenDescriptions);
                        case "name" -> addText(reader.getElementText(), screenDescriptions);
                        default -> {
                            // Şimdilik diğer XML elementlerini ignore ediyoruz.
                            // Gerektiğinde condition, validation gibi alanlar ayrıca ayrıştırılabilir.
                        }
                    }
                }
            }

            reader.close();
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Workflow XML parse edilemedi. workflowCode=" + document.getWorkflowCode(),
                    exception
            );
        }

        String searchText = buildSearchText(
                document,
                screenTitles,
                screenDescriptions,
                actionTexts,
                technicalTokens
        );

        return new SearchDocument(
                String.valueOf(document.getId()),
                document.getId(),
                document.getWorkflowCode(),
                document.getWorkflowName(),
                document.getStatus(),
                document.getDomain(),
                screenTitles,
                screenDescriptions,
                actionTexts,
                List.copyOf(technicalTokens),
                searchText,
                document.getXmlContent(),
                document.getXmlSizeKb()
        );
    }

    private void extractWorkflowAttributes(XMLStreamReader reader, Set<String> technicalTokens) {
        addText(reader.getAttributeValue(null, "id"), technicalTokens);
        addText(reader.getAttributeValue(null, "version"), technicalTokens);
    }

    private void extractScreenAttributes(XMLStreamReader reader, Set<String> technicalTokens) {
        addText(reader.getAttributeValue(null, "id"), technicalTokens);
        addText(reader.getAttributeValue(null, "type"), technicalTokens);
    }

    private void extractFieldAttributes(XMLStreamReader reader, Set<String> technicalTokens) {
        addText(reader.getAttributeValue(null, "name"), technicalTokens);
        addText(reader.getAttributeValue(null, "label"), technicalTokens);
        addText(reader.getAttributeValue(null, "required"), technicalTokens);
    }

    private void extractAction(
            XMLStreamReader reader,
            List<String> actionTexts,
            Set<String> technicalTokens
    ) throws Exception {
        addText(reader.getAttributeValue(null, "code"), technicalTokens);
        addText(reader.getElementText(), actionTexts);
    }

    private void extractTransitionAttributes(XMLStreamReader reader, Set<String> technicalTokens) {
        addText(reader.getAttributeValue(null, "from"), technicalTokens);
        addText(reader.getAttributeValue(null, "to"), technicalTokens);
    }

    private void extractRuleAttributes(XMLStreamReader reader, Set<String> technicalTokens) {
        addText(reader.getAttributeValue(null, "id"), technicalTokens);
    }

    private void addText(String value, List<String> target) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            target.add(normalized);
        }
    }

    private void addText(String value, Set<String> target) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            target.add(normalized);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String buildSearchText(
            WorkflowDocument document,
            List<String> screenTitles,
            List<String> screenDescriptions,
            List<String> actionTexts,
            Set<String> technicalTokens
    ) {
        List<String> parts = new ArrayList<>();

        addText(document.getWorkflowCode(), parts);
        addText(document.getWorkflowName(), parts);
        addText(document.getStatus(), parts);
        addText(document.getDomain(), parts);

        parts.addAll(screenTitles);
        parts.addAll(screenDescriptions);
        parts.addAll(actionTexts);
        parts.addAll(technicalTokens);

        return String.join(" ", parts);
    }
}