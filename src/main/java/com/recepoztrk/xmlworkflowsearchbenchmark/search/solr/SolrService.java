package com.recepoztrk.xmlworkflowsearchbenchmark.search.solr;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchHitDto;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.client.SearchEngineClient;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.entity.WorkflowDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.repository.WorkflowDocumentRepository;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.service.WorkflowXmlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

/**
 * Apache Solr entegrasyon servisi.
 *
 * Elasticsearch/OpenSearch tarafında mapping + Query DSL yaklaşımı kullanmıştık.
 * Solr tarafında ise core/collection + field suffix + query parser yaklaşımı kullanıyoruz.
 *
 * Bu servis şimdilik:
 * 1. Solr bağlantısını kontrol eder.
 * 2. Solr core içindeki dokümanları temizler.
 * 3. PostgreSQL'deki workflow kayıtlarını SearchDocument'a çevirip Solr'a indexler.
 * 4. eDisMax query parser ile full-text search çalıştırır.
 */
@Service
public class SolrService implements SearchEngineClient {

    private static final String ENGINE_NAME = "solr";

    private final WorkflowDocumentRepository workflowRepository;
    private final WorkflowXmlParser xmlParser;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;
    private final String collectionName;

    public SolrService(
            WorkflowDocumentRepository workflowRepository,
            WorkflowXmlParser xmlParser,
            @Value("${search.solr.base-url}") String baseUrl,
            @Value("${search.solr.collection-name}") String collectionName
    ) {
        this.workflowRepository = workflowRepository;
        this.xmlParser = xmlParser;
        this.jsonMapper = JsonMapper.builder().build();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(60_000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        this.collectionName = collectionName;
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }

    /**
     * Solr core durumunu kontrol eder.
     */
    public String health() {
        return restClient.get()
                .uri("/admin/cores?action=STATUS&wt=json")
                .retrieve()
                .body(String.class);
    }

    /**
     * Solr core içindeki mevcut dokümanları temizler ve PostgreSQL'deki workflow kayıtlarını yeniden indexler.
     */
    public IndexOperationResult reindexAll() {
        deleteAllDocuments();

        List<WorkflowDocument> workflowDocuments = workflowRepository.findAll();

        for (WorkflowDocument workflowDocument : workflowDocuments) {
            SearchDocument searchDocument = xmlParser.parse(workflowDocument);
            indexDocument(searchDocument);
        }

        commit();

        return new IndexOperationResult(
                ENGINE_NAME,
                collectionName,
                workflowDocuments.size(),
                "All workflow documents indexed into Solr successfully."
        );
    }

    /**
     * Solr üzerinde eDisMax query parser ile full-text search çalıştırır.
     *
     * eDisMax yaklaşımı, birden fazla field üzerinde ağırlıklı arama yapmak için uygundur.
     * Elasticsearch/OpenSearch tarafındaki multi_match query'ye kabaca karşılık gelen bir query stratejisi olarak kullanılabilir.
     */
    public SearchEngineResult search(String query, int limit) {
        long startNs = System.nanoTime();

        String responseBody = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{collection}/select")
                        .queryParam("q", query)
                        .queryParam("defType", "edismax")
                        .queryParam(
                                "qf",
                                "workflowName_txt^3 screenTitles_txt^2 screenDescriptions_txt actionTexts_txt searchText_txt technicalTokens_txt"
                        )
                        .queryParam("fq", "status_s:ACTIVE")
                        .queryParam("rows", limit)
                        .queryParam("fl", "id,score,workflowCode_s,workflowName_txt,status_s,domain_s,xmlSizeKb_i")
                        .queryParam("wt", "json")
                        .build(collectionName))
                .retrieve()
                .body(String.class);

        long tookMs = (System.nanoTime() - startNs) / 1_000_000;

        return parseSearchResponse(query, tookMs, responseBody);
    }

    private String joinTexts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return String.join(" ", values);
    }

    private void deleteAllDocuments() {
        Map<String, Object> deleteBody = new LinkedHashMap<>();
        deleteBody.put("delete", Map.of("query", "*:*"));
        deleteBody.put("commit", Map.of());

        restClient.post()
                .uri("/{collection}/update?wt=json", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(deleteBody)
                .retrieve()
                .toBodilessEntity();
    }

    private void indexDocument(SearchDocument document) {
        Map<String, Object> solrDocument = new LinkedHashMap<>();

        solrDocument.put("id", document.id());
        solrDocument.put("databaseId_l", document.databaseId());
        solrDocument.put("workflowCode_s", document.workflowCode());
        solrDocument.put("workflowName_txt", document.workflowName());
        solrDocument.put("status_s", document.status());
        solrDocument.put("domain_s", document.domain());

        /*
         * Solr tarafında dynamic field'ların multi-valued olup olmadığı garanti değil.
         * Bu yüzden List<String> alanları tek string'e çeviriyoruz.
         */
        solrDocument.put("screenTitles_txt", joinTexts(document.screenTitles()));
        solrDocument.put("screenDescriptions_txt", joinTexts(document.screenDescriptions()));
        solrDocument.put("actionTexts_txt", joinTexts(document.actionTexts()));
        solrDocument.put("technicalTokens_txt", joinTexts(document.technicalTokens()));

        solrDocument.put("searchText_txt", document.searchText());
        solrDocument.put("xmlContent_txt", document.xmlContent());
        solrDocument.put("xmlSizeKb_i", document.xmlSizeKb());

        Map<String, Object> addBody = new LinkedHashMap<>();
        addBody.put("add", Map.of("doc", solrDocument));

        restClient.post()
                .uri("/{collection}/update?wt=json", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(addBody)
                .retrieve()
                .toBodilessEntity();
    }

    private void commit() {
        Map<String, Object> commitBody = Map.of("commit", Map.of());

        restClient.post()
                .uri("/{collection}/update?wt=json", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(commitBody)
                .retrieve()
                .toBodilessEntity();
    }

    private String readStringField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);

        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return "";
        }

        if (fieldNode.isArray()) {
            if (fieldNode.size() == 0) {
                return "";
            }

            return fieldNode.get(0).asText();
        }

        return fieldNode.asText();
    }

    private Integer readIntegerField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.path(fieldName);

        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return 0;
        }

        if (fieldNode.isArray()) {
            if (fieldNode.size() == 0) {
                return 0;
            }

            return fieldNode.get(0).asInt();
        }

        return fieldNode.asInt();
    }

    private SearchEngineResult parseSearchResponse(String query, long tookMs, String responseBody) {
        try {
            JsonNode root = jsonMapper.readTree(responseBody);
            JsonNode responseNode = root.path("response");

            int totalHits = responseNode.path("numFound").asInt();

            List<SearchHitDto> hits = new ArrayList<>();

            for (JsonNode docNode : responseNode.path("docs")) {
                hits.add(new SearchHitDto(
                        readStringField(docNode, "id"),
                        docNode.path("score").asDouble(),
                        readStringField(docNode, "workflowCode_s"),
                        readStringField(docNode, "workflowName_txt"),
                        readStringField(docNode, "status_s"),
                        readStringField(docNode, "domain_s"),
                        readIntegerField(docNode, "xmlSizeKb_i")
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
            throw new IllegalStateException("Solr search response parse edilemedi.", exception);
        }
    }
}
