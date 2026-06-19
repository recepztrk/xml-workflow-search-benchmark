# Migration Compatibility Evaluator

Bu modül, Elasticsearch yerine geçmesi değerlendirilen aday search engine'lerin Elasticsearch baseline'ına ne kadar yakın sonuç ürettiğini ölçmek için eklenmiştir.

## Amaç

Mevcut sistemde XML workflow free-text search işlemi Elasticsearch ile yapılmaktadır. Elasticsearch yerine OpenSearch veya Apache Solr gibi açık kaynak alternatiflere geçilmesi değerlendirildiğinde yalnızca latency ölçmek yeterli değildir.

Bu modül şu soruya cevap verir:

> Aday search engine Elasticsearch yerine geçerse, kullanıcı aynı query için benzer workflow sonuçlarını görmeye devam eder mi?

## Endpoint

```http
POST /api/migration/evaluate
```

## Varsayılan davranış

Request body gönderilmezse aşağıdaki varsayılanlar kullanılır:

- `baselineEngine`: `elasticsearch`
- `candidateEngines`: Elasticsearch dışındaki tüm kayıtlı engine'ler
- `mode`: `RAW_XML`
- `responseMode`: `METADATA_ONLY`
- `limit`: `5`
- `queries`: benchmark varsayılan query seti

> Not: Migration evaluation yalnızca `METADATA_ONLY` response mode ile çalışır. Gerçek migration kararında full XML response ölçümü kullanılmaz.

## Örnek request

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

## Üretilen metrikler

| Metrik | Açıklama |
|---|---|
| `averageTopKOverlap` | Elasticsearch top-k sonuçları ile aday engine top-k sonuçlarının kesişim oranı |
| `top1MatchRate` | Elasticsearch ilk sonucu ile aday engine ilk sonucunun eşleşme oranı |
| `averageRankShift` | Ortak workflow sonuçlarının sıralamada ortalama kaç pozisyon kaydığı |
| `averageMissingBaselineRate` | Elasticsearch sonucunda olup aday engine sonucunda olmayan workflow oranı |
| `latencyRatioToBaseline` | Aday engine latency değerinin Elasticsearch latency değerine oranı |
| `migrationCompatibilityScore` | Sonuç benzerliği, rank stabilitesi, eksik sonuç oranı, latency ve hata oranını birlikte değerlendiren 0-100 arası skor |
| `decision` | Skora göre üretilen karar etiketi |

## Decision etiketleri

| Etiket | Anlamı |
|---|---|
| `HIGH_COMPATIBILITY` | Aday engine Elasticsearch davranışına oldukça yakın |
| `COMPATIBLE_WITH_REVIEW` | Genel olarak uyumlu, fakat detaylı inceleme önerilir |
| `FAST_BUT_RELEVANCE_REVIEW` | Aday engine hızlıdır, ancak sonuç benzerliği ayrıca incelenmelidir |
| `REVIEW_REQUIRED` | Migration öncesi detaylı relevance/query incelemesi gerekir |
| `LOW_COMPATIBILITY` | Elasticsearch davranışından ciddi sapma vardır |
| `LOW_COMPATIBILITY_HIGH_ERROR_RATE` | Hata oranı yüksektir; migration için risklidir |

## Neden benchmarktan farklı?

Benchmark modülü şu soruya cevap verir:

> Hangi engine daha hızlı?

Migration Compatibility Evaluator ise şu soruya cevap verir:

> Hangi engine Elasticsearch yerine geçtiğinde mevcut search davranışını daha az bozar?

Bu nedenle modül migration kararına doğrudan katkı sağlar.
