package com.recepoztrk.xmlworkflowsearchbenchmark.search.elasticsearch;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.client.SearchEngineClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchHitDto;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.client.SearchEngineClient;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.entity.WorkflowDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.repository.WorkflowDocumentRepository;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.service.WorkflowXmlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Elasticsearch baseline entegrasyon servisi.
 *
 * Bu servis şimdilik üç temel iş yapar:
 * 1. Elasticsearch indexini oluşturur.
 * 2. PostgreSQL'deki workflow kayıtlarını SearchDocument'a çevirip Elasticsearch'e indexler.
 * 3. Elasticsearch üzerinde full-text search çalıştırır.
 *
 * Not:
 * Bu aşama benchmark değildir. Bu aşama sadece Elasticsearch baseline'ın çalışır hale getirilmesidir.
 */
@Service
public class ElasticsearchService implements SearchEngineClient {

    private static final String ENGINE_NAME = "elasticsearch";

    private final WorkflowDocumentRepository workflowRepository;
    private final WorkflowXmlParser xmlParser;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;
    private final String indexName;

    public ElasticsearchService(
            WorkflowDocumentRepository workflowRepository,
            WorkflowXmlParser xmlParser,
            JsonMapper jsonMapper,
            @Value("${search.elasticsearch.base-url}") String baseUrl,
            @Value("${search.elasticsearch.index-name}") String indexName
    ) {
        this.workflowRepository = workflowRepository;
        this.xmlParser = xmlParser;
        this.jsonMapper = jsonMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.indexName = indexName;
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }

    /**
     * Elasticsearch bağlantısını basit şekilde kontrol eder.
     */
    public String health() {
        return restClient.get()
                .uri("/")
                .retrieve()
                .body(String.class);
    }

    /**
     * Indexi sıfırdan oluşturur.
     *
     * Şimdilik xmlContent alanını _source içinde tutuyoruz ama indexlemiyoruz.
     * Bunun sebebi:
     * - Extracted SearchDocument yaklaşımını ana senaryo olarak kullanmak.
     * - Büyük XML'in text indexini şişirmesini engellemek.
     * - İleride full XML response maliyetini test edebilmek.
     */
    public void recreateIndex() {
        deleteIndexIfExists();

        Map<String, Object> requestBody = createIndexMapping();

        restClient.put()
                .uri("/{indexName}", indexName)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * PostgreSQL'deki tüm workflow kayıtlarını Elasticsearch'e indexler.
     */
    public IndexOperationResult reindexAll() {
        recreateIndex();

        List<WorkflowDocument> documents = workflowRepository.findAll();

        for (WorkflowDocument workflowDocument : documents) {
            SearchDocument searchDocument = xmlParser.parse(workflowDocument);
            indexDocument(searchDocument);
        }

        refreshIndex();

        return new IndexOperationResult(
                ENGINE_NAME,
                indexName,
                documents.size(),
                "All workflow documents indexed into Elasticsearch successfully."
        );
    }

    /**
     * Elasticsearch üzerinde basit multi-field full-text search çalıştırır.
     *
     * Burada multi_match kullanıyoruz:
     * - workflowName daha yüksek ağırlık alıyor.
     * - screenTitles ikinci öncelikte.
     * - açıklama/action/searchText alanları normal ağırlıkta.
     *
     * Elasticsearch multi_match query birden fazla field üzerinde arama yapmayı ve field boost
     * kullanımını destekler.
     */
    public SearchEngineResult search(String query, int limit) {
        Map<String, Object> requestBody = createSearchRequest(query, limit);

        long startNs = System.nanoTime();

        String responseBody = restClient.post()
                .uri("/{indexName}/_search", indexName)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        long tookMs = (System.nanoTime() - startNs) / 1_000_000;

        return parseSearchResponse(query, tookMs, responseBody);
    }

    private void indexDocument(SearchDocument document) {
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

    private void deleteIndexIfExists() {
        try {
            restClient.delete()
                    .uri("/{indexName}", indexName)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // Index yoksa sorun değil. Zaten yeniden oluşturacağız.
        }
    }

    private void refreshIndex() {
        restClient.post()
                .uri("/{indexName}/_refresh", indexName)
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> createIndexMapping() {
        Map<String, Object> properties = new LinkedHashMap<>();

        properties.put("id", keywordField());
        properties.put("databaseId", longField());
        properties.put("workflowCode", keywordField());
        properties.put("workflowName", textField());
        properties.put("status", keywordField());
        properties.put("domain", keywordField());
        properties.put("screenTitles", textField());
        properties.put("screenDescriptions", textField());
        properties.put("actionTexts", textField());
        properties.put("technicalTokens", keywordField());
        properties.put("searchText", textField());
        properties.put("xmlSizeKb", integerField());

        /*
         * xmlContent _source içinde saklanacak ama search indexine dahil edilmeyecek.
         * Böylece extracted searchText senaryosunda büyük XML indexi şişirmez.
         */
        Map<String, Object> xmlContentField = new LinkedHashMap<>();
        xmlContentField.put("type", "text");
        xmlContentField.put("index", false);
        properties.put("xmlContent", xmlContentField);

        return Map.of(
                "mappings", Map.of(
                        "properties", properties
                )
        );
    }

    private Map<String, Object> createSearchRequest(String query, int limit) {
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
            throw new IllegalStateException("Elasticsearch search response parse edilemedi.", exception);
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