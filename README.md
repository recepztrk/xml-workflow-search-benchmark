# XML Workflow Search Benchmark

Bu proje, PostgreSQL üzerinde saklanan XML tabanlı workflow dokümanlarında full-text search performansını karşılaştırmak için geliştirilmiş bir Spring Boot PoC projesidir.

Amaç; müşteri temsilcisi platformlarında kullanılan büyük XML workflow dokümanları üzerinde **Elasticsearch**, **OpenSearch** ve **Apache Solr** servislerinin arama performansını kontrollü, tekrar edilebilir ve raporlanabilir şekilde ölçmektir.

## Problem Bağlamı

İncelenen sistemde müşteri temsilcisinin kullandığı ekranlar ve iş akışları XML dokümanları içinde tanımlanmaktadır. Bu XML dokümanları PostgreSQL üzerinde saklanmakta, free-text search işlemleri ise mevcut sistemde Elasticsearch üzerinden yapılmaktadır.

Bu proje kapsamında aşağıdaki sorulara cevap aranmıştır:

- Büyük XML workflow dokümanlarında Elasticsearch baseline olarak nasıl davranır?
- OpenSearch ve Apache Solr bu problem için teknik alternatif olabilir mi?
- XML’i parse etmeden ham XML üzerinde arama yapmak yeterince kararlı mı?
- XML parse edilerek oluşturulan normalize search document performans avantajı sağlar mı?
- Search response içinde büyük XML içeriği döndürmek latency üzerinde ne kadar etkilidir?

## Kapsam

Karşılaştırılan search engine’ler:

| Search Engine | Rol |
|---|---|
| Elasticsearch | Mevcut sistem / baseline |
| OpenSearch | Elasticsearch’e yakın alternatif |
| Apache Solr | Lucene tabanlı kurumsal alternatif |

Benchmark iki farklı indexleme stratejisini destekler:

| Mod | Açıklama |
|---|---|
| `RAW_XML` | XML parse edilmeden `xmlContent` alanı search engine’e aktarılır. Mevcut sisteme en yakın senaryodur. |
| `EXTRACTED_DOCUMENT` | XML parse edilir; `workflowName`, `screenTitles`, `screenDescriptions`, `actionTexts`, `technicalTokens`, `searchText` gibi alanlar çıkarılır ve arama bu alanlar üzerinden yapılır. |

Ayrıca iki farklı response modu desteklenir:

| Response Mode | Açıklama |
|---|---|
| `METADATA_ONLY` | Search sonucunda yalnızca küçük metadata alanları döner. Search engine latency’sini ölçmek için kullanılır. |
| `FULL_XML_RESPONSE` | Metadata alanlarına ek olarak XML içeriği de response’a dahil edilir. Büyük payload taşıma maliyetini ölçmek için kullanılır. |

## Temel Sonuç

Final benchmark sonuçlarına göre:

- **Apache Solr**, metadata-only arama senaryolarında en düşük latency değerlerini üretmiştir.
- **Elasticsearch**, Solr’a göre daha yüksek latency üretmiş ancak büyük testlerde kararlı baseline olarak çalışmıştır.
- **OpenSearch**, küçük ve orta ölçekli testlerde çalışmış; ancak büyük XML yüklerinde lokal kaynak koşullarında ana final karşılaştırmaya dahil edilmemiştir.
- **RAW_XML**, mevcut sistem yaklaşımına yakın, sade ve kararlı bir baseline olarak öne çıkmıştır.
- **EXTRACTED_DOCUMENT**, mevcut parse yapısı ve query setiyle genel performans avantajı sağlamamıştır.
- **FULL_XML_RESPONSE**, latency’yi ciddi şekilde artırmıştır. Bu nedenle search endpointinin metadata-only çalışması, XML detayının ayrı endpoint üzerinden alınması önerilir.

## Final Büyük Test Özeti

Final büyük testler, 100 adet yaklaşık 2 MB XML workflow dokümanı üzerinde, Elasticsearch ve Solr ile çalıştırılmıştır.

| Test | Engine | Ortalama Avg | Ortalama P95 | Açıklama |
|---|---:|---:|---:|---|
| `RAW_XML + METADATA_ONLY` | Elasticsearch | 12.66 ms | 19.92 ms | Kararlı baseline |
| `RAW_XML + METADATA_ONLY` | Solr | 1.12 ms | 1.79 ms | En düşük metadata-only latency |
| `EXTRACTED_DOCUMENT + METADATA_ONLY` | Elasticsearch | 27.83 ms | 49.77 ms | RAW_XML’den belirgin yavaş |
| `EXTRACTED_DOCUMENT + METADATA_ONLY` | Solr | 1.20 ms | 2.11 ms | RAW_XML’e yakın fakat daha iyi değil |
| `RAW_XML + FULL_XML_RESPONSE` | Elasticsearch | 76.08 ms | 88.07 ms | Büyük response payload maliyeti belirgin |
| `RAW_XML + FULL_XML_RESPONSE` | Solr | 66.32 ms | 75.97 ms | Arama değil payload taşıma baskın hale geliyor |

Detaylı sonuç dosyaları `benchmark-results/` dizininde tutulmaktadır.

## Benchmark Artifact Dosyaları

Final benchmark çıktıları JSON formatında saklanmıştır:

```text
benchmark-results/
├── small_raw_xml_metadata_only.json
├── small_extracted_document_metadata_only.json
├── medium_raw_xml_metadata_only.json
├── medium_extracted_document_metadata_only.json
├── large_raw_xml_metadata_only.json
├── large_extracted_document_metadata_only.json
└── large_raw_xml_full_xml_response.json
```

CSV çıktıları ara analiz için üretilebilmekle birlikte repoda temel sonuç dosyası olarak JSON çıktıları tutulmuştur.

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

## Servis Portları

| Servis | Port |
|---|---:|
| Spring Boot API | `8080` |
| PostgreSQL | `5434` |
| OpenSearch | `9200` |
| Elasticsearch | `9201` |
| Apache Solr | `8983` |

## Proje Yapısı

```text
src/main/java/com/recepoztrk/xmlworkflowsearchbenchmark
├── benchmark
│   ├── controller
│   │   └── BenchmarkController.java
│   ├── model
│   │   ├── BenchmarkExportResponse.java
│   │   ├── BenchmarkMeasurementResult.java
│   │   ├── BenchmarkReindexResponse.java
│   │   ├── BenchmarkRunRequest.java
│   │   └── BenchmarkRunResponse.java
│   └── service
│       ├── BenchmarkExportService.java
│       └── BenchmarkService.java
│
├── search
│   ├── client
│   │   └── SearchEngineClient.java
│   ├── elasticsearch
│   │   ├── ElasticsearchController.java
│   │   └── ElasticsearchService.java
│   ├── opensearch
│   │   ├── OpenSearchController.java
│   │   └── OpenSearchService.java
│   ├── solr
│   │   ├── SolrController.java
│   │   └── SolrService.java
│   └── model
│       ├── IndexOperationResult.java
│       ├── ResponseMode.java
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

## Genel Veri Akışı

### RAW_XML

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

### EXTRACTED_DOCUMENT

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

## SearchDocument Modeli

`EXTRACTED_DOCUMENT` modunda ham XML parse edilerek aşağıdaki normalize alanlara ayrılır:

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

Bu modun amacı XML’i daha anlamlı alanlara ayırarak arama davranışını kontrol edilebilir hale getirmektir. Ancak mevcut testlerde bu yaklaşım genel performans avantajı üretmemiştir. Bunun temel nedeni, ayrıştırılmış alanların arama uzayını yeterince daraltmaması ve çok alanlı aramanın ek skor hesaplama maliyeti oluşturmasıdır.

## Docker Servislerini Çalıştırma

```bash
docker compose up -d
```

Container durumunu görmek için:

```bash
docker compose ps
```

Servisleri durdurmak için:

```bash
docker compose down
```

Volume’larla birlikte temizlemek için:

```bash
docker compose down -v
```

## Uygulamayı Çalıştırma

Docker servisleri çalıştıktan sonra:

```bash
./mvnw spring-boot:run
```

Compile kontrolü:

```bash
./mvnw clean compile
```

Uygulama varsayılan olarak şu adreste çalışır:

```text
http://localhost:8080
```

## PostgreSQL Bilgileri

```text
Host: localhost
Port: 5434
Database: xml_benchmark
Username: postgres
Password: postgres
```

Bağlantı kontrolü:

```bash
docker exec -it xml-benchmark-postgres psql -U postgres -d xml_benchmark -c "SELECT current_user, current_database();"
```

## Workflow Endpointleri

### Workflow Sayısı

```bash
curl "http://localhost:8080/api/workflows/count"
```

### Sentetik Dataset Üretme

```bash
curl -X POST "http://localhost:8080/api/workflows/generate?count=100&screenCount=1200"
```

Parametreler:

| Parametre | Açıklama |
|---|---|
| `count` | Üretilecek workflow sayısı |
| `screenCount` | Her workflow içindeki ekran sayısı |

Yaklaşık XML boyutları:

| screenCount | Yaklaşık XML Boyutu |
|---:|---:|
| 20 | 34 KB |
| 300 | 515 KB |
| 600 | 1032 KB |
| 1200 | 2068 KB |

### Workflow Özetleri

```bash
curl "http://localhost:8080/api/workflows"
```

Bu endpoint XML içeriğini döndürmez. Büyük XML payload’larının response’u şişirmemesi için yalnızca özet metadata döner.

### SearchDocument Preview

```bash
curl "http://localhost:8080/api/workflows/{id}/search-document"
```

İlk N kayıt için:

```bash
curl "http://localhost:8080/api/workflows/search-documents?limit=3"
```

## Tekil Search Engine Endpointleri

Tekil engine endpointleri manuel test içindir. Karşılaştırmalı benchmark için `/api/benchmark` endpointleri kullanılmalıdır.

### Elasticsearch

```bash
curl "http://localhost:8080/api/elasticsearch/health"

curl -X POST "http://localhost:8080/api/elasticsearch/reindex?mode=RAW_XML"

curl "http://localhost:8080/api/elasticsearch/search?q=fatura%20itiraz&limit=5&mode=RAW_XML"
```

### OpenSearch

```bash
curl "http://localhost:8080/api/opensearch/health"

curl -X POST "http://localhost:8080/api/opensearch/reindex?mode=RAW_XML"

curl "http://localhost:8080/api/opensearch/search?q=fatura%20itiraz&limit=5&mode=RAW_XML"
```

### Apache Solr

```bash
curl "http://localhost:8080/api/solr/health"

curl -X POST "http://localhost:8080/api/solr/reindex?mode=RAW_XML"

curl "http://localhost:8080/api/solr/search?q=fatura%20itiraz&limit=5&mode=RAW_XML"
```

## Benchmark Endpointleri

### Kullanılabilir Engine Listesi

```bash
curl "http://localhost:8080/api/benchmark/engines"
```

Beklenen çıktı:

```json
[
  "elasticsearch",
  "opensearch",
  "solr"
]
```

### Reindex

RAW_XML:

```bash
curl -X POST "http://localhost:8080/api/benchmark/reindex-all?mode=RAW_XML"
```

EXTRACTED_DOCUMENT:

```bash
curl -X POST "http://localhost:8080/api/benchmark/reindex-all?mode=EXTRACTED_DOCUMENT"
```

Benchmark çalıştırmadan önce aynı modda reindex yapılmalıdır:

```text
RAW_XML benchmark              → önce RAW_XML reindex
EXTRACTED_DOCUMENT benchmark   → önce EXTRACTED_DOCUMENT reindex
```

### Benchmark Çalıştırma

```bash
curl -X POST "http://localhost:8080/api/benchmark/run" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "RAW_XML",
    "responseMode": "METADATA_ONLY",
    "engines": ["elasticsearch", "opensearch", "solr"],
    "queries": ["fatura itiraz", "müşteri bilgileri", "ödeme durumu"],
    "limit": 5,
    "warmupIterations": 5,
    "measurementIterations": 20
  }'
```

### Benchmark Çalıştırma ve JSON/CSV Export

```bash
curl -X POST "http://localhost:8080/api/benchmark/run-and-export" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "RAW_XML",
    "responseMode": "METADATA_ONLY",
    "engines": ["elasticsearch", "solr"],
    "queries": ["fatura itiraz", "müşteri bilgileri", "ödeme durumu", "abonelik iptal", "arıza kaydı"],
    "limit": 5,
    "warmupIterations": 10,
    "measurementIterations": 50
  }'
```

Bu endpoint benchmark sonucunu `benchmark-results/` dizinine JSON ve CSV olarak yazar.

## Önemli Teknik Notlar

### Local test production benchmark değildir

Tüm testler lokal single-node Docker ortamında yapılmıştır. Sonuçlar production performansını doğrudan temsil etmez; PoC seviyesinde davranış gözlemi ve yöntem karşılaştırması olarak değerlendirilmelidir.

### Warm-up gereklidir

İlk sorgular JVM, JIT, cache ve search engine iç ısınma etkileri nedeniyle yanıltıcı olabilir. Bu nedenle benchmark runner warm-up iteration destekler.

### Metadata-only response bilinçli tercihtir

Search latency ölçülürken XML içeriği response’a dahil edilmemelidir. Aksi halde search engine maliyeti ile büyük payload taşıma maliyeti karışır.

### Full XML response ayrı ölçülmelidir

`FULL_XML_RESPONSE` testleri, 5 adet yaklaşık 2 MB XML sonucunun response içinde taşınmasının latency’yi ciddi artırdığını göstermiştir. Bu nedenle önerilen mimari:

```text
Search endpoint  → metadata-only sonuç döndürür
Detail endpoint  → seçilen workflow için XML içeriğini ayrıca döndürür
```

### RAW_XML beklenenden güçlü bir baseline’dır

RAW_XML, her sorguda XML string’i baştan sona lineer taramaz. Search engine indexleme sırasında XML içeriğini token’lara ayırır ve inverted index oluşturur. Bu nedenle tek büyük text field üzerinde arama yapmak, mevcut testlerde kararlı ve hızlı bir baseline üretmiştir.

### EXTRACTED_DOCUMENT otomatik performans avantajı üretmez

Parse edilmiş doküman yaklaşımı daha anlamlı alanlar sunsa da mevcut query setinde arama uzayını yeterince daraltmamıştır. Çok alanlı arama, field boost ve skor birleştirme maliyeti oluşturduğu için bazı testlerde RAW_XML’den daha yavaş çalışmıştır.

### OpenSearch büyük final testlerde dışarıda bırakılmıştır

OpenSearch küçük ve orta testlerde değerlendirilmiştir. Ancak büyük XML yüklerinde lokal kaynak koşulları ve bellek baskısı nedeniyle final büyük karşılaştırma Elasticsearch + Solr üzerinden yapılmıştır.

## Hızlı Smoke Test

```bash
docker compose up -d

./mvnw clean compile

./mvnw spring-boot:run

curl -X POST "http://localhost:8080/api/workflows/generate?count=10&screenCount=20"

curl -X POST "http://localhost:8080/api/benchmark/reindex-all?mode=RAW_XML"

curl -X POST "http://localhost:8080/api/benchmark/run-and-export" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "RAW_XML",
    "responseMode": "METADATA_ONLY",
    "engines": ["elasticsearch", "opensearch", "solr"],
    "queries": ["fatura itiraz", "müşteri bilgileri", "ödeme durumu"],
    "limit": 5,
    "warmupIterations": 3,
    "measurementIterations": 10
  }'
```

Beklenen durum:

- Reindex işlemi başarılı tamamlanmalıdır.
- `successCount`, measurement sayısına eşit olmalıdır.
- `errorCount` sıfır olmalıdır.
- `lastHitCount` sıfırdan büyük olmalıdır.

## Hedef

Bu projenin hedefi, XML tabanlı büyük workflow dokümanları için search engine karşılaştırması yapılabilecek tekrar edilebilir bir benchmark altyapısı oluşturmaktır. Elde edilen sonuçlar, mevcut Elasticsearch yaklaşımının davranışını ölçmüş; Apache Solr’ın metadata-only arama senaryosunda güçlü bir alternatif olduğunu göstermiş; XML payload taşıma ve parse edilmiş doküman yaklaşımı için ek teknik değerlendirme sağlamıştır.
