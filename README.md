# XML Workflow Search Benchmark

Bu proje, PostgreSQL üzerinde tutulan XML tabanlı workflow dokümanları için arama motoru benchmark altyapısı oluşturmak amacıyla geliştirilmiştir.

Projenin temel amacı; yaklaşık 2 MB boyutundaki XML workflow dokümanları üzerinde **Elasticsearch**, **OpenSearch** ve **Apache Solr** servislerinin free-text search performansını karşılaştırabilecek kontrollü bir PoC altyapısı oluşturmaktır.

> Bu repo şu anda benchmark sisteminin ilk temel katmanını içerir: PostgreSQL veri modeli, sentetik XML workflow üretimi, XML parser ve ortak `SearchDocument` modeli.

---

## Problem Bağlamı

İncelenen senaryoda bir müşteri temsilcisi platformu bulunmaktadır. Bu platformda müşteri temsilcisinin gördüğü ekranlar ve iş akışları XML dosyaları içinde tanımlanmaktadır.

Mevcut sistemde:

- XML veriler PostgreSQL üzerinde saklanmaktadır.
- Aynı XML veriler Elasticsearch üzerinde de bulunmaktadır.
- Free-text search işlemleri Elasticsearch üzerinden yapılmaktadır.
- Test kapsamında yaklaşık 2 MB boyutundaki XML dosyalarında arama performansının ölçülmesi hedeflenmektedir.

Bu proje, ilerleyen aşamalarda Elasticsearch mevcut sistem/baseline olacak şekilde OpenSearch ve Apache Solr alternatiflerini karşılaştırmayı hedefler.

---

## Benchmark Kapsamı

Planlanan karşılaştırma kapsamı:

| Servis | Rol |
|---|---|
| Elasticsearch | Mevcut sistem / baseline |
| OpenSearch | Elasticsearch’e en yakın alternatif |
| Apache Solr | Lucene tabanlı olgun kurumsal alternatif |

Bu çalışmada Meilisearch ve PostgreSQL FTS kapsam dışı bırakılmıştır. Çünkü bu problem özelinde PostgreSQL source database olarak konumlanmaktadır; search engine alternatifi olarak ise Elasticsearch’e daha yakın kurumsal çözümler değerlendirilmelidir.

---

## Teknik Yaklaşım

Benchmark yalnızca “hangi servis kaç ms cevap verdi?” sorusuna indirgenmemelidir. XML workflow verilerinde performansı etkileyen temel faktörler şunlardır:

- XML’in ham haliyle mi indexlendiği
- XML’den `workflowName`, `screenTitles`, `descriptions`, `actions`, `technicalTokens` ve `searchText` gibi alanların çıkarılıp çıkarılmadığı
- Search sonucunda full XML’in dönüp dönmediği
- Sadece metadata response ile full XML response arasındaki fark
- Query tipi
- Index mapping/schema yapısı
- Veri boyutu ve doküman sayısı

Bu yüzden ileride benchmark senaryoları şu şekilde ayrılacaktır:

| Senaryo | Açıklama |
|---|---|
| Raw XML Search | XML’in ham haliyle aranması |
| Extracted SearchText Search | XML’den çıkarılmış normalize alanlar üzerinden arama |
| Search-only Response | Sadece id, workflowName, score gibi küçük metadata dönmesi |
| Full XML Response | Search sonucunda XML içeriğinin de dönmesi |

---

## Mevcut Durum

Şu ana kadar tamamlananlar:

- Spring Boot projesi oluşturuldu.
- PostgreSQL Docker Compose ile ayağa kaldırıldı.
- `WorkflowDocument` entity modeli oluşturuldu.
- PostgreSQL üzerinde XML workflow kaydı tutan yapı kuruldu.
- Sentetik XML workflow üreten generator yazıldı.
- XML içeriğini parse eden temel parser katmanı oluşturuldu.
- Search engine’lere gönderilecek ortak `SearchDocument` modeli oluşturuldu.
- REST endpointleri ile veri üretimi ve parser çıktısı test edildi.

Henüz yapılmayanlar:

- Elasticsearch entegrasyonu
- OpenSearch entegrasyonu
- Apache Solr entegrasyonu
- Index oluşturma servisleri
- Search endpointleri
- Benchmark runner
- p50 / p95 / p99 ölçümleri
- CSV/JSON benchmark raporu

---

## Kullanılan Teknolojiler

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Docker Compose
- Maven
- Lombok

---

## Proje Yapısı

```text
src/main/java/com/recepoztrk/xmlworkflowsearchbenchmark
├── search
│   └── model
│       └── SearchDocument.java
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

## PostgreSQL’i Çalıştırma

Proje kök dizininde:

```bash
docker compose up -d
```

PostgreSQL bağlantı bilgileri:

```text
Host: localhost
Port: 5434
Database: xml_benchmark
Username: postgres
Password: postgres
```

Container durdurmak için:

```bash
docker compose down
```

Volume ile birlikte tamamen silmek için:

```bash
docker compose down -v
```

---

## Uygulamayı Çalıştırma

```bash
./mvnw spring-boot:run
```

Uygulama varsayılan olarak şu portta çalışır:

```text
http://localhost:8080
```

---

## REST Endpointleri

### Workflow Sayısını Görüntüleme

```bash
curl "http://localhost:8080/api/workflows/count"
```

Örnek cevap:

```json
{
  "totalCount": 0
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

`screenCount` arttıkça XML boyutu büyür. Bu değer ileride yaklaşık 2 MB XML dokümanları oluşturmak için kullanılacaktır.

---

### Workflow Özetlerini Listeleme

```bash
curl "http://localhost:8080/api/workflows"
```

Örnek cevap:

```json
[
  {
    "id": 21,
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
curl "http://localhost:8080/api/workflows/21/search-document"
```

Örnek cevap:

```json
{
  "id": "21",
  "databaseId": 21,
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

Bu endpoint, XML parser katmanının doğru çalışıp çalışmadığını kontrol etmek için kullanılır.

---

### İlk N Workflow İçin SearchDocument Preview

```bash
curl "http://localhost:8080/api/workflows/search-documents?limit=3"
```

Bu endpoint ilk N workflow kaydını parse ederek `SearchDocument` preview çıktısı döndürür.

---

## Veri Akışı

Şu anki temel veri akışı:

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
```

İleride hedeflenen veri akışı:

```text
PostgreSQL
    ↓
WorkflowDocument
    ↓
WorkflowXmlParser
    ↓
SearchDocument
    ↓
Elasticsearch / OpenSearch / Apache Solr
    ↓
Benchmark Runner
```

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

Bu modelin amacı, Elasticsearch, OpenSearch ve Solr tarafına aynı mantıksal veriyi gönderebilmektir.

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

## Önemli Notlar

- Local benchmark sonuçları production performansını doğrudan temsil etmez.
- Local benchmark yalnızca aynı makinede, aynı veriyle, aynı query setiyle göreli karşılaştırma yapmak için kullanılmalıdır.
- Elasticsearch/OpenSearch/Solr sonuçları ancak aynı veri modeli, aynı query mantığı ve aynı response modu ile adil şekilde karşılaştırılabilir.
- Full XML response ile metadata-only response ayrı ölçülmelidir.
- İlk query sonuçları cache/JVM etkisi nedeniyle yanıltıcı olabilir; benchmark aşamasında warm-up çalıştırmaları yapılmalıdır.

---

## Planlanan Sonraki Adımlar

1. Elasticsearch Docker servisini ekleme
2. Elasticsearch index oluşturma servisi yazma
3. `SearchDocument` verilerini Elasticsearch’e indexleme
4. Elasticsearch search endpointi oluşturma
5. OpenSearch entegrasyonu
6. Apache Solr entegrasyonu
7. Ortak benchmark runner geliştirme
8. Raw XML vs extracted searchText senaryolarını ölçme
9. Search-only vs full XML response senaryolarını ölçme
10. p50 / p95 / p99 değerlerini hesaplama
11. Sonuçları JSON/CSV olarak raporlama

---

## Hedef

Bu projenin nihai hedefi, XML tabanlı büyük workflow dokümanları için Elasticsearch, OpenSearch ve Apache Solr servislerini kontrollü ve tekrar edilebilir şekilde benchmark edebilecek bir altyapı oluşturmaktır.

Bu sayede mevcut Elasticsearch kullanımının performansı ölçülebilecek ve OpenSearch ile Apache Solr alternatiflerinin teknik uygunluğu değerlendirilebilecektir.