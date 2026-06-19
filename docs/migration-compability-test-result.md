# Migration Compatibility Test Report

## Amaç

Elasticsearch mevcut sistem baseline'ı olarak alınarak OpenSearch ve Solr'ın migration uygunluğu değerlendirildi.

**Test Kapsamı**

- Mode: `RAW_XML`
- Response Mode: `METADATA_ONLY`
- Veri Seti: `100 workflow × 1200 screen`
- Engine'ler: Elasticsearch, OpenSearch, Solr

---

## Küçük Veri Seti Sonucu

10 workflow / 20 screen ile yapılan smoke testte tüm engine'ler başarılı çalıştı.

| Engine | Compatibility Score | Decision |
|---|---:|---|
| OpenSearch | 100.0 | HIGH_COMPATIBILITY |
| Solr | 100.0 | HIGH_COMPATIBILITY |

**Sonuç:** OpenSearch ve Solr, Elasticsearch ile aynı sonuçları aynı sırada döndürdü.

---

## Büyük Veri Seti Sonucu

### Reindex

| Engine | Durum |
|---|---|
| Elasticsearch | Başarılı |
| OpenSearch | Başarılı |
| Solr | Başarılı |

Tüm engine'ler büyük veri setini başarıyla indexledi.

---

## Performans Özeti

| Engine | Ortalama Latency | Stabilite |
|---|---:|---|
| Elasticsearch | 11-15 ms | Stabil |
| Solr | 1-2 ms | Stabil |
| OpenSearch | Benchmark sırasında hata | Stabil değil |

### OpenSearch Hatası

Benchmark sırasında:

- `429 Too Many Requests`
- `circuit_breaking_exception`
- Memory breaker limit aşımı

gözlendi.

Bu nedenle OpenSearch daha yüksek heap ile tekrar test edilmelidir.

---

## Migration Compatibility Sonuçları

### OpenSearch

| Metrik | Değer |
|---|---:|
| Top-K Overlap | 1.0 |
| Top1 Match Rate | 1.0 |
| Rank Shift | 0.0 |
| Compatibility Score | 92.49 |

**Sonuç:** Elasticsearch ile sonuç uyumluluğu çok yüksek, ancak performans/stabilite tarafında tekrar test gerekli.

### Solr

| Metrik | Değer |
|---|---:|
| Top-K Overlap | 1.0 |
| Top1 Match Rate | 1.0 |
| Rank Shift | 0.0 |
| Compatibility Score | 100.0 |

**Sonuç:** Elasticsearch ile aynı sonuçları üretirken daha düşük latency verdi.

---

## Genel Değerlendirme

| Engine | Uyumluluk | Performans | Değerlendirme |
|---|---|---|---|
| Elasticsearch | Baseline | Orta | Mevcut sistem |
| OpenSearch | Çok yüksek | Bellek problemi yaşadı | Yüksek heap ile tekrar test edilmeli |
| Solr | Çok yüksek | En iyi sonuç | Güçlü aday |

---

## Sonuç

- **Solr**, bu PoC kapsamında hem performans hem de Elasticsearch uyumluluğu açısından en güçlü adaydır.
- **OpenSearch**, Elasticsearch ile çok uyumlu sonuçlar üretmiştir ancak benchmark sırasında memory/circuit breaker problemi yaşamıştır.
- OpenSearch için bir sonraki adım, yalnızca PostgreSQL + OpenSearch çalışacak şekilde daha yüksek heap ile izole test yapılmasıdır.
