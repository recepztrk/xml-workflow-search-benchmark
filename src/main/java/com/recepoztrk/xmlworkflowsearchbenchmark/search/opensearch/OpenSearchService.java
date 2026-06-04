package com.recepoztrk.xmlworkflowsearchbenchmark.search.opensearch;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.client.SearchEngineClient;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchHitDto;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.entity.WorkflowDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.repository.WorkflowDocumentRepository;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.service.WorkflowXmlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenSearch alternatif arama motoru entegrasyon servisi.
 *
 * RAW_XML:
 * - Mevcut sistem yaklaşımına yakındır.
 * - XML parse edilmeden xmlContent alanına indexlenir.
 * - Arama doğrudan xmlContent üzerinde yapılır.
 *
 * EXTRACTED_DOCUMENT:
 * - XML parse edilir.
 * - SearchDocument alanları OpenSearch'e aktarılır.
 * - Arama workflowName, screenTitles, descriptions, actions ve searchText gibi alanlarda yapılır.
 */
@Service
public class OpenSearchService implements SearchEngineClient {

    private static final String ENGINE_NAME = "opensearch";

    private final WorkflowDocumentRepository workflowRepository;
    private final WorkflowXmlParser xmlParser;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;
    private final String indexName;

    public OpenSearchService(
            WorkflowDocumentRepository workflowRepository,
            WorkflowXmlParser xmlParser,
            @Value("${search.opensearch.base-url}") String baseUrl,
            @Value("${search.opensearch.index-name}") String indexName
    ) {
        this.workflowRepository = workflowRepository;
        this.xmlParser = xmlParser;
        this.jsonMapper = JsonMapper.builder().build();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.indexName = indexName;
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }

    public String health() {
        return restClient.get()
                .uri("/")
                .retrieve()
                .body(String.class);
    }

    /**
     * Seçilen moda göre OpenSearch indexini sıfırdan oluşturur.
     */
    public void recreateIndex(SearchMode mode) {
        SearchMode effectiveMode = mode == null ? SearchMode.RAW_XML : mode;

        deleteIndexIfExists();

        Map<String, Object> requestBody = createIndexMapping(effectiveMode);

        restClient.put()
                .uri("/{indexName}", indexName)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * PostgreSQL'deki workflow kayıtlarını seçilen moda göre OpenSearch'e indexler.
     */
    @Override
    public IndexOperationResult reindexAll(SearchMode mode) {
        SearchMode effectiveMode = mode == null ? SearchMode.RAW_XML : mode;

        recreateIndex(effectiveMode);

        List<WorkflowDocument> documents = workflowRepository.findAll();

        for (WorkflowDocument workflowDocument : documents) {
            if (effectiveMode == SearchMode.RAW_XML) {
                indexRawDocument(workflowDocument);
            } else {
                SearchDocument searchDocument = xmlParser.parse(workflowDocument);
                indexExtractedDocument(searchDocument);
            }
        }

        refreshIndex();

        return new IndexOperationResult(
                ENGINE_NAME,
                indexName,
                documents.size(),
                "All workflow documents indexed into OpenSearch successfully. mode=" + effectiveMode
        );
    }

    /**
     * Seçilen moda göre OpenSearch üzerinde full-text search çalıştırır.
     */
    @Override
    public SearchEngineResult search(String query, int limit, SearchMode mode) {
        SearchMode effectiveMode = mode == null ? SearchMode.RAW_XML : mode;

        Map<String, Object> requestBody = effectiveMode == SearchMode.RAW_XML
                ? createRawXmlSearchRequest(query, limit)
                : createExtractedSearchRequest(query, limit);

        long startNs = System.nanoTime();

        String responseBody = restClient.post()
                .uri("/{indexName}/_search", indexName)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        long tookMs = (System.nanoTime() - startNs) / 1_000_000;

        return parseSearchResponse(query, tookMs, responseBody);
    }

    /**
     * EXTRACTED_DOCUMENT modu için parse edilmiş SearchDocument indexleme.
     */
    private void indexExtractedDocument(SearchDocument document) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", document.id());
        body.put("databaseId", document.databaseId());
        body.put("workflowCode", document.workflowCode());
        body.put("workflowName", document.workflowName());
        body.put("status", document.status());
        body.put("domain", document.domain());
        body.put("screenTitles", document.screenTitles());
        body.put("screenDescriptions", document.screenDescriptions());
        body.put("actionTexts", document.actionTexts());
        body.put("technicalTokens", document.technicalTokens());
        body.put("searchText", document.searchText());
        body.put("xmlContent", document.xmlContent());
        body.put("xmlSizeKb", document.xmlSizeKb());

        restClient.put()
                .uri("/{indexName}/_doc/{id}", indexName, document.id())
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * RAW_XML modu için ham XML indexleme.
     */
    private void indexRawDocument(WorkflowDocument document) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", String.valueOf(document.getId()));
        body.put("databaseId", document.getId());
        body.put("workflowCode", document.getWorkflowCode());
        body.put("workflowName", document.getWorkflowName());
        body.put("status", document.getStatus());
        body.put("domain", document.getDomain());
        body.put("xmlContent", document.getXmlContent());
        body.put("xmlSizeKb", document.getXmlSizeKb());

        restClient.put()
                .uri("/{indexName}/_doc/{id}", indexName, document.getId())
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private void deleteIndexIfExists() {
        try {
            restClient.delete()
                    .uri("/{indexName}", indexName)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // Index yoksa sorun değil.
        }
    }

    private void refreshIndex() {
        restClient.post()
                .uri("/{indexName}/_refresh", indexName)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Moda göre OpenSearch mapping oluşturur.
     *
     * RAW_XML:
     * - xmlContent indexlenir.
     *
     * EXTRACTED_DOCUMENT:
     * - Parse edilmiş alanlar indexlenir.
     * - xmlContent _source içinde saklanır ama search indexine dahil edilmez.
     */
    private Map<String, Object> createIndexMapping(SearchMode mode) {
        Map<String, Object> properties = new LinkedHashMap<>();

        properties.put("id", keywordField());
        properties.put("databaseId", longField());
        properties.put("workflowCode", keywordField());
        properties.put("workflowName", textField());
        properties.put("status", keywordField());
        properties.put("domain", keywordField());
        properties.put("xmlSizeKb", integerField());

        if (mode == SearchMode.EXTRACTED_DOCUMENT) {
            properties.put("screenTitles", textField());
            properties.put("screenDescriptions", textField());
            properties.put("actionTexts", textField());
            properties.put("technicalTokens", keywordField());
            properties.put("searchText", textField());

            Map<String, Object> xmlContentField = new LinkedHashMap<>();
            xmlContentField.put("type", "text");
            xmlContentField.put("index", false);
            properties.put("xmlContent", xmlContentField);
        } else {
            properties.put("xmlContent", textField());
        }

        return Map.of(
                "mappings", Map.of(
                        "properties", properties
                )
        );
    }

    /**
     * EXTRACTED_DOCUMENT arama isteği.
     */
    private Map<String, Object> createExtractedSearchRequest(String query, int limit) {
        return Map.of(
                "size", limit,
                "_source", List.of(
                        "id",
                        "databaseId",
                        "workflowCode",
                        "workflowName",
                        "status",
                        "domain",
                        "xmlSizeKb"
                ),
                "query", Map.of(
                        "bool", Map.of(
                                "must", List.of(
                                        Map.of(
                                                "multi_match", Map.of(
                                                        "query", query,
                                                        "fields", List.of(
                                                                "workflowName^3",
                                                                "screenTitles^2",
                                                                "screenDescriptions",
                                                                "actionTexts",
                                                                "searchText"
                                                        )
                                                )
                                        )
                                ),
                                "filter", List.of(
                                        Map.of(
                                                "term", Map.of(
                                                        "status", "ACTIVE"
                                                )
                                        )
                                )
                        )
                )
        );
    }

    /**
     * RAW_XML arama isteği.
     */
    private Map<String, Object> createRawXmlSearchRequest(String query, int limit) {
        return Map.of(
                "size", limit,
                "_source", List.of(
                        "id",
                        "databaseId",
                        "workflowCode",
                        "workflowName",
                        "status",
                        "domain",
                        "xmlSizeKb"
                ),
                "query", Map.of(
                        "bool", Map.of(
                                "must", List.of(
                                        Map.of(
                                                "match", Map.of(
                                                        "xmlContent", query
                                                )
                                        )
                                ),
                                "filter", List.of(
                                        Map.of(
                                                "term", Map.of(
                                                        "status", "ACTIVE"
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private SearchEngineResult parseSearchResponse(String query, long tookMs, String responseBody) {
        try {
            JsonNode root = jsonMapper.readTree(responseBody);
            JsonNode hitsNode = root.path("hits");
            int totalHits = hitsNode.path("total").path("value").asInt();

            List<SearchHitDto> hits = new ArrayList<>();

            for (JsonNode hitNode : hitsNode.path("hits")) {
                JsonNode source = hitNode.path("_source");

                hits.add(new SearchHitDto(
                        hitNode.path("_id").asText(),
                        hitNode.path("_score").asDouble(),
                        source.path("workflowCode").asText(),
                        source.path("workflowName").asText(),
                        source.path("status").asText(),
                        source.path("domain").asText(),
                        source.path("xmlSizeKb").asInt()
                ));
            }

            return new SearchEngineResult(
                    ENGINE_NAME,
                    query,
                    tookMs,
                    totalHits,
                    hits
            );
        } catch (Exception exception) {
            throw new IllegalStateException("OpenSearch search response parse edilemedi.", exception);
        }
    }

    private Map<String, Object> textField() {
        return Map.of("type", "text");
    }

    private Map<String, Object> keywordField() {
        return Map.of("type", "keyword");
    }

    private Map<String, Object> integerField() {
        return Map.of("type", "integer");
    }

    private Map<String, Object> longField() {
        return Map.of("type", "long");
    }
}