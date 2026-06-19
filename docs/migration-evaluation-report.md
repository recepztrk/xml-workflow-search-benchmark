# Migration Evaluation Report

## 1. Amaç

Bu rapor, mevcut Elasticsearch tabanlı XML workflow search yapısının OpenSearch veya Solr ile değiştirilme ihtimalini değerlendirir.

Değerlendirme yalnızca hız ölçümüne dayanmaz. Migration kararı için şu başlıklar birlikte ele alınmıştır:

| Kriter | Açıklama |
|---|---|
| Sonuç uyumluluğu | Elasticsearch ile aynı veya benzer sonuçların dönmesi |
| Sıralama uyumluluğu | İlk sonuç ve top-K sıralamasının korunması |
| Performans | Avg, P95 ve P99 latency değerleri |
| Stabilite | Error count, circuit breaker, kaynak kullanımı |
| Migration eforu | Mevcut Elasticsearch yapısından geçiş kolaylığı |
| Mimari uygunluk | Metadata-only search ve XML detail ayrımı |

Ana migration senaryosu:

```text
RAW_XML + METADATA_ONLY
```

`FULL_XML_RESPONSE`, migration karar senaryosu olarak değerlendirilmemiştir. Bu mod yalnızca büyük XML payload maliyetini görmek için denenmiştir.

---

## 2. Baseline ve Adaylar

| Engine | Rol | Değerlendirme Nedeni |
|---|---|---|
| Elasticsearch | Baseline | Mevcut sisteme en yakın referans |
| OpenSearch | Migration adayı | Elasticsearch API ve ekosistemine yakın |
| Solr | Alternatif search engine | Açık kaynak, hızlı full-text search performansı |

Elasticsearch mevcut davranışı temsil ettiği için baseline alınmıştır. Aday motorlar Elasticsearch sonuçlarıyla karşılaştırılmıştır.

---

## 3. Migration Compatibility Metrikleri

Migration Compatibility Evaluator, aday engine sonuçlarını Elasticsearch baseline sonuçlarıyla karşılaştırır.

| Metrik | Anlamı | İyi Sonuç |
|---|---|---|
| `topKOverlap` | Elasticsearch top-K sonuçları ile aday engine top-K sonuçlarının kesişimi | 1.0 |
| `top1MatchRate` | İlk sıradaki sonucun Elasticsearch ile aynı olması | 1.0 |
| `averageRankShift` | Ortak sonuçların sıralama kayması | 0.0 |
| `missingBaselineRate` | Elasticsearch sonucunda olup aday engine sonucunda eksik kalan kayıt oranı | 0.0 |
| `latencyRatioToBaseline` | Aday engine latency değerinin Elasticsearch'e oranı | 1.0 altı daha iyi |
| `migrationCompatibilityScore` | Genel migration uyumluluk skoru | 100'e yakın |

Bu metrikler, yalnızca hız değil, arama davranışı uyumluluğunu da ölçmek için eklenmiştir.

---

## 4. Compatibility Sonuçları

Büyük veri seti üzerinde Elasticsearch baseline alınarak OpenSearch ve Solr değerlendirilmiştir.

| Engine | Top-K Overlap | Top1 Match | Rank Shift | Missing Baseline | Latency Ratio | Score | Decision |
|---|---:|---:|---:|---:|---:|---:|---|
| OpenSearch | 1.0 | 1.0 | 0.0 | 0.0 | 2.50 | 92.49 | HIGH_COMPATIBILITY |
| Solr | 1.0 | 1.0 | 0.0 | 0.0 | 0.43 | 100.00 | HIGH_COMPATIBILITY |

Yorum:

- İki aday engine de Elasticsearch ile aynı top-K sonuçları aynı sırada döndürmüştür.
- Solr, aynı sonuç uyumluluğunu daha düşük latency ile üretmiştir.
- OpenSearch sonuç uyumluluğu açısından güçlüdür; ancak latency tarafında Elasticsearch baseline'dan yavaş kalmıştır.

---

## 5. Final İzole Performans Sonuçları

Aşağıdaki sonuçlar her engine ayrı çalıştırılarak alınmıştır.

| Engine | Avg Latency | P50 Avg | P95 Avg | P99 Avg | Success | Error | Yorum |
|---|---:|---:|---:|---:|---:|---:|---|
| Solr | 1.28 ms | 1.15 ms | 2.03 ms | 3.12 ms | 250 | 0 | En hızlı aday |
| Elasticsearch | 12.52 ms | 12.03 ms | 15.56 ms | 19.28 ms | 250 | 0 | Stabil baseline |
| OpenSearch | 22.05 ms | 21.11 ms | 28.56 ms | 30.67 ms | 250 | 0 | Stabil fakat daha yavaş |

Yorum:

- Solr, performans açısından en güçlü adaydır.
- Elasticsearch stabil baseline olarak çalışmıştır.
- OpenSearch izole ve yüksek heap ile stabil hale gelmiştir.
- OpenSearch stabil olmasına rağmen Elasticsearch'ten yaklaşık 1.76x, Solr'dan yaklaşık 17.25x daha yavaş kalmıştır.

---

## 6. OpenSearch Bellek Davranışı

OpenSearch ilk büyük karma testte bellek baskısı yaşamıştır.

| Test | Success | Error | Durum |
|---|---:|---:|---|
| Büyük karma test | 15 | 235 | `circuit_breaking_exception`, `429 Too Many Requests` |
| İzole yüksek heap test | 250 | 0 | Stabil çalıştı |

Yorum:

OpenSearch'ün ilk büyük testte hata vermesi doğrudan query uyumsuzluğundan değil, lokal Docker ortamındaki bellek baskısından kaynaklanmıştır. Heap artırılıp Elasticsearch ve Solr kapatılarak izole test yapıldığında OpenSearch hata üretmemiştir.

Ancak bu stabiliteye rağmen OpenSearch performans olarak Elasticsearch ve Solr'ın gerisinde kalmıştır. Bu yüzden OpenSearch için sonuç şu şekilde yorumlanmalıdır:

```text
Uyumluluk güçlü, stabilite kaynak ayarına bağlı, performans avantajı yok.
```

---

## 7. RAW_XML, EXTRACTED_DOCUMENT ve FULL_XML_RESPONSE Etkisi

### 7.1 RAW_XML

`RAW_XML`, mevcut sisteme en yakın search senaryosudur. XML içeriği tek büyük text alanı olarak indexlenmiştir.

Bu yaklaşım final karar için ana senaryo seçilmiştir.

### 7.2 EXTRACTED_DOCUMENT

`EXTRACTED_DOCUMENT`, XML içeriğinin parse edilerek daha anlamlı search alanlarına ayrıldığı alternatif yaklaşımdır.

Büyük testlerde beklenen avantajı üretmemiştir.

| Mode | Elasticsearch Avg | Solr Avg | Sonuç |
|---|---:|---:|---|
| RAW_XML + METADATA_ONLY | 12.66 ms | 1.12 ms | Ana senaryo için daha iyi |
| EXTRACTED_DOCUMENT + METADATA_ONLY | 27.83 ms | 1.20 ms | Genel avantaj sağlamadı |

Yorum:

Extracted yaklaşım teorik olarak daha temiz arama alanları sunar. Ancak mevcut sentetik veri ve query setinde arama uzayını yeterince daraltmamıştır. Elasticsearch tarafında belirgin şekilde yavaşlamış, Solr tarafında ise RAW_XML'e yakın ama daha iyi olmayan sonuçlar üretmiştir.

### 7.3 FULL_XML_RESPONSE

`FULL_XML_RESPONSE`, search sonucunda yaklaşık 10 MB XML payload dönülmesine neden olmuştur.

| Mode | Elasticsearch Avg | Solr Avg | Payload |
|---|---:|---:|---:|
| RAW_XML + METADATA_ONLY | 12.66 ms | 1.12 ms | ~0 KB |
| RAW_XML + FULL_XML_RESPONSE | 76.08 ms | 66.32 ms | ~10340 KB |

Yorum:

Full XML response, search motoru performansını ölçmek için uygun değildir. Çünkü latency büyük oranda response serialization ve payload transfer maliyetinden etkilenir.

Önerilen mimari:

```text
Search endpoint:
- workflowCode
- workflowName
- status
- domain
- score
- xmlSizeKb

Detail endpoint:
- seçilen workflow için full XML content
```

---

## 8. Migration Adayları Karşılaştırması

| Kriter | OpenSearch | Solr |
|---|---|---|
| Elasticsearch'e API yakınlığı | Yüksek | Düşük |
| Sonuç uyumluluğu | Yüksek | Yüksek |
| Sıralama uyumluluğu | Yüksek | Yüksek |
| Performans | Elasticsearch'ten yavaş | En hızlı |
| Kaynak hassasiyeti | Daha yüksek | Daha düşük |
| Migration eforu | Daha düşük | Daha yüksek |
| Uzun vadeli performans potansiyeli | Orta | Yüksek |
| Bu PoC için genel adaylık | İkinci aday | En güçlü aday |

---

## 9. Risk Değerlendirmesi

| Risk | OpenSearch | Solr | Açıklama |
|---|---|---|---|
| Sonuç farklılığı | Düşük | Düşük | Compatibility testlerinde top-K uyumu yüksek |
| Performans riski | Orta | Düşük | OpenSearch final testte daha yavaş kaldı |
| Bellek/kaynak riski | Orta-Yüksek | Düşük | OpenSearch karma testte circuit breaker verdi |
| Kod değişikliği riski | Düşük | Orta | OpenSearch Elasticsearch'e daha yakın |
| Operasyonel geçiş riski | Orta | Orta | İki engine için de production-like test gerekir |
| Relevance riski | Orta | Orta | Sentetik veriyle test edildiği için gerçek query logları gerekir |

---

## 10. Sonuç ve Öneri

Bu PoC kapsamında en güçlü teknik aday **Solr** olarak görünmektedir.

Solr:

- En düşük latency değerlerini üretmiştir.
- Elasticsearch baseline ile sonuç uyumluluğu göstermiştir.
- Büyük RAW_XML + METADATA_ONLY senaryosunda stabil çalışmıştır.
- Search endpointinin metadata-only çalıştığı mimariye uygundur.

OpenSearch:

- Elasticsearch'e API ve ekosistem olarak daha yakın olduğu için migration eforu daha düşüktür.
- Sonuç uyumluluğu yüksektir.
- Yüksek heap ile stabil hale gelmiştir.
- Ancak bu workload altında Elasticsearch ve Solr performans seviyesine ulaşamamıştır.

Final karar tablosu:

| Öncelik | Önerilen Engine |
|---|---|
| En düşük latency | Solr |
| Elasticsearch'e en yakın geçiş | OpenSearch |
| En düşük kod değişikliği | OpenSearch |
| En güçlü PoC sonucu | Solr |
| Mevcut baseline | Elasticsearch |

Final öneri:

```text
Performans öncelikli migration için Solr daha güçlü adaydır.
Elasticsearch API uyumluluğu ve daha düşük geçiş eforu öncelikliyse OpenSearch ikinci aday olarak değerlendirilebilir.
Production kararı öncesinde gerçek query logları, gerçek workflow verisi ve production-like cluster ortamında test tekrar edilmelidir.
```
