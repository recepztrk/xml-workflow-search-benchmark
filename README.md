# XML Workflow Search Benchmark

Bu proje, PostgreSQL üzerinde tutulan XML tabanlı workflow dokümanları için arama motoru benchmark altyapısı oluşturmak amacıyla geliştirilmiş bir Spring Boot PoC projesidir.

Projenin temel amacı; müşteri temsilcisi platformlarında kullanılan XML workflow dokümanları üzerinde **Elasticsearch**, **OpenSearch** ve **Apache Solr** servislerinin free-text search performansını kontrollü ve tekrar edilebilir şekilde karşılaştırmaktır.

Çalışma iki temel arama stratejisini destekler:

1. **RAW_XML**  
   Mevcut sisteme en yakın senaryodur. XML dokümanı parse edilmeden search engine’e aktarılır ve arama doğrudan ham XML içeriği üzerinde yapılır.

2. **EXTRACTED_DOCUMENT**  
   Alternatif/iyileştirme senaryosudur. XML parse edilir; `workflowName`, `screenTitles`, `screenDescriptions`, `actionTexts`, `technicalTokens` ve `searchText` gibi alanlar çıkarılır. Arama bu normalize edilmiş alanlar üzerinde yapılır.

---

## Problem Bağlamı

İncelenen senaryoda bir **müşteri temsilcisi platformu** bulunmaktadır. Bu platformda müşteri temsilcisinin gördüğü ekranlar ve iş akışları XML dosyaları içinde tanımlanmaktadır.

Mevcut sistemde:

- XML veriler PostgreSQL üzerinde saklanmaktadır.
- Aynı XML veriler Elasticsearch üzerinde de bulunmaktadır.
- Free-text search işlemleri Elasticsearch üzerinden yapılmaktadır.
- Mevcut sistemde XML dokümanları Elasticsearch’e uygulama tarafında parse edilmeden aktarılmaktadır.
- Test kapsamında yaklaşık 2 MB boyutundaki XML dosyalarında arama performansının ölçülmesi hedeflenmektedir.

Bu proje, Elasticsearch’i mevcut sistem/baseline olarak alıp OpenSearch ve Apache Solr alternatiflerini teknik olarak karşılaştırmayı hedefler.

---

## Benchmark Kapsamı

| Servis | Rol |
|---|---|
| Elasticsearch | Mevcut sistem / baseline |
| OpenSearch | Elasticsearch’e en yakın alternatif |
| Apache Solr | Lucene tabanlı olgun kurumsal alternatif |

Bu problem özelinde Meilisearch ve PostgreSQL FTS ana benchmark kapsamına alınmamıştır. Çünkü mevcut sistem zaten Elasticsearch kullanmaktadır ve amaç Elasticsearch’e alternatif olabilecek kurumsal search engine’leri karşılaştırmaktır.

---

## Desteklenen Arama Modları

### 1. RAW_XML

Bu mod mevcut sisteme en yakın yaklaşımı temsil eder.

Veri akışı:

```text
PostgreSQL
    ↓
WorkflowDocument
    ↓
xmlContent
    ↓
Elasticsearch / OpenSearch / Apache Solr
    ↓
xmlContent üzerinde search
```

Bu modda XML parse edilmez. Search engine’e gönderilen temel doküman yapısı şu şekildedir:

```json
{
   "id": "51",
   "databaseId": 51,
   "workflowCode": "WF_BILLING_1",
   "workflowName": "Fatura İtiraz Süreci 1",
   "status": "ACTIVE",
   "domain": "Billing",
   "xmlContent": "<workflow>...</workflow>",
   "xmlSizeKb": 34
}
```

Arama yapılan ana alan:

```text
xmlContent
```

Solr tarafında karşılığı:

```text
xmlContent_txt
```

---

### 2. EXTRACTED_DOCUMENT

Bu mod, XML’in parse edilip daha anlamlı bir search document haline getirildiği alternatif yaklaşımı temsil eder.

Veri akışı:

```text
PostgreSQL
    ↓
WorkflowDocument
    ↓
xmlContent
    ↓
WorkflowXmlParser
    ↓
SearchDocument
    ↓
Elasticsearch / OpenSearch / Apache Solr
    ↓
Ayrıştırılmış alanlar üzerinde search
```

Bu modda search engine’e gönderilen normalize doküman şu alanları içerir:

```text
id
databaseId
workflowCode
workflowName
status
domain
screenTitles
screenDescriptions
actionTexts
technicalTokens
searchText
xmlContent
xmlSizeKb
```

Arama yapılan temel alanlar:

```text
workflowName
screenTitles
screenDescriptions
actionTexts
technicalTokens
searchText
```

Bu mod mevcut sistemi birebir temsil etmez. Ek iyileştirme / teknik katkı senaryosu olarak değerlendirilmelidir.

---

## Benchmark Matrisi

| Senaryo | Elasticsearch | OpenSearch | Apache Solr | Açıklama |
|---|---:|---:|---:|---|
| RAW_XML | Var | Var | Var | Mevcut sisteme en yakın benchmark |
| EXTRACTED_DOCUMENT | Var | Var | Var | XML parse edilirse performans/arama davranışı nasıl değişir? |

---

## Benchmark Sonuç Özeti

Ayrıntılı RAW_XML benchmark sonuçları ayrı raporda tutulmaktadır: [`docs/raw_xml_benchmark_results.md`](docs/raw_xml_benchmark_results.md)

Kısa özet:

- `RAW_XML` modu mevcut sisteme en yakın senaryo olarak test edilmiştir.
- XML boyutu `screenCount` parametresiyle kademeli olarak artırılmıştır.
- Yaklaşık 2 MB XML dokümanı için `screenCount=1200` kullanılmıştır.
- Testler lokal single-node Docker ortamında ve metadata-only response ile yapılmıştır.
- Elasticsearch, 100 adet yaklaşık 2 MB XML dokümanı üzerinde RAW_XML arama senaryosunu kararlı şekilde tamamlamıştır.
- Solr, aynı senaryoda Elasticsearch’e göre daha düşük latency üretmiştir.
- OpenSearch, küçük ve orta ölçekli testlerde çalışmasına rağmen 50 ve 100 adet 2 MB RAW_XML testlerinde 2 GB heap ile kararlı sonuç üretmemiştir.

100 adet yaklaşık 2 MB XML dokümanı üzerinde Elasticsearch + Solr izolasyon testi:

| Engine | Ortalama Avg ms | Ortalama P50 ms | Ortalama P95 ms | Başarı |
|---|---:|---:|---:|---:|
| Elasticsearch | 15.72 | 13.33 | 19.33 | 60/60 |
| Apache Solr | 2.20 | 1.33 | 4.67 | 60/60 |

OpenSearch için not:

```text
OpenSearch, 1 GB heap ile 100 adet yaklaşık 2 MB XML dokümanını RAW_XML modunda reindex ederken circuit_breaking_exception üretmiştir.
Heap 2 GB seviyesine çıkarıldığında reindex başarılı olmuştur; ancak 50 ve 100 dokümanlık 2 MB RAW_XML search testlerinde kararlı sonuç üretmemiştir.
```

Bu sonuçlar production kararı olarak değerlendirilmemelidir. Testler lokal geliştirme ortamında yapılmıştır. Nihai karar için production-like cluster ortamı, gerçek query seti, full XML response senaryosu ve daha kapsamlı arama kalitesi ölçümleri ayrıca gereklidir.


## Mevcut Durum

Tamamlananlar:

- Spring Boot projesi oluşturuldu.
- PostgreSQL Docker Compose ile ayağa kaldırıldı.
- `WorkflowDocument` entity modeli oluşturuldu.
- PostgreSQL üzerinde XML workflow kaydı tutan yapı kuruldu.
- Sentetik XML workflow üreten generator yazıldı.
- XML içeriğini parse eden parser katmanı oluşturuldu.
- Search engine’lere gönderilecek ortak `SearchDocument` modeli oluşturuldu.
- Elasticsearch entegrasyonu tamamlandı.
- OpenSearch entegrasyonu tamamlandı.
- Apache Solr entegrasyonu tamamlandı.
- Üç servis için health, reindex ve search endpointleri oluşturuldu.
- Ortak `SearchEngineClient` interface’i oluşturuldu.
- Ortak benchmark runner geliştirildi.
- `RAW_XML` ve `EXTRACTED_DOCUMENT` modları eklendi.
- Benchmark request/response modellerine `SearchMode` desteği eklendi.
- Warm-up iteration desteği eklendi.
- Measurement iteration desteği eklendi.
- `avg`, `min`, `max`, `p50`, `p95`, `p99` ölçümleri eklendi.
- `RAW_XML` modu için 34 KB, 515 KB, 1 MB ve yaklaşık 2 MB XML dokümanlarıyla lokal benchmark testleri yapıldı.
- 100 adet yaklaşık 2 MB XML dokümanı ile Elasticsearch + Solr izolasyon testi başarıyla tamamlandı.
- OpenSearch tarafında büyük RAW_XML yükünde heap/circuit breaker davranışı gözlemlendi.
- Ayrıntılı RAW_XML benchmark raporu `docs/raw_xml_benchmark_results.md` dosyasına taşındı.

Devam eden / yapılacak işler:

- EXTRACTED_DOCUMENT modunu daha büyük veri setlerinde sınırlı şekilde test etmek
- Search-only response vs full XML response benchmarkı eklemek
- Benchmark response modeline hata mesajı alanı eklemek
- CSV/JSON benchmark raporu üretimi
- Bulk indexing optimizasyonu
- Gerçek query loglarına yakın query seti
- Relevance / arama kalitesi değerlendirmesi
- Production-like cluster testi

## Kullanılan Teknolojiler

- Java 21+
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Elasticsearch
- OpenSearch
- Apache Solr
- Docker Compose
- Maven
- Lombok

---

## Servis Portları

| Servis | Port |
|---|---|
| Spring Boot API | `8080` |
| PostgreSQL | `5434` |
| OpenSearch | `9200` |
| Elasticsearch | `9201` |
| Apache Solr | `8983` |

---

## Proje Yapısı

```text
src/main/java/com/recepoztrk/xmlworkflowsearchbenchmark
├── benchmark
│   ├── controller
│   │   └── BenchmarkController.java
│   ├── model
│   │   ├── BenchmarkMeasurementResult.java
│   │   ├── BenchmarkReindexResponse.java
│   │   ├── BenchmarkRunRequest.java
│   │   └── BenchmarkRunResponse.java
│   └── service
│       └── BenchmarkService.java
│
├── search
│   ├── client
│   │   └── SearchEngineClient.java
│   │
│   ├── elasticsearch
│   │   ├── ElasticsearchController.java
│   │   └── ElasticsearchService.java
│   │
│   ├── opensearch
│   │   ├── OpenSearchController.java
│   │   └── OpenSearchService.java
│   │
│   ├── solr
│   │   ├── SolrController.java
│   │   └── SolrService.java
│   │
│   └── model
│       ├── IndexOperationResult.java
│       ├── SearchDocument.java
│       ├── SearchEngineResult.java
│       ├── SearchHitDto.java
│       └── SearchMode.java
│
├── workflow
│   ├── controller
│   │   └── WorkflowController.java
│   ├── entity
│   │   └── WorkflowDocument.java
│   ├── repository
│   │   └── WorkflowDocumentRepository.java
│   └── service
│       ├── WorkflowSeedService.java
│       ├── WorkflowXmlGenerator.java
│       └── WorkflowXmlParser.java
│
└── XmlWorkflowSearchBenchmarkApplication.java
```

---

## Genel Veri Akışı

### RAW_XML Modu

```text
PostgreSQL
    ↓
WorkflowDocument
    ↓
xmlContent
    ↓
Elasticsearch / OpenSearch / Apache Solr
    ↓
SearchEngineResult
```

### EXTRACTED_DOCUMENT Modu

```text
PostgreSQL
    ↓
WorkflowDocument
    ↓
xmlContent
    ↓
WorkflowXmlParser
    ↓
SearchDocument
    ↓
Elasticsearch / OpenSearch / Apache Solr
    ↓
SearchEngineResult
```

---

## SearchDocument Modeli

`SearchDocument`, XML parse edildikten sonra search engine’lere gönderilecek ortak normalize veri modelidir.

Alanlar:

```text
id
databaseId
workflowCode
workflowName
status
domain
screenTitles
screenDescriptions
actionTexts
technicalTokens
searchText
xmlContent
xmlSizeKb
```

Bu model sadece `EXTRACTED_DOCUMENT` modunda ana arama dokümanı olarak kullanılır.

---

## XML Parser Mantığı

`WorkflowXmlParser`, ham XML içeriğinden şu alanları çıkarır:

| Alan | Açıklama |
|---|---|
| `screenTitles` | XML içindeki ekran başlıkları |
| `screenDescriptions` | Açıklama ve isim metinleri |
| `actionTexts` | Aksiyon/button metinleri |
| `technicalTokens` | Workflow id, screen id, field name, action code gibi teknik tokenlar |
| `searchText` | Arama için oluşturulan birleşik metin alanı |

Bu yapı sayesinde ham XML’in tamamını aramak yerine daha temiz ve anlamlı bir search document üretilebilir.

---

## Docker Servislerini Çalıştırma

Proje kök dizininde:

```bash
docker compose up -d
```

Container durumunu görmek için:

```bash
docker compose ps
```

veya:

```bash
docker ps
```

Servisleri durdurmak için:

```bash
docker compose down
```

Volume’larla birlikte tamamen silmek için:

```bash
docker compose down -v
```

### JVM Heap Notu

Büyük XML testlerinde search engine servisleri için JVM heap değeri önemlidir. 100 adet yaklaşık 2 MB XML dokümanı ile yapılan RAW_XML testlerinde 1 GB OpenSearch heap değeri yetersiz kalmış ve `circuit_breaking_exception` oluşmuştur.

Lokal benchmarklarda Elasticsearch, OpenSearch ve Solr için 2 GB heap kullanılmıştır. Daha yüksek heap değerleri lokal makinede swap kullanımını artırabileceği için sonuçları kirletebilir.


---

## PostgreSQL Bilgileri

```text
Host: localhost
Port: 5434
Database: xml_benchmark
Username: postgres
Password: postgres
```

PostgreSQL bağlantısını test etmek için:

```bash
docker exec -it xml-benchmark-postgres psql -U postgres -d xml_benchmark -c "SELECT current_user, current_database();"
```

Beklenen çıktı:

```text
current_user | current_database
-------------+-----------------
postgres     | xml_benchmark
```

---

## Elasticsearch Bilgileri

Elasticsearch host portu:

```text
http://localhost:9201
```

Doğrudan kontrol:

```bash
curl http://localhost:9201
```

Spring Boot üzerinden kontrol:

```bash
curl "http://localhost:8080/api/elasticsearch/health"
```

---

## OpenSearch Bilgileri

OpenSearch host portu:

```text
http://localhost:9200
```

Doğrudan kontrol:

```bash
curl http://localhost:9200
```

Spring Boot üzerinden kontrol:

```bash
curl "http://localhost:8080/api/opensearch/health"
```

---

## Apache Solr Bilgileri

Solr host portu:

```text
http://localhost:8983
```

Solr Admin UI:

```text
http://localhost:8983/solr
```

Solr core kontrolü:

```bash
curl "http://localhost:8983/solr/admin/cores?action=STATUS&wt=json"
```

Spring Boot üzerinden kontrol:

```bash
curl "http://localhost:8080/api/solr/health"
```

---

## Uygulamayı Çalıştırma

Docker servisleri ayağa kalktıktan sonra:

```bash
./mvnw spring-boot:run
```

Uygulama varsayılan olarak şu adreste çalışır:

```text
http://localhost:8080
```

Compile kontrolü:

```bash
./mvnw clean compile
```

---

## Workflow Endpointleri

### Workflow Sayısını Görüntüleme

```bash
curl "http://localhost:8080/api/workflows/count"
```

Örnek cevap:

```json
{
  "totalCount": 10
}
```

---

### Sentetik XML Workflow Dataset Üretme

```bash
curl -X POST "http://localhost:8080/api/workflows/generate?count=10&screenCount=20"
```

Örnek cevap:

```json
{
  "message": "Workflow XML dataset generated successfully.",
  "totalCount": 10
}
```

Parametreler:

| Parametre | Açıklama |
|---|---|
| `count` | Üretilecek workflow sayısı |
| `screenCount` | Her workflow içinde üretilecek ekran sayısı |

`screenCount` arttıkça XML boyutu büyür. Lokal testlerde gözlenen yaklaşık değerler:

| screenCount | Yaklaşık XML Boyutu |
|---:|---:|
| 20 | 34 KB |
| 300 | 515 KB |
| 600 | 1032 KB |
| 1200 | 2068 KB |

Yaklaşık 2 MB XML dokümanı üretmek için `screenCount=1200` kullanılabilir.

---

### Workflow Özetlerini Listeleme

```bash
curl "http://localhost:8080/api/workflows"
```

Örnek cevap:

```json
[
  {
    "id": 51,
    "workflowCode": "WF_BILLING_1",
    "workflowName": "Fatura İtiraz Süreci 1",
    "status": "ACTIVE",
    "domain": "Billing",
    "xmlSizeKb": 34
  }
]
```

Bu endpoint XML içeriğini döndürmez. Bunun nedeni büyük XML payload’larının response’u gereksiz şişirmesini önlemektir.

---

### Tek Bir Workflow İçin SearchDocument Preview

```bash
curl "http://localhost:8080/api/workflows/51/search-document"
```

Örnek cevap:

```json
{
  "id": "51",
  "databaseId": 51,
  "workflowCode": "WF_BILLING_1",
  "workflowName": "Fatura İtiraz Süreci 1",
  "status": "ACTIVE",
  "domain": "Billing",
  "screenTitleCount": 20,
  "screenDescriptionCount": 62,
  "actionTextCount": 60,
  "technicalTokenCount": 273,
  "searchTextLength": 8376,
  "searchTextPreview": "WF_BILLING_1 Fatura İtiraz Süreci 1 ACTIVE Billing ...",
  "xmlSizeKb": 34
}
```

Not: `generate` endpointi her çalıştırıldığında eski kayıtları siler ve yeni kayıtlar oluşturur. PostgreSQL sequence sıfırlanmadığı için ID değeri sürekli artabilir. Bu yüzden sabit `21` veya `51` gibi ID’lere güvenmek yerine önce `/api/workflows` çıktısından güncel ID alınmalıdır.

---

### İlk N Workflow İçin SearchDocument Preview

```bash
curl "http://localhost:8080/api/workflows/search-documents?limit=3"
```

Bu endpoint ilk N workflow kaydını parse ederek `SearchDocument` preview çıktısı döndürür.

---

## Tekil Search Engine Endpointleri

Tekil engine endpointleri manuel test amacıyla kullanılabilir. Ortak benchmark için `/api/benchmark` endpointleri tercih edilmelidir.

### Elasticsearch

Health:

```bash
curl "http://localhost:8080/api/elasticsearch/health"
```

RAW_XML reindex:

```bash
curl -X POST "http://localhost:8080/api/elasticsearch/reindex?mode=RAW_XML"
```

RAW_XML search:

```bash
curl "http://localhost:8080/api/elasticsearch/search?q=fatura%20itiraz&limit=5&mode=RAW_XML"
```

EXTRACTED_DOCUMENT reindex:

```bash
curl -X POST "http://localhost:8080/api/elasticsearch/reindex?mode=EXTRACTED_DOCUMENT"
```

EXTRACTED_DOCUMENT search:

```bash
curl "http://localhost:8080/api/elasticsearch/search?q=fatura%20itiraz&limit=5&mode=EXTRACTED_DOCUMENT"
```

---

### OpenSearch

Health:

```bash
curl "http://localhost:8080/api/opensearch/health"
```

RAW_XML reindex:

```bash
curl -X POST "http://localhost:8080/api/opensearch/reindex?mode=RAW_XML"
```

RAW_XML search:

```bash
curl "http://localhost:8080/api/opensearch/search?q=fatura%20itiraz&limit=5&mode=RAW_XML"
```

EXTRACTED_DOCUMENT reindex:

```bash
curl -X POST "http://localhost:8080/api/opensearch/reindex?mode=EXTRACTED_DOCUMENT"
```

EXTRACTED_DOCUMENT search:

```bash
curl "http://localhost:8080/api/opensearch/search?q=fatura%20itiraz&limit=5&mode=EXTRACTED_DOCUMENT"
```

---

### Apache Solr

Health:

```bash
curl "http://localhost:8080/api/solr/health"
```

RAW_XML reindex:

```bash
curl -X POST "http://localhost:8080/api/solr/reindex?mode=RAW_XML"
```

RAW_XML search:

```bash
curl "http://localhost:8080/api/solr/search?q=fatura%20itiraz&limit=5&mode=RAW_XML"
```

EXTRACTED_DOCUMENT reindex:

```bash
curl -X POST "http://localhost:8080/api/solr/reindex?mode=EXTRACTED_DOCUMENT"
```

EXTRACTED_DOCUMENT search:

```bash
curl "http://localhost:8080/api/solr/search?q=fatura%20itiraz&limit=5&mode=EXTRACTED_DOCUMENT"
```

Solr tarafında `EXTRACTED_DOCUMENT` modunda liste alanları (`screenTitles`, `screenDescriptions`, `actionTexts`, `technicalTokens`) tek text alanına dönüştürülerek indexlenmektedir. Bunun nedeni Solr dynamic field yapısında multi-valued alan davranışını PoC aşamasında sade ve stabil tutmaktır.

---

## Benchmark Endpointleri

### Kullanılabilir Engine Listesi

```bash
curl "http://localhost:8080/api/benchmark/engines"
```

Örnek cevap:

```json
[
  "elasticsearch",
  "opensearch",
  "solr"
]
```

---

### Tüm Engine’leri Seçilen Moda Göre Reindex Etme

RAW_XML:

```bash
curl -X POST "http://localhost:8080/api/benchmark/reindex-all?mode=RAW_XML"
```

EXTRACTED_DOCUMENT:

```bash
curl -X POST "http://localhost:8080/api/benchmark/reindex-all?mode=EXTRACTED_DOCUMENT"
```

Örnek cevap:

```json
{
  "executedAt": "2026-06-04T14:25:52.195767",
  "results": [
    {
      "engine": "elasticsearch",
      "indexName": "workflow-documents",
      "indexedDocumentCount": 10,
      "message": "All workflow documents indexed into Elasticsearch successfully. mode=RAW_XML"
    },
    {
      "engine": "opensearch",
      "indexName": "workflow-documents",
      "indexedDocumentCount": 10,
      "message": "All workflow documents indexed into OpenSearch successfully. mode=RAW_XML"
    },
    {
      "engine": "solr",
      "indexName": "workflow-documents",
      "indexedDocumentCount": 10,
      "message": "All workflow documents indexed into Solr successfully. mode=RAW_XML"
    }
  ]
}
```

---

### Benchmark Çalıştırma

RAW_XML benchmark:

```bash
curl -X POST "http://localhost:8080/api/benchmark/run" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "RAW_XML",
    "engines": ["elasticsearch", "opensearch", "solr"],
    "queries": ["fatura itiraz", "müşteri bilgileri", "ödeme durumu"],
    "limit": 5,
    "warmupIterations": 5,
    "measurementIterations": 20
  }'
```

EXTRACTED_DOCUMENT benchmark:

```bash
curl -X POST "http://localhost:8080/api/benchmark/run" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "EXTRACTED_DOCUMENT",
    "engines": ["elasticsearch", "opensearch", "solr"],
    "queries": ["fatura itiraz", "müşteri bilgileri", "ödeme durumu"],
    "limit": 5,
    "warmupIterations": 5,
    "measurementIterations": 20
  }'
```

Benchmark çalıştırmadan önce aynı modda reindex yapılmalıdır:

```text
RAW_XML benchmark              → önce RAW_XML reindex
EXTRACTED_DOCUMENT benchmark   → önce EXTRACTED_DOCUMENT reindex
```

---

## Benchmark Raporu

İlk RAW_XML benchmark sonuçları README içinde uzun tablolar halinde tutulmak yerine ayrı bir raporda toplanmıştır: [`docs/raw_xml_benchmark_results.md`](docs/raw_xml_benchmark_results.md)

Bu raporda aşağıdaki başlıklar yer alır:

- kullanılan test ortamı,
- XML boyutu ve doküman sayısı matrisi,
- RAW_XML benchmark sonuçları,
- OpenSearch circuit breaker / heap gözlemi,
- Elasticsearch + Solr izolasyon testi,
- ilk teknik değerlendirme ve sonraki adımlar.

README içinde yalnızca kısa benchmark özeti verilmiştir. Ayrıntılı deney sonuçları için ilgili rapor dosyası incelenmelidir.

## Hızlı Smoke Test

Aşağıdaki komutlar mevcut yapının sağlıklı çalışıp çalışmadığını hızlıca kontrol etmek için kullanılabilir.

```bash
docker ps

curl "http://localhost:8080/api/workflows/count"

curl -X POST "http://localhost:8080/api/workflows/generate?count=10&screenCount=20"

curl "http://localhost:8080/api/workflows/search-documents?limit=1"

curl -X POST "http://localhost:8080/api/benchmark/reindex-all?mode=RAW_XML"

curl -X POST "http://localhost:8080/api/benchmark/run" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "RAW_XML",
    "engines": ["elasticsearch", "opensearch", "solr"],
    "queries": ["fatura itiraz", "müşteri bilgileri", "ödeme durumu"],
    "limit": 5,
    "warmupIterations": 5,
    "measurementIterations": 20
  }'
```

Beklenen durum:

- Üç engine de reindex işlemini başarıyla tamamlamalıdır.
- Benchmark response içinde `successCount` ölçüm sayısına eşit olmalıdır.
- `errorCount` sıfır olmalıdır.
- `hitCount` sıfır olmamalıdır.

---

## Önemli Teknik Notlar

### 1. Local test production benchmark değildir

Local ortamda alınan `tookMs` değerleri production performansını doğrudan temsil etmez. Bu değerler sadece entegrasyon testi, metodoloji doğrulama ve kabaca davranış gözlemi için kullanılmalıdır.

### 2. İlk sorgular yanıltıcı olabilir

İlk sorgularda JVM, cache ve engine internal warming etkileri nedeniyle süreler yüksek çıkabilir. Bu nedenle benchmark runner içinde warm-up iteration uygulanmaktadır.

### 3. Metadata-only response bilinçli tercihtir

Şu an search endpointleri full XML dönmemektedir. Sadece küçük metadata alanları döndürülmektedir. Bu bilinçli bir tercihtir. Çünkü 2 MB XML response’a eklenirse ölçülen süreye payload taşıma maliyeti karışır.

### 4. Full XML response ayrı ölçülmelidir

İleride full XML response senaryosu ayrıca test edilmelidir. Böylece search engine maliyeti ile payload taşıma maliyeti ayrıştırılabilir.

### 5. Indexleme ve search modu aynı olmalıdır

Benchmark yapmadan önce aynı modda reindex yapılmalıdır:

```text
RAW_XML benchmark              → önce RAW_XML reindex
EXTRACTED_DOCUMENT benchmark   → önce EXTRACTED_DOCUMENT reindex
```

Aksi halde ölçüm teknik olarak yanlış olur.

### 6. Elasticsearch ve OpenSearch query mantığı benzerdir

Elasticsearch ve OpenSearch tarafında:

- RAW_XML modunda `xmlContent` üzerinde `match` query kullanılır.
- EXTRACTED_DOCUMENT modunda `multi_match` query kullanılır.

EXTRACTED_DOCUMENT alan ağırlıkları:

```text
workflowName^3
screenTitles^2
screenDescriptions
actionTexts
searchText
```

### 7. Solr query mantığı farklıdır

Solr tarafında:

- RAW_XML modunda `qf=xmlContent_txt`
- EXTRACTED_DOCUMENT modunda `edismax` query parser kullanılır.

EXTRACTED_DOCUMENT alan ağırlıkları:

```text
workflowName_txt^3
screenTitles_txt^2
screenDescriptions_txt
actionTexts_txt
searchText_txt
technicalTokens_txt
```

---

## Planlanan Sonraki Adımlar

1. EXTRACTED_DOCUMENT modunu daha büyük veri setlerinde sınırlı şekilde test etmek
2. Search-only response vs full XML response ayrımını eklemek
3. Benchmark response modeline `lastErrorMessage` benzeri hata alanı eklemek
4. CSV/JSON benchmark raporu üretmek
5. Büyük veri setleri için Elasticsearch/OpenSearch `_bulk` ve Solr batch indexing optimizasyonunu değerlendirmek
6. Daha gerçekçi query seti oluşturmak
7. Relevance / arama kalitesi değerlendirmesi eklemek
8. Production-like cluster ortamında test yapmak

## Hedef

Bu projenin nihai hedefi, XML tabanlı büyük workflow dokümanları için Elasticsearch, OpenSearch ve Apache Solr servislerini kontrollü ve tekrar edilebilir şekilde benchmark edebilecek bir altyapı oluşturmaktır.

Bu sayede mevcut Elasticsearch kullanımının performansı ölçülebilecek, OpenSearch ve Apache Solr alternatiflerinin teknik uygunluğu değerlendirilebilecek ve XML’i parse edip normalize search document üretmenin etkisi ayrıca analiz edilebilecektir.