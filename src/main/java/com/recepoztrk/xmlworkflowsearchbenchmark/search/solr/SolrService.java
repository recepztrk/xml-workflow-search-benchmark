package com.recepoztrk.xmlworkflowsearchbenchmark.search.solr;

import com.recepoztrk.xmlworkflowsearchbenchmark.search.client.SearchEngineClient;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.IndexOperationResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.ResponseMode;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchEngineResult;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchHitDto;
import com.recepoztrk.xmlworkflowsearchbenchmark.search.model.SearchMode;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.entity.WorkflowDocument;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.repository.WorkflowDocumentRepository;
import com.recepoztrk.xmlworkflowsearchbenchmark.workflow.service.WorkflowXmlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Apache Solr entegrasyon servisi.
 * <p>
 * RAW_XML:
 * - Mevcut sistem yaklaşımına yakındır.
 * - XML parse edilmeden xmlContent_txt alanına indexlenir.
 * - Arama doğrudan xmlContent_txt üzerinde yapılır.
 * <p>
 * EXTRACTED_DOCUMENT:
 * - XML parse edilir.
 * - SearchDocument alanları Solr'a aktarılır.
 * - Arama workflowName, screenTitles, descriptions, actions, searchText gibi alanlarda yapılır.
 * <p>
 * ResponseMode:
 * - METADATA_ONLY: Search sonucunda sadece metadata alanları döndürülür.
 * - FULL_XML_RESPONSE: Metadata alanlarına ek olarak xmlContent_txt de döndürülür.
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

        /*
         * Solr/Jetty tarafında Java HttpClient ile HTTP/2 stream reset problemi yaşanabildi.
         * Bu nedenle Solr entegrasyonunda daha klasik HTTP/1.1 davranışına yakın
         * SimpleClientHttpRequestFactory kullanıyoruz.
         */
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
     * Seçilen moda göre Solr core içindeki dokümanları temizler,
     * gerekli schema field'larını hazırlar ve PostgreSQL'deki kayıtları yeniden indexler.
     */
    @Override
    public IndexOperationResult reindexAll(SearchMode mode) {
        SearchMode effectiveMode = mode == null ? SearchMode.RAW_XML : mode;

        deleteAllDocuments();
        ensureSchemaFields(effectiveMode);

        List<WorkflowDocument> workflowDocuments = workflowRepository.findAll();

        for (WorkflowDocument workflowDocument : workflowDocuments) {
            if (effectiveMode == SearchMode.RAW_XML) {
                indexRawDocument(workflowDocument);
            } else {
                SearchDocument searchDocument = xmlParser.parse(workflowDocument);
                indexExtractedDocument(searchDocument);
            }
        }

        commit();

        return new IndexOperationResult(
                ENGINE_NAME,
                collectionName,
                workflowDocuments.size(),
                "All workflow documents indexed into Solr successfully. mode=" + effectiveMode
        );
    }

    /**
     * Seçilen SearchMode ve ResponseMode'a göre Solr üzerinde arama yapar.
     */
    @Override
    public SearchEngineResult search(
            String query,
            int limit,
            SearchMode mode,
            ResponseMode responseMode
    ) {
        SearchMode effectiveMode = mode == null ? SearchMode.RAW_XML : mode;
        ResponseMode effectiveResponseMode = responseMode == null
                ? ResponseMode.METADATA_ONLY
                : responseMode;

        String queryFields = effectiveMode == SearchMode.RAW_XML
                ? "xmlContent_txt"
                : "workflowName_txt^3 screenTitles_txt^2 screenDescriptions_txt actionTexts_txt searchText_txt technicalTokens_txt";

        long startNs = System.nanoTime();

        String responseBody = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{collection}/select")
                        .queryParam("q", query)
                        .queryParam("defType", "edismax")
                        .queryParam("qf", queryFields)
                        .queryParam("fq", "status_s:ACTIVE")
                        .queryParam("rows", limit)
                        .queryParam("fl", fieldList(effectiveResponseMode))
                        .queryParam("wt", "json")
                        .build(collectionName))
                .retrieve()
                .body(String.class);

        double tookMs = (System.nanoTime() - startNs) / 1_000_000.0;

        return parseSearchResponse(query, tookMs, responseBody);
    }

    /**
     * Solr schema field'larını seçilen moda göre hazırlar.
     * <p>
     * Solr'da Elasticsearch/OpenSearch mapping mantığı yerine schema field yaklaşımı vardır.
     * Bu yüzden field'ları açık şekilde tanımlıyoruz.
     */
    private void ensureSchemaFields(SearchMode mode) {
        addOrReplaceField("databaseId_l", "plong", true, true);
        addOrReplaceField("workflowCode_s", "string", true, true);
        addOrReplaceField("workflowName_txt", "text_general", true, true);
        addOrReplaceField("status_s", "string", true, true);
        addOrReplaceField("domain_s", "string", true, true);
        addOrReplaceField("xmlSizeKb_i", "pint", true, true);

        if (mode == SearchMode.EXTRACTED_DOCUMENT) {
            addOrReplaceField("screenTitles_txt", "text_general", true, true);
            addOrReplaceField("screenDescriptions_txt", "text_general", true, true);
            addOrReplaceField("actionTexts_txt", "text_general", true, true);
            addOrReplaceField("technicalTokens_txt", "text_general", true, true);
            addOrReplaceField("searchText_txt", "text_general", true, true);
        }

        /*
         * RAW_XML modunda asıl arama bu alanda yapılır.
         * EXTRACTED_DOCUMENT modunda da XML saklanabilir.
         */
        addOrReplaceField("xmlContent_txt", "text_general", true, true);
    }

    private void addOrReplaceField(String name, String type, boolean indexed, boolean stored) {
        Map<String, Object> fieldDefinition = new LinkedHashMap<>();
        fieldDefinition.put("name", name);
        fieldDefinition.put("type", type);
        fieldDefinition.put("indexed", indexed);
        fieldDefinition.put("stored", stored);

        try {
            addField(fieldDefinition);
        } catch (HttpClientErrorException.BadRequest exception) {
            replaceField(fieldDefinition);
        }
    }

    private void addField(Map<String, Object> fieldDefinition) {
        Map<String, Object> body = Map.of("add-field", fieldDefinition);

        restClient.post()
                .uri("/{collection}/schema?wt=json", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private void replaceField(Map<String, Object> fieldDefinition) {
        Map<String, Object> body = Map.of("replace-field", fieldDefinition);

        restClient.post()
                .uri("/{collection}/schema?wt=json", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
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

    /**
     * Mevcut sistem yaklaşımına uygun ham XML indexleme.
     */
    private void indexRawDocument(WorkflowDocument document) {
        Map<String, Object> solrDocument = new LinkedHashMap<>();

        solrDocument.put("id", String.valueOf(document.getId()));
        solrDocument.put("databaseId_l", document.getId());
        solrDocument.put("workflowCode_s", document.getWorkflowCode());
        solrDocument.put("workflowName_txt", document.getWorkflowName());
        solrDocument.put("status_s", document.getStatus());
        solrDocument.put("domain_s", document.getDomain());
        solrDocument.put("xmlContent_txt", document.getXmlContent());
        solrDocument.put("xmlSizeKb_i", document.getXmlSizeKb());

        Map<String, Object> addBody = new LinkedHashMap<>();
        addBody.put("add", Map.of("doc", solrDocument));

        restClient.post()
                .uri("/{collection}/update?wt=json", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(addBody)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * XML parse edilmiş SearchDocument yaklaşımı.
     */
    private void indexExtractedDocument(SearchDocument document) {
        Map<String, Object> solrDocument = new LinkedHashMap<>();

        solrDocument.put("id", document.id());
        solrDocument.put("databaseId_l", document.databaseId());
        solrDocument.put("workflowCode_s", document.workflowCode());
        solrDocument.put("workflowName_txt", document.workflowName());
        solrDocument.put("status_s", document.status());
        solrDocument.put("domain_s", document.domain());

        /*
         * Solr tarafında multi-valued field karmaşası yaşamamak için
         * List<String> alanları tek text değerine çeviriyoruz.
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

    /**
     * ResponseMode'a göre Solr select response içinde dönecek field listesini belirler.
     */
    private String fieldList(ResponseMode responseMode) {
        String fields = "id,score,workflowCode_s,workflowName_txt,status_s,domain_s,xmlSizeKb_i";

        if (responseMode == ResponseMode.FULL_XML_RESPONSE) {
            fields += ",xmlContent_txt";
        }

        return fields;
    }

    private String joinTexts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return String.join(" ", values);
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

    private String readNullableStringField(JsonNode node, String fieldName) {
        String value = readStringField(node, fieldName);

        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

    private SearchEngineResult parseSearchResponse(String query, double tookMs, String responseBody) {
        try {
            JsonNode root = jsonMapper.readTree(responseBody);
            JsonNode responseNode = root.path("response");

            int totalHits = responseNode.path("numFound").asInt();

            List<SearchHitDto> hits = new ArrayList<>();

            for (JsonNode docNode : responseNode.path("docs")) {
                String xmlContent = readNullableStringField(docNode, "xmlContent_txt");

                Integer responseSizeKb = calculateHitResponseSizeKb(docNode, xmlContent);

                hits.add(new SearchHitDto(
                        readStringField(docNode, "id"),
                        docNode.path("score").asDouble(),
                        readStringField(docNode, "workflowCode_s"),
                        readStringField(docNode, "workflowName_txt"),
                        readStringField(docNode, "status_s"),
                        readStringField(docNode, "domain_s"),
                        readIntegerField(docNode, "xmlSizeKb_i"),
                        xmlContent,
                        responseSizeKb
                ));
            }

            int totalResponseSizeKb = hits.stream()
                    .map(SearchHitDto::responseSizeKb)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            return new SearchEngineResult(
                    ENGINE_NAME,
                    query,
                    tookMs,
                    totalHits,
                    totalResponseSizeKb,
                    hits
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Solr search response parse edilemedi.", exception);
        }
    }

    private Integer calculateHitResponseSizeKb(JsonNode docNode, String xmlContent) {
        String approximatePayload = readStringField(docNode, "id")
                + readStringField(docNode, "workflowCode_s")
                + readStringField(docNode, "workflowName_txt")
                + readStringField(docNode, "status_s")
                + readStringField(docNode, "domain_s")
                + readIntegerField(docNode, "xmlSizeKb_i")
                + (xmlContent == null ? "" : xmlContent);

        return calculateSizeKb(approximatePayload);
    }

    private Integer calculateSizeKb(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        return value.getBytes(StandardCharsets.UTF_8).length / 1024;
    }
}