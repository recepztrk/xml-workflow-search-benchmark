# XML Workflow Search Benchmark

Bu proje, büyük XML workflow dokümanları üzerinde farklı search engine'lerin performansını ve migration uygunluğunu karşılaştırmak için geliştirilmiş bir Spring Boot PoC çalışmasıdır.

Proje kapsamında PostgreSQL üzerinde saklanan XML workflow verileri Elasticsearch, OpenSearch ve Apache Solr üzerinde indexlenir. Daha sonra aynı query seti ile arama performansı, response maliyeti ve Elasticsearch baseline'a göre migration uyumluluğu ölçülür.

---

## 1. Projenin Amacı

Mevcut sistemde müşteri temsilciliği platformundaki ekran akışları ve workflow tanımları büyük XML dokümanları olarak tutulmaktadır. Bu XML'ler PostgreSQL üzerinde saklanmakta, free-text search işlemi ise Elasticsearch üzerinden yapılmaktadır.

Bu PoC çalışmasının amacı:

- Büyük XML workflow dokümanlarında full-text search performansını ölçmek
- Elasticsearch mevcut yapısını baseline kabul etmek
- OpenSearch ve Apache Solr alternatiflerini değerlendirmek
- Ham XML arama ve parse edilmiş doküman arama stratejilerini karşılaştırmak
- Search response içinde full XML dönmenin latency etkisini ölçmek
- Migration sırasında sonuçların Elasticsearch ile ne kadar uyumlu kaldığını görmek

Bu proje production sistemi değildir. Lokal single-node Docker ortamında çalışan, migration kararına teknik veri üretmek için hazırlanmış bir benchmark ve değerlendirme altyapısıdır.

---

## 2. Kapsam

### Değerlendirilen Search Engine'ler

| Engine | Rol |
|---|---|
| Elasticsearch | Mevcut sistem baseline'ı |
| OpenSearch | Elasticsearch'e yakın açık kaynak migration adayı |
| Apache Solr | Lucene tabanlı alternatif search engine |

### Search Mode'ları

| Mode | Açıklama |
|---|---|
| `RAW_XML` | XML dokümanı parse edilmeden ham metin olarak indexlenir. Mevcut sisteme en yakın senaryodur. |
| `EXTRACTED_DOCUMENT` | XML parse edilerek workflow adı, ekran başlıkları, açıklamalar, action text'leri ve teknik tokenlar ayrı alanlara çıkarılır. |

### Response Mode'ları

| Response Mode | Açıklama |
|---|---|
| `METADATA_ONLY` | Search sonucunda yalnızca workflow metadata bilgileri döner. Ana benchmark senaryosudur. |
| `FULL_XML_RESPONSE` | Search sonucunda metadata ile birlikte full XML içeriği de döner. Sadece payload maliyetini görmek için denenmiştir. |

Ana karar senaryosu:

```text
RAW_XML + METADATA_ONLY
```

---

## 3. Genel Mimari

Proje dört ana katmandan oluşur:

```text
PostgreSQL
   ↓
Spring Boot API
   ↓
Search Engine Adapter Layer
   ↓
Elasticsearch / OpenSearch / Solr
```

### Bileşenler

| Bileşen | Görev |
|---|---|
| PostgreSQL | Üretilen XML workflow dokümanlarının kaynak veri deposudur. |
| Spring Boot API | Dataset üretimi, reindex, search, benchmark ve migration evaluation endpointlerini sağlar. |
| Elasticsearch Client | XML workflow verisini Elasticsearch'e indexler ve arama yapar. |
| OpenSearch Client | XML workflow verisini OpenSearch'e indexler ve arama yapar. |
| Solr Client | XML workflow verisini Solr'a indexler ve arama yapar. |
| Benchmark Service | Belirlenen query setini engine'ler üzerinde tekrar tekrar çalıştırır ve latency metriklerini üretir. |
| Migration Evaluation Service | Elasticsearch baseline sonuçları ile aday engine sonuçlarını karşılaştırır. |

---

## 4. Çalışma Akışı

Projenin genel çalışma sırası şöyledir:

```text
1. Docker servisleri başlatılır.
2. PostgreSQL içinde sentetik XML workflow verisi üretilir.
3. XML verileri seçilen search mode'a göre search engine'lere indexlenir.
4. Aynı query seti Elasticsearch, OpenSearch ve Solr üzerinde çalıştırılır.
5. Benchmark servisi warmup ve measurement iterasyonlarını yürütür.
6. Avg, min, max, p50, p95, p99, successCount ve errorCount metrikleri hesaplanır.
7. Sonuçlar JSON olarak benchmark-results/ dizinine yazılır.
8. Migration evaluator, Elasticsearch baseline ile aday engine sonuçlarını karşılaştırır.
```

### Veri Akışı

```text
WorkflowGeneratorService
        ↓
WorkflowEntity
        ↓
PostgreSQL
        ↓
Reindex Endpoint
        ↓
SearchEngineClient
        ↓
Elasticsearch / OpenSearch / Solr
        ↓
BenchmarkService
        ↓
BenchmarkExportService
        ↓
benchmark-results/*.json
```

---

## 5. Proje Yapısı

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
├── migration
│   ├── controller
│   │   └── MigrationEvaluationController.java
│   ├── model
│   │   ├── CandidateMigrationResult.java
│   │   ├── MigrationEvaluationRequest.java
│   │   ├── MigrationEvaluationResponse.java
│   │   └── QueryParityResult.java
│   └── service
│       └── MigrationEvaluationService.java
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
│       ├── SearchEngineType.java
│       ├── SearchMode.java
│       ├── SearchResponseMode.java
│       └── SearchResultItem.java
│
├── workflow
│   ├── controller
│   │   └── WorkflowController.java
│   ├── entity
│   │   └── WorkflowEntity.java
│   ├── repository
│   │   └── WorkflowRepository.java
│   └── service
│       ├── WorkflowGeneratorService.java
│       └── WorkflowService.java
│
└── XmlWorkflowSearchBenchmarkApplication.java
```

---

## 6. Paketlerin Görevleri

### `workflow`

Bu paket PostgreSQL tarafındaki XML workflow verilerinden sorumludur.

- Sentetik XML workflow üretir.
- Workflow kayıtlarını PostgreSQL'e yazar.
- Mevcut workflow sayısını ve özetlerini döner.
- Büyük XML üretimi için `screenCount` parametresini kullanır.

Örnek veri üretimi:

```bash
curl -X POST "http://localhost:8080/api/workflows/generate?count=100&screenCount=1200"
```

### `search`

Bu paket search engine adapter katmanıdır.

Ortak amaç:

- PostgreSQL'den gelen workflow verisini ilgili search engine'e indexlemek
- Aynı search isteğini Elasticsearch, OpenSearch veya Solr üzerinde çalıştırmak
- Farklı engine sonuçlarını ortak `SearchResultItem` modeline dönüştürmek

Bu yapı sayesinde benchmark katmanı engine detaylarını bilmeden ortak arayüz üzerinden çalışır.

### `benchmark`

Bu paket performans ölçümünden sorumludur.

Ölçülen başlıca metrikler:

| Metrik | Açıklama |
|---|---|
| `avgMs` | Başarılı ölçümlerin ortalama latency değeri |
| `minMs` | En düşük latency |
| `maxMs` | En yüksek latency |
| `p50Ms` | Median latency |
| `p95Ms` | 95. yüzdelik latency |
| `p99Ms` | 99. yüzdelik latency |
| `successCount` | Başarılı measurement sayısı |
| `errorCount` | Hatalı measurement sayısı |
| `lastHitCount` | Son başarılı sorgudaki toplam eşleşme sayısı |
| `lastResponseSizeKb` | Son başarılı response boyutu |

`run-and-export` endpointi sonuçları JSON olarak `benchmark-results/` dizinine yazar.

### `migration`

Bu paket Elasticsearch baseline'a göre migration uygunluğunu ölçer.

Amaç yalnızca latency ölçmek değildir. Aday engine'in Elasticsearch ile aynı sonuçları getirip getirmediği de değerlendirilir.

Ölçülen migration metrikleri:

| Metrik | Açıklama |
|---|---|
| `topKOverlap` | Elasticsearch top-K sonuçları ile aday engine top-K sonuçlarının kesişimi |
| `top1MatchRate` | İlk sıradaki sonucun Elasticsearch ile aynı olup olmadığı |
| `averageRankShift` | Ortak sonuçların sıralama kayması |
| `missingBaselineRate` | Elasticsearch sonucunda olup aday engine sonucunda eksik kalan sonuç oranı |
| `latencyRatioToBaseline` | Aday engine latency değerinin Elasticsearch'e oranı |
| `migrationCompatibilityScore` | Genel migration uyumluluk skoru |

---

## 7. Teknolojiler

| Teknoloji | Kullanım |
|---|---|
| Java 21+ | Backend uygulama dili |
| Spring Boot | REST API ve servis katmanı |
| Spring Data JPA | PostgreSQL erişimi |
| PostgreSQL | Kaynak XML workflow verisi |
| Elasticsearch | Baseline search engine |
| OpenSearch | Elasticsearch alternatifi |
| Apache Solr | Alternatif full-text search engine |
| Docker Compose | Lokal test ortamı |
| Maven | Build ve dependency yönetimi |
| Lombok | Model ve servis kodlarını sadeleştirme |

---

## 8. Servis Portları

| Servis | Port |
|---|---:|
| Spring Boot API | `8080` |
| PostgreSQL | `5434` |
| OpenSearch | `9200` |
| Elasticsearch | `9201` |
| Apache Solr | `8983` |

---

## 9. Kurulum ve Çalıştırma

### Docker servislerini başlatma

```bash
docker compose up -d
```

### Container durumunu kontrol etme

```bash
docker compose ps
```

### Spring Boot uygulamasını çalıştırma

```bash
./mvnw spring-boot:run
```

### Compile kontrolü

```bash
./mvnw clean compile
```

### Servisleri durdurma

```bash
docker compose down
```

### Volume temizliği ile durdurma

```bash
docker compose down -v
```

---

## 10. Dataset Üretimi

Büyük testlerde kullanılan ana veri seti:

```bash
curl -X POST "http://localhost:8080/api/workflows/generate?count=100&screenCount=1200"
```

Yaklaşık XML boyutları:

| screenCount | Yaklaşık XML Boyutu |
|---:|---:|
| 20 | 34 KB |
| 300 | 515 KB |
| 600 | 1032 KB |
| 1200 | 2068 KB |

Workflow sayısı:

```bash
curl "http://localhost:8080/api/workflows/count"
```

Workflow listesi:

```bash
curl "http://localhost:8080/api/workflows"
```

---

## 11. Reindex İşlemi

Search engine'ler PostgreSQL'deki veriyi doğrudan her aramada okumaz. Önce PostgreSQL'deki XML workflow verileri search engine indexlerine aktarılır.

Tüm engine'leri RAW_XML modunda reindex etmek için:

```bash
curl -X POST "http://localhost:8080/api/benchmark/reindex-all?mode=RAW_XML"
```

Tekil reindex:

```bash
curl -X POST "http://localhost:8080/api/elasticsearch/reindex?mode=RAW_XML"
curl -X POST "http://localhost:8080/api/opensearch/reindex?mode=RAW_XML"
curl -X POST "http://localhost:8080/api/solr/reindex?mode=RAW_XML"
```

EXTRACTED_DOCUMENT modunda reindex:

```bash
curl -X POST "http://localhost:8080/api/benchmark/reindex-all?mode=EXTRACTED_DOCUMENT"
```

---

## 12. Manuel Search

Elasticsearch:

```bash
curl "http://localhost:8080/api/elasticsearch/search?q=fatura%20itiraz&limit=5&mode=RAW_XML"
```

OpenSearch:

```bash
curl "http://localhost:8080/api/opensearch/search?q=fatura%20itiraz&limit=5&mode=RAW_XML"
```

Solr:

```bash
curl "http://localhost:8080/api/solr/search?q=fatura%20itiraz&limit=5&mode=RAW_XML"
```

---

## 13. Benchmark Çalıştırma

Ana benchmark senaryosu:

```bash
curl -X POST "http://localhost:8080/api/benchmark/run-and-export" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "RAW_XML",
    "responseMode": "METADATA_ONLY",
    "engines": ["elasticsearch", "opensearch", "solr"],
    "queries": [
      "fatura itiraz",
      "müşteri bilgileri",
      "ödeme durumu",
      "abonelik iptal",
      "arıza kaydı"
    ],
    "limit": 5,
    "warmupIterations": 10,
    "measurementIterations": 50
  }'
```

Bu endpoint:

1. Verilen query setini seçilen engine'lerde çalıştırır.
2. Önce warmup iterasyonlarını uygular.
3. Ardından measurement iterasyonlarını ölçer.
4. Latency metriklerini hesaplar.
5. Sonucu JSON olarak `benchmark-results/` dizinine yazar.

CSV export kapatılmıştır. Çıktı yalnızca JSON formatındadır.

---

## 14. Migration Compatibility Evaluation

Elasticsearch baseline alınarak OpenSearch ve Solr sonuç uyumluluğunu ölçmek için:

```bash
curl -X POST "http://localhost:8080/api/migration/evaluate" \
  -H "Content-Type: application/json" \
  -d '{
    "baselineEngine": "elasticsearch",
    "candidateEngines": ["opensearch", "solr"],
    "mode": "RAW_XML",
    "responseMode": "METADATA_ONLY",
    "queries": [
      "fatura itiraz",
      "müşteri bilgileri",
      "ödeme durumu",
      "abonelik iptal",
      "arıza kaydı"
    ],
    "limit": 5
  }'
```

Bu endpoint şunu ölçer:

```text
Elasticsearch sonucu ile aday engine sonucu aynı mı?
İlk sıradaki sonuç korunuyor mu?
Top-K sonuçlar örtüşüyor mu?
Sıralama ne kadar kayıyor?
Aday engine baseline'a göre hızlı mı yavaş mı?
```

---

## 15. Benchmark Sonuçlarının Saklanması

Final JSON çıktıları için önerilen isimlendirme:

```text
benchmark-results/
├── final-isolated-solr-raw-xml-metadata-only.json
├── final-isolated-elasticsearch-raw-xml-metadata-only.json
├── final-isolated-opensearch-raw-xml-metadata-only.json
├── large-mixed-engines-raw-xml-metadata-only-opensearch-memory-error.json
├── large-es-solr-raw-xml-metadata-only.json
├── large-es-solr-extracted-document-metadata-only.json
└── large-es-solr-raw-xml-full-xml-response.json
```

Bu yapı ile:

- Final karar için ilk üç izole test sonucu kullanılır.
- OpenSearch bellek davranışı için karma büyük test sonucu saklanır.
- EXTRACTED_DOCUMENT ve FULL_XML_RESPONSE testleri destekleyici sonuç olarak tutulur.
- Small ve medium ara test JSON'ları repoda tutulmak zorunda değildir.

---

## 16. Benchmark Özeti

Final izole testler, 100 adet yaklaşık 2 MB XML workflow dokümanı üzerinde çalıştırılmıştır.

| Engine | Avg Latency | P95 Avg | P99 Avg | Success | Error | Yorum |
|---|---:|---:|---:|---:|---:|---|
| Solr | 1.28 ms | 2.03 ms | 3.12 ms | 250 | 0 | En hızlı aday |
| Elasticsearch | 12.52 ms | 15.56 ms | 19.28 ms | 250 | 0 | Stabil baseline |
| OpenSearch | 22.05 ms | 28.56 ms | 30.67 ms | 250 | 0 | Stabil fakat daha yavaş |

Temel çıkarımlar:

- Solr, RAW_XML + METADATA_ONLY senaryosunda en düşük latency değerlerini üretmiştir.
- Elasticsearch stabil baseline olarak çalışmıştır.
- OpenSearch, yüksek heap ile stabil çalışmıştır fakat Elasticsearch'ten yavaş kalmıştır.
- EXTRACTED_DOCUMENT büyük testlerde beklenen avantajı sağlamamıştır.
- FULL_XML_RESPONSE latency değerlerini ciddi artırmıştır.
- Search endpointinin metadata-only çalışması daha doğru mimaridir.

Detaylı benchmark analizi için:

[Performance Benchmark Report](docs/performance-benchmark-report.md)

---

## 17. Migration Özeti

Migration compatibility testlerinde Elasticsearch baseline alınmıştır.

| Engine | Top-K Overlap | Top1 Match | Rank Shift | Compatibility Score | Yorum |
|---|---:|---:|---:|---:|---|
| OpenSearch | 1.0 | 1.0 | 0.0 | 92.49 | Uyumlu ama daha yavaş |
| Solr | 1.0 | 1.0 | 0.0 | 100.0 | Uyumlu ve daha hızlı |

Migration yorumu:

- Solr performans açısından en güçlü adaydır.
- OpenSearch, Elasticsearch'e daha yakın API yapısı nedeniyle geçiş eforu düşük olan adaydır.
- OpenSearch performans avantajı sağlamamıştır.
- Production kararı öncesinde gerçek query logları ve gerçek workflow verisiyle tekrar test gerekir.

Detaylı migration analizi için:

[Migration Evaluation Report](docs/migration-evaluation-report.md)

---

## 18. Önerilen Search Mimarisi

Benchmark sonuçlarına göre search endpointi full XML döndürmemelidir.

Önerilen yapı:

```text
Search Endpoint
  ↓
workflowCode
workflowName
domain
status
score
xmlSizeKb

Detail Endpoint
  ↓
selected workflow full XML content
```

Bu ayrımın nedeni:

- Search response küçük kalır.
- Büyük XML payload latency'yi bozmaz.
- Kullanıcı sadece seçtiği workflow'un detay XML içeriğini ister.
- Benchmark sonuçları search engine performansını daha doğru yansıtır.

---

## 19. Sınırlılıklar

Bu çalışma PoC seviyesindedir.

Sınırlılıklar:

- Testler lokal single-node Docker ortamında yapılmıştır.
- Veri seti sentetiktir.
- Query seti sınırlıdır.
- Production cluster, shard/replica ve gerçek trafik test edilmemiştir.
- Relevance ölçümü gerçek kullanıcı query loglarıyla yapılmamıştır.
- Cache etkisi tamamen izole edilmemiştir.
- OpenSearch kaynak davranışı lokal Docker ayarlarından etkilenmiştir.

Bu nedenle sonuçlar doğrudan production kararı olarak değil, teknik yön gösterici benchmark sonucu olarak değerlendirilmelidir.

---

## 20. Final Teknik Değerlendirme

Bu PoC kapsamında:

```text
Performans öncelikli açık kaynak alternatif: Solr
Elasticsearch'e en yakın migration yolu: OpenSearch
Mevcut sistem baseline'ı: Elasticsearch
Ana önerilen search senaryosu: RAW_XML + METADATA_ONLY
Önerilen response yaklaşımı: Metadata-only search + ayrı XML detail endpoint
```

Final öneri:

- Performans önceliği varsa Solr daha güçlü adaydır.
- Elasticsearch uyumluluğu ve düşük migration eforu öncelikliyse OpenSearch ikinci aday olarak değerlendirilebilir.
- Production kararı öncesinde gerçek veri, gerçek query logları ve production-like cluster ortamında testler tekrarlanmalıdır.
