# Performance Benchmark Report

## 1. Amaç

Bu rapor, XML workflow dokümanları üzerinde yapılan search benchmark testlerini özetler. Odak nokta, Elasticsearch yerine kullanılabilecek açık kaynak alternatifleri teknik olarak değerlendirmektir.

Ana senaryo `RAW_XML + METADATA_ONLY` olarak kabul edilmiştir. Çünkü gerçek kullanımda arama sonucunda büyük XML içeriğinin tamamı değil, workflow metadata bilgisi dönmesi beklenir.

---

## 2. Test Verileri ve Senaryolar

Aşağıdaki testlerde aynı query ailesi kullanılmıştır:

```text
fatura itiraz
müşteri bilgileri
ödeme durumu
abonelik iptal
arıza kaydı
```

Küçük testlerde bazı çalışmalarda ilk 3 query kullanılmıştır. Büyük ve final testlerde 5 query kullanılmıştır.

| Test | Mode | Response | Engine'ler | Ölçüm | Ortalama Hit Count | Amaç |
|---|---|---|---|---:|---:|---|
| Small RAW_XML + METADATA_ONLY | `RAW_XML` | `METADATA_ONLY` | elasticsearch, opensearch, solr | 10 | 10 | Smoke test |
| Small EXTRACTED_DOCUMENT + METADATA_ONLY | `EXTRACTED_DOCUMENT` | `METADATA_ONLY` | elasticsearch, opensearch, solr | 10 | 10 | Extracted smoke test |
| Medium RAW_XML + METADATA_ONLY | `RAW_XML` | `METADATA_ONLY` | elasticsearch, opensearch, solr | 20 | 50 | Orta ölçek kontrol |
| Medium EXTRACTED_DOCUMENT + METADATA_ONLY | `EXTRACTED_DOCUMENT` | `METADATA_ONLY` | elasticsearch, opensearch, solr | 20 | 50 | Extracted orta ölçek kontrol |
| Large RAW_XML + METADATA_ONLY | `RAW_XML` | `METADATA_ONLY` | elasticsearch, solr | 50 | 100 | Büyük RAW baseline |
| Large EXTRACTED_DOCUMENT + METADATA_ONLY | `EXTRACTED_DOCUMENT` | `METADATA_ONLY` | elasticsearch, solr | 50 | 100 | Extracted büyük test |
| Large RAW_XML + FULL_XML_RESPONSE | `RAW_XML` | `FULL_XML_RESPONSE` | elasticsearch, solr | 50 | 100 | Payload maliyeti ölçümü |
| Large RAW_XML + METADATA_ONLY, karma engine | `RAW_XML` | `METADATA_ONLY` | elasticsearch, opensearch, solr | 50 | 73 | OpenSearch bellek davranışı |
| Final izole Elasticsearch | `RAW_XML` | `METADATA_ONLY` | elasticsearch | 50 | 100 | Final izole baseline |
| Final izole OpenSearch | `RAW_XML` | `METADATA_ONLY` | opensearch | 50 | 100 | Final izole OpenSearch |
| Final izole Solr | `RAW_XML` | `METADATA_ONLY` | solr | 50 | 100 | Final izole Solr |

---

## 3. Final İzole Test: RAW_XML + METADATA_ONLY

Final karşılaştırma, her engine ayrı çalıştırılarak yapılmıştır. Bu sayede container kaynak paylaşımı ve servisler arası bellek baskısı azaltılmıştır.

### 3.1 Genel Karşılaştırma

| Engine | Avg | Min | Max | P50 | P95 | P99 | Success | Error |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Solr | **1.28 ms** | 0.84 ms | 5.53 ms | **1.15 ms** | **2.03 ms** | **3.12 ms** | 250 | 0 |
| Elasticsearch | 12.52 ms | 10.45 ms | 24.48 ms | 12.03 ms | 15.56 ms | 19.28 ms | 250 | 0 |
| OpenSearch | 22.05 ms | 18.92 ms | 47.93 ms | 21.11 ms | 28.56 ms | 30.67 ms | 250 | 0 |

### 3.2 Query Bazlı Karşılaştırma

| Query | Solr Avg / P95 / P99 | Elasticsearch Avg / P95 / P99 | OpenSearch Avg / P95 / P99 | En iyi |
|---|---:|---:|---:|---|
| fatura itiraz | **1.60 / 2.83 / 3.48 ms** | 14.84 / 20.03 / 24.48 ms | 25.37 / 46.01 / 47.93 ms | Solr |
| müşteri bilgileri | **1.46 / 2.43 / 5.53 ms** | 12.30 / 15.66 / 20.11 ms | 21.57 / 24.89 / 25.36 ms | Solr |
| ödeme durumu | **1.15 / 1.69 / 1.94 ms** | 11.91 / 13.04 / 15.51 ms | 20.94 / 23.58 / 24.32 ms | Solr |
| abonelik iptal | **1.05 / 1.25 / 1.96 ms** | 11.77 / 14.27 / 21.13 ms | 20.85 / 24.39 / 27.10 ms | Solr |
| arıza kaydı | **1.13 / 1.94 / 2.68 ms** | 11.76 / 14.78 / 15.17 ms | 21.51 / 23.95 / 28.63 ms | Solr |

### 3.3 Oransal Fark

| Karşılaştırma | Sonuç |
|---|---:|
| Elasticsearch / Solr | Elasticsearch yaklaşık **9.78x yavaş** |
| OpenSearch / Elasticsearch | OpenSearch yaklaşık **1.76x yavaş** |
| OpenSearch / Solr | OpenSearch yaklaşık **17.23x yavaş** |

**Yorum:** Solr final izole testlerde açık ara en düşük latency değerlerini üretmiştir. Elasticsearch stabil baseline olarak çalışmıştır. OpenSearch yüksek heap ile hata vermeden çalışmıştır; ancak Elasticsearch'ten daha yavaş kalmıştır.

---

## 4. Small ve Medium Testler

Bu testler karar testi değil, ölçek büyütme ve entegrasyon doğrulama testleridir.

Tablodaki değerler `Avg / P95 / P99 / errorCount` formatındadır.

| Test | Elasticsearch | OpenSearch | Solr |
|---|---:|---:|---:|
| Small RAW_XML + METADATA_ONLY | 3.87 / 7.76 / 7.76 / err 0 | 5.94 / 9.25 / 9.25 / err 0 | 1.50 / 3.04 / 3.04 / err 0 |
| Small EXTRACTED_DOCUMENT + METADATA_ONLY | 3.62 / 5.32 / 5.32 / err 0 | 5.45 / 7.95 / 7.95 / err 0 | 1.43 / 2.08 / 2.08 / err 0 |
| Medium RAW_XML + METADATA_ONLY | 7.76 / 15.34 / 17.06 / err 0 | 10.79 / 14.34 / 21.11 / err 0 | 0.95 / 2.23 / 3.10 / err 0 |
| Medium EXTRACTED_DOCUMENT + METADATA_ONLY | 13.85 / 28.56 / 36.36 / err 0 | 13.96 / 17.59 / 25.76 / err 0 | 2.64 / 1.22 / 39.47 / err 0 |

**Yorum:** Small ve medium testler üç engine'in temel entegrasyonlarının çalıştığını gösterdi. Bu aşamada Solr genelde en düşük latency değerlerini verdi. EXTRACTED_DOCUMENT yaklaşımı küçük testlerde denenebilir görünse de büyük testlerde avantaj sağlamadı.

---

## 5. RAW_XML ve EXTRACTED_DOCUMENT Karşılaştırması

EXTRACTED_DOCUMENT yaklaşımı XML içeriğini parse edip daha anlamlı alanlara ayırmak için denendi. Ancak büyük testlerde beklenen performans avantajı oluşmadı.

| Büyük Test | Elasticsearch Avg / P95 / P99 | Solr Avg / P95 / P99 | Error |
|---|---:|---:|---:|
| Large RAW_XML + METADATA_ONLY | 12.66 / 19.92 / 27.14 | 1.12 / 1.79 / 2.24 | 0 |
| Large EXTRACTED_DOCUMENT + METADATA_ONLY | 27.83 / 49.77 / 71.02 | 1.20 / 2.11 / 3.92 | 0 |

**Yorum:** Elasticsearch tarafında EXTRACTED_DOCUMENT, RAW_XML'e göre daha yüksek latency üretti. Solr tarafında extracted yaklaşım RAW_XML'e yakın kalsa da belirgin bir avantaj sağlamadı. Bu nedenle ana benchmark senaryosu RAW_XML olarak bırakıldı.

---

## 6. FULL_XML_RESPONSE Denemesi

FULL_XML_RESPONSE, ana migration senaryosu değildir. Deneme amaçlı yapılmıştır. Amaç, büyük XML payload'un response latency üzerindeki etkisini görmekti.

| Test | Elasticsearch Avg / P95 / P99 | Solr Avg / P95 / P99 | Ortalama Payload |
|---|---:|---:|---:|
| Large RAW_XML + FULL_XML_RESPONSE | 76.08 / 88.07 / 107.59 | 66.32 / 75.97 / 87.80 | ~10340 KB |

**Yorum:** FULL_XML_RESPONSE modunda yaklaşık 10 MB response payload oluştu. Bu yüzden latency ciddi arttı. Bu sonuç, search endpointinin metadata-only çalışması gerektiğini destekliyor. Full XML gerekiyorsa ayrı bir detail endpoint üzerinden alınması daha doğru olur.

---

## 7. OpenSearch Bellek ve İzole Test Sonucu

OpenSearch büyük karma testte düşük kaynak koşullarında circuit breaker hatası verdi. Daha sonra yalnızca OpenSearch çalıştırılıp heap artırıldığında stabil hale geldi.

| Test | Avg | P95 | P99 | Success | Error | Yorum |
|---|---:|---:|---:|---:|---:|---|
| Büyük karma test | 17.34 ms | 57.74 ms | 57.74 ms | 15 | 235 | Bellek baskısı / circuit breaker |
| Final izole yüksek heap test | 22.05 ms | 28.56 ms | 30.67 ms | 250 | 0 | Stabil fakat yavaş |

**Yorum:** OpenSearch'ün ilk büyük testteki problemi tamamen engine'in çalışmaması değil, lokal kaynak koşullarında bellek baskısına girmesiydi. İzole ve yüksek heap ile stabil çalıştı. Buna rağmen final testlerde Elasticsearch'ten yaklaşık 1.76x, Solr'dan yaklaşık 17.25x daha yavaş kaldı.

---

## 8. Teknik Sonuç

| Engine | Güçlü Taraf | Zayıf Taraf | Genel Değerlendirme |
|---|---|---|---|
| Solr | En düşük latency, stabil çalışma | Elasticsearch API uyumu doğal değil | Performans odaklı en güçlü aday |
| Elasticsearch | Stabil baseline, mevcut sisteme en yakın mevcut yapı | Lisans / kurumsal kullanım problemi | Karşılaştırma referansı |
| OpenSearch | Elasticsearch'e yakın API ve migration yolu | Bu workload altında daha yüksek latency | Uyumlu ama performans avantajı yok |

## 9. Final Yorum

Bu benchmark sonucunda `RAW_XML + METADATA_ONLY` senaryosu en doğru ana ölçüm senaryosu olarak kalmıştır.

- Solr performans açısından en güçlü açık kaynak alternatiftir.
- Elasticsearch stabil baseline olarak çalışmıştır.
- OpenSearch, yüksek heap ile stabil hale gelmiştir ancak Elasticsearch ve Solr performans seviyesine ulaşamamıştır.
- EXTRACTED_DOCUMENT mevcut veri ve query setinde avantaj sağlamamıştır.
- FULL_XML_RESPONSE search endpoint için uygun değildir; XML içeriği ayrı detail endpoint ile alınmalıdır.
