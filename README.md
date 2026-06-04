# XML Workflow Search Benchmark

Bu proje, PostgreSQL üzerinde tutulan XML tabanlı workflow dokümanları için arama motoru benchmark altyapısı oluşturmak amacıyla geliştirilmiş bir Spring Boot PoC projesidir.

Projenin temel amacı; yaklaşık **2 MB boyutundaki XML workflow dokümanları** üzerinde **Elasticsearch**, **OpenSearch** ve **Apache Solr** servislerinin free-text search performansını kontrollü ve tekrar edilebilir şekilde karşılaştırabilecek bir altyapı oluşturmaktır.

---

## Problem Bağlamı

İncelenen senaryoda bir **müşteri temsilcisi platformu** bulunmaktadır. Bu platformda müşteri temsilcisinin gördüğü ekranlar ve iş akışları XML dosyaları içinde tanımlanmaktadır.

Mevcut sistemde:

- XML veriler PostgreSQL üzerinde saklanmaktadır.
- Aynı XML veriler Elasticsearch üzerinde de bulunmaktadır.
- Free-text search işlemleri Elasticsearch üzerinden yapılmaktadır.
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

## Teknik Yaklaşım

Benchmark yalnızca “hangi servis kaç ms cevap verdi?” sorusuna indirgenmemelidir. XML workflow verilerinde performansı etkileyen temel faktörler şunlardır:

- XML’in ham haliyle mi indexlendiği
- XML’den `workflowName`, `screenTitles`, `descriptions`, `actions`, `technicalTokens` ve `searchText` gibi alanların çıkarılıp çıkarılmadığı
- Search sonucunda full XML’in dönüp dönmediği
- Metadata-only response ile full XML response arasındaki fark
- Query tipi
- Index mapping/schema yapısı
- Veri boyutu ve doküman sayısı
- JVM/cache/warm-up etkisi

Bu nedenle ilerleyen aşamada benchmark senaryoları şu şekilde ayrılacaktır:

| Senaryo | Açıklama |
|---|---|
| Raw XML Search | XML’in ham haliyle aranması |
| Extracted SearchText Search | XML’den çıkarılmış normalize alanlar üzerinden arama |
| Search-only Response | Sadece id, workflowName, score gibi küçük metadata dönmesi |
| Full XML Response | Search sonucunda XML içeriğinin de dönmesi |

---

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
- Elasticsearch, OpenSearch ve Solr üzerinde aynı `SearchDocument` modeliyle arama yapılabilir hale getirildi.

Henüz yapılmayanlar:

- Ortak benchmark runner
- Warm-up / measurement iteration ayrımı
- p50 / p95 / p99 ölçümleri
- Raw XML vs extracted searchText benchmarkı
- Search-only vs full XML response benchmarkı
- CSV/JSON benchmark raporu

---

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
├── search
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
│       └── SearchHitDto.java
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

## Veri Akışı

Mevcut veri akışı:

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

Bu yapı sayesinde üç farklı search engine aynı mantıksal veri modeliyle test edilebilir.

---

## SearchDocument Modeli

`SearchDocument`, search engine’lere gönderilecek ortak normalize veri modelidir.

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

Amaç, PostgreSQL’de tutulan ham XML verisini arama motorları için daha uygun ve karşılaştırılabilir bir dokümana dönüştürmektir.

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

`screenCount` arttıkça XML boyutu büyür. Testlerde `screenCount=20` iken XML boyutu yaklaşık 34 KB oluşmuştur. Yaklaşık 2 MB XML simülasyonu için bu değer ileride artırılacaktır.

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

## Elasticsearch Endpointleri

### Elasticsearch Health

```bash
curl "http://localhost:8080/api/elasticsearch/health"
```

### Elasticsearch Reindex

PostgreSQL’deki workflow kayıtlarını Elasticsearch’e yeniden indexler.

```bash
curl -X POST "http://localhost:8080/api/elasticsearch/reindex"
```

Örnek cevap:

```json
{
  "engine": "elasticsearch",
  "indexName": "workflow-documents",
  "indexedDocumentCount": 10,
  "message": "All workflow documents indexed into Elasticsearch successfully."
}
```

### Elasticsearch Search

```bash
curl "http://localhost:8080/api/elasticsearch/search?q=fatura%20itiraz&limit=5"
```

Örnek cevap:

```json
{
  "engine": "elasticsearch",
  "query": "fatura itiraz",
  "tookMs": 9,
  "hitCount": 10,
  "hits": [
    {
      "id": "51",
      "score": 9.681772,
      "workflowCode": "WF_BILLING_1",
      "workflowName": "Fatura İtiraz Süreci 1",
      "status": "ACTIVE",
      "domain": "Billing",
      "xmlSizeKb": 34
    }
  ]
}
```

---

## OpenSearch Endpointleri

### OpenSearch Health

```bash
curl "http://localhost:8080/api/opensearch/health"
```

### OpenSearch Reindex

```bash
curl -X POST "http://localhost:8080/api/opensearch/reindex"
```

Örnek cevap:

```json
{
  "engine": "opensearch",
  "indexName": "workflow-documents",
  "indexedDocumentCount": 10,
  "message": "All workflow documents indexed into OpenSearch successfully."
}
```

### OpenSearch Search

```bash
curl "http://localhost:8080/api/opensearch/search?q=fatura%20itiraz&limit=5"
```

Örnek cevap:

```json
{
  "engine": "opensearch",
  "query": "fatura itiraz",
  "tookMs": 8,
  "hitCount": 10,
  "hits": [
    {
      "id": "51",
      "score": 9.681772,
      "workflowCode": "WF_BILLING_1",
      "workflowName": "Fatura İtiraz Süreci 1",
      "status": "ACTIVE",
      "domain": "Billing",
      "xmlSizeKb": 34
    }
  ]
}
```

---

## Apache Solr Endpointleri

### Solr Health

```bash
curl "http://localhost:8080/api/solr/health"
```

### Solr Reindex

```bash
curl -X POST "http://localhost:8080/api/solr/reindex"
```

Örnek cevap:

```json
{
  "engine": "solr",
  "indexName": "workflow-documents",
  "indexedDocumentCount": 10,
  "message": "All workflow documents indexed into Solr successfully."
}
```

### Solr Search

```bash
curl "http://localhost:8080/api/solr/search?q=fatura%20itiraz&limit=5"
```

Örnek cevap yapısı:

```json
{
  "engine": "solr",
  "query": "fatura itiraz",
  "tookMs": 12,
  "hitCount": 10,
  "hits": [
    {
      "id": "51",
      "score": 3.14,
      "workflowCode": "WF_BILLING_1",
      "workflowName": "Fatura İtiraz Süreci 1",
      "status": "ACTIVE",
      "domain": "Billing",
      "xmlSizeKb": 34
    }
  ]
}
```

Solr tarafında liste alanları (`screenTitles`, `screenDescriptions`, `actionTexts`, `technicalTokens`) tek text alanına dönüştürülerek indexlenmektedir. Bunun nedeni Solr dynamic field yapısında multi-valued alan davranışını PoC aşamasında sade ve stabil tutmaktır.

---

## Hızlı Smoke Test

Aşağıdaki komutlar mevcut yapının sağlıklı çalışıp çalışmadığını hızlıca kontrol etmek için kullanılabilir.

```bash
docker ps

curl "http://localhost:8080/api/workflows/count"

curl -X POST "http://localhost:8080/api/workflows/generate?count=10&screenCount=20"

curl "http://localhost:8080/api/workflows/search-documents?limit=1"

curl -X POST "http://localhost:8080/api/elasticsearch/reindex"
curl "http://localhost:8080/api/elasticsearch/search?q=fatura%20itiraz&limit=3"

curl -X POST "http://localhost:8080/api/opensearch/reindex"
curl "http://localhost:8080/api/opensearch/search?q=fatura%20itiraz&limit=3"

curl -X POST "http://localhost:8080/api/solr/reindex"
curl "http://localhost:8080/api/solr/search?q=fatura%20itiraz&limit=3"
```

Beklenen durum:

- Üç engine de reindex işlemini başarıyla tamamlamalıdır.
- `fatura itiraz` sorgusunda `WF_BILLING_*` workflow kayıtları üst sıralarda gelmelidir.
- `hitCount` sıfır olmamalıdır.

---

## Önemli Teknik Notlar

### 1. Local test benchmark sonucu değildir

Local ortamda alınan `tookMs` değerleri production performansını doğrudan temsil etmez. Bu değerler sadece entegrasyon testi ve kabaca davranış gözlemi için kullanılmalıdır.

### 2. İlk sorgular yanıltıcı olabilir

İlk sorgularda JVM, cache ve engine internal warming etkileri nedeniyle süreler yüksek çıkabilir. Benchmark aşamasında warm-up çalıştırmaları yapılmalıdır.

### 3. Metadata-only response önemlidir

Şu an search endpointleri full XML dönmemektedir. Sadece küçük metadata alanları döndürülmektedir. Bu bilinçli bir tercihtir. Çünkü 2 MB XML response’a eklenirse ölçülen süreye payload taşıma maliyeti karışır.

### 4. Full XML response ayrı ölçülmelidir

İleride benchmark aşamasında full XML response senaryosu ayrıca test edilmelidir. Böylece search engine maliyeti ile payload taşıma maliyeti ayrıştırılabilir.

### 5. Elasticsearch ve OpenSearch query mantığı benzerdir

Elasticsearch ve OpenSearch tarafında `multi_match` query yapısı kullanılmıştır. Alan ağırlıkları şu mantıkla verilmiştir:

```text
workflowName^3
screenTitles^2
screenDescriptions
actionTexts
searchText
```

### 6. Solr query mantığı farklıdır

Solr tarafında Elasticsearch/OpenSearch `multi_match` yapısına kabaca karşılık gelecek şekilde `edismax` query parser kullanılmıştır. Alan ağırlıkları `qf` parametresi üzerinden verilmiştir.

---

## Planlanan Sonraki Adımlar

1. Ortak `SearchEngineClient` / interface tasarımı
2. Ortak benchmark runner geliştirme
3. Query set tanımı
4. Warm-up iteration desteği
5. Measurement iteration desteği
6. Ortalama, min, max, p50, p95, p99 hesaplama
7. Elasticsearch vs OpenSearch vs Solr karşılaştırma endpointi
8. Raw XML search senaryosu
9. Extracted SearchText search senaryosu
10. Search-only response vs full XML response senaryosu
11. JSON/CSV benchmark raporu üretimi
12. Daha büyük XML datasetleriyle test

---

## Hedef

Bu projenin nihai hedefi, XML tabanlı büyük workflow dokümanları için Elasticsearch, OpenSearch ve Apache Solr servislerini kontrollü ve tekrar edilebilir şekilde benchmark edebilecek bir altyapı oluşturmaktır.

Bu sayede mevcut Elasticsearch kullanımının performansı ölçülebilecek ve OpenSearch ile Apache Solr alternatiflerinin teknik uygunluğu değerlendirilebilecektir.