# RAW_XML Benchmark Sonuçları

## 1. Amaç

Bu doküman, `xml-workflow-search-benchmark` projesi kapsamında yürütülen ilk `RAW_XML` benchmark testlerinin sonuçlarını özetler.

Projenin temel amacı, PostgreSQL üzerinde tutulan büyük XML tabanlı workflow dokümanları üzerinde farklı search engine servislerinin free-text search performansını karşılaştırmaktır.

Bu raporda özellikle mevcut sisteme en yakın senaryo olan `RAW_XML` modu incelenmiştir.

`RAW_XML` modunda XML içeriği uygulama tarafında parse edilmeden search engine servislerine aktarılır.

```text
PostgreSQL
→ WorkflowDocument
→ xmlContent
→ Elasticsearch / OpenSearch / Solr
→ xmlContent üzerinde free-text search
```

Bu nedenle `RAW_XML` modu, mevcut sistem davranışına en yakın benchmark senaryosu olarak değerlendirilmiştir.

---

## 2. Test Edilen Search Engine Servisleri

Benchmark kapsamında üç search engine servisi test edilmiştir.

| Servis | Rol |
|---|---|
| Elasticsearch | Mevcut sistem / baseline |
| OpenSearch | Elasticsearch’e yakın alternatif |
| Apache Solr | Lucene tabanlı kurumsal alternatif |

İlk testlerde üç servis birlikte çalıştırılmıştır. Daha sonra OpenSearch tarafında bellek baskısı ve hata davranışı gözlendiği için Elasticsearch + Solr izolasyon testleri ayrıca yapılmıştır.

---

## 3. Test Ortamı

Testler lokal geliştirme ortamında çalıştırılmıştır.

| Bileşen | Değer |
|---|---|
| İşletim sistemi | macOS |
| Makine | MacBook Air |
| RAM | 16 GB |
| Runtime | Docker Desktop |
| Spring Boot API | `localhost:8080` |
| PostgreSQL | `localhost:5434` |
| Elasticsearch | `localhost:9201` |
| OpenSearch | `localhost:9200` |
| Apache Solr | `localhost:8983` |

İlk testlerde Elasticsearch, OpenSearch ve Solr için JVM heap değeri 1 GB olarak çalıştırılmıştır.

100 adet yaklaşık 2 MB XML dokümanı ile RAW_XML reindex aşamasında OpenSearch tarafında `circuit_breaking_exception` oluştuğu için Elasticsearch, OpenSearch ve Solr heap değerleri 2 GB seviyesine çıkarılmıştır.

Güncellenen JVM heap ayarları:

| Servis | Heap |
|---|---:|
| Elasticsearch | 2 GB |
| OpenSearch | 2 GB |
| Apache Solr | 2 GB |

Heap güncellemesinden sonra 100 adet yaklaşık 2 MB XML dokümanı için RAW_XML reindex işlemi üç servis için de başarıyla tamamlanmıştır.

---

## 4. Benchmark Parametreleri

Benchmark testlerinde kullanılan temel parametreler aşağıdaki gibidir.

| Parametre | Değer |
|---|---:|
| Arama modu | `RAW_XML` |
| Response tipi | Metadata-only |
| Query sayısı | 3 |
| Limit | 5 |
| Warm-up iteration | 5 |
| Measurement iteration | 20 |

Kullanılan query seti:

```text
fatura itiraz
müşteri bilgileri
ödeme durumu
```

Benchmark runner, her engine + query kombinasyonu için önce warm-up sorguları çalıştırır. Warm-up sonuçları ölçüme dahil edilmez. Ardından measurement iteration sonuçları üzerinden aşağıdaki metrikler hesaplanır.

```text
avgMs
minMs
maxMs
p50Ms
p95Ms
p99Ms
successCount
errorCount
lastHitCount
samplesMs
```

---

## 5. Kullanılan Veri Setleri

Testlerde sentetik XML workflow dokümanları kullanılmıştır.

XML boyutu, `screenCount` parametresi artırılarak kontrollü şekilde büyütülmüştür.

| screenCount | Yaklaşık XML Boyutu |
|---:|---:|
| 20 | 34 KB |
| 300 | 515 KB |
| 600 | 1032 KB |
| 1200 | 2068 KB |

Bu sonuç, mevcut XML generator’ın yaklaşık lineer boyut artışı sağladığını göstermektedir.

Çalıştırılan ana RAW_XML test zinciri aşağıdaki gibidir.

| Test No | Doküman Sayısı | XML Boyutu | Toplam Yaklaşık XML Hacmi |
|---:|---:|---:|---:|
| 1 | 10 | 34 KB | 340 KB |
| 2 | 10 | 515 KB | 5 MB |
| 3 | 100 | 515 KB | 51.5 MB |
| 4 | 10 | 1032 KB | 10 MB |
| 5 | 10 | 2068 KB | 20 MB |
| 6 | 50 | 2068 KB | 103 MB |
| 7 | 100 | 2068 KB | 206 MB |
| 8 | 100 | 2068 KB | 206 MB, Elasticsearch + Solr izolasyon testi |

---

## 6. RAW_XML Test Sonuçları

### 6.1. 10 Doküman x 34 KB

Bu test, benchmark altyapısının uçtan uca çalıştığını doğrulamak için smoke test olarak yapılmıştır.

| Engine | Ortalama Avg ms | Ortalama P50 ms | Ortalama P95 ms | Durum |
|---|---:|---:|---:|---|
| Elasticsearch | 2.93 | 3.00 | 3.00 | Başarılı |
| OpenSearch | 5.60 | 3.33 | 7.33 | Başarılı, outlier mevcut |
| Solr | 1.32 | 1.00 | 1.67 | Başarılı |

Gözlem:

```text
Üç servis de hatasız çalışmıştır.
Bu test yalnızca benchmark altyapısının doğrulanması amacıyla değerlendirilmelidir.
```

---

### 6.2. 10 Doküman x 515 KB

Bu testte doküman sayısı sabit tutulmuş, XML boyutu yaklaşık 15 kat artırılmıştır.

| Engine | Ortalama Avg ms | Ortalama P50 ms | Ortalama P95 ms | Durum |
|---|---:|---:|---:|---|
| Elasticsearch | 5.23 | 5.00 | 6.33 | Başarılı |
| OpenSearch | 12.12 | 9.33 | 12.67 | Başarılı, outlier mevcut |
| Solr | 0.98 | 1.00 | 1.00 | Başarılı |

Gözlem:

```text
XML boyutu 34 KB seviyesinden 515 KB seviyesine çıktığında Elasticsearch ve OpenSearch tarafında latency artışı gözlenmiştir.
Solr düşük latency üretmeye devam etmiştir.
```

---

### 6.3. 100 Doküman x 515 KB

Bu testte XML boyutu sabit tutulmuş, doküman sayısı 10’dan 100’e çıkarılmıştır.

| Engine | Ortalama Avg ms | Ortalama P50 ms | Ortalama P95 ms | Durum |
|---|---:|---:|---:|---|
| Elasticsearch | 6.60 | 5.67 | 8.67 | Başarılı |
| OpenSearch | 8.08 | 7.33 | 10.33 | Başarılı |
| Solr | 0.77 | 0.67 | 1.00 | Başarılı |

Gözlem:

```text
100 adet 515 KB XML dokümanı üzerinde üç servis de hatasız çalışmıştır.
OpenSearch bu testte önceki küçük testlere göre daha stabil görünmüştür.
```

---

### 6.4. 10 Doküman x 1032 KB

Bu testte tekil XML boyutu yaklaşık 1 MB seviyesine çıkarılmıştır.

| Engine | Ortalama Avg ms | Ortalama P50 ms | Ortalama P95 ms | Durum |
|---|---:|---:|---:|---|
| Elasticsearch | 9.98 | 8.33 | 15.67 | Başarılı |
| OpenSearch | 13.47 | 13.67 | 14.67 | Başarılı |
| Solr | 0.38 | 0.00 | 1.67 | Başarılı |

Gözlem:

```text
Elasticsearch ve OpenSearch tarafında XML boyutu büyüdükçe latency artışı belirginleşmiştir.
Solr tarafında bazı ölçümlerde 0 ms değeri görülmüştür.
Bu, işlemin gerçek anlamda sıfır sürede çalıştığı anlamına gelmez; milisaniye çözünürlüğündeki ölçüm nedeniyle 1 ms altındaki süreler 0 olarak görünmektedir.
```

---

### 6.5. 10 Doküman x 2068 KB

Bu testte tekil XML boyutu yaklaşık 2 MB seviyesine çıkarılmıştır. Bu boyut, projenin problem bağlamındaki ana hedef boyutudur.

| Engine | Ortalama Avg ms | Ortalama P50 ms | Ortalama P95 ms | Durum |
|---|---:|---:|---:|---|
| Elasticsearch | 13.88 | 11.67 | 21.67 | Başarılı |
| OpenSearch | 25.03 | 24.67 | 25.67 | Başarılı |
| Solr | 0.77 | 0.67 | 1.00 | Başarılı |

Gözlem:

```text
10 adet yaklaşık 2 MB XML dokümanı üzerinde üç servis de hatasız çalışmıştır.
OpenSearch, 2 MB seviyesinde Elasticsearch’e göre daha yüksek latency üretmiştir.
```

---

## 7. OpenSearch Bellek Sınırı ve Heap Güncellemesi

100 adet yaklaşık 2 MB XML dokümanı ile RAW_XML reindex denemesinde OpenSearch tarafında hata oluşmuştur.

Hata özeti:

```text
429 Too Many Requests
circuit_breaking_exception
[parent] Data too large
```

Bu hata, OpenSearch parent circuit breaker mekanizmasının isteği reddettiğini göstermektedir.

İlk denemede OpenSearch 1 GB heap ile 100 adet 2 MB XML dokümanını tam olarak indexleyememiştir. OpenSearch index durumu incelendiğinde `workflow-documents` indexinde 88 doküman bulunduğu görülmüştür. Bu da reindex işleminin kısmi olarak ilerlediğini, ancak 100 dokümanlık yük tamamlanmadan hata aldığını göstermektedir.

Bunun üzerine Elasticsearch, OpenSearch ve Solr için JVM heap değerleri 2 GB seviyesine çıkarılmıştır.

Heap güncellemesi sonrası aynı reindex senaryosu başarıyla tamamlanmıştır.

| Servis | Indexed Document Count |
|---|---:|
| Elasticsearch | 100 |
| OpenSearch | 100 |
| Solr | 100 |

Ancak OpenSearch loglarında 2 GB heap ile de yüksek heap kullanımı ve GC tetiklenmesi gözlenmiştir.

```text
attempting to trigger G1GC due to high heap usage
GC did bring memory usage down
```

Lokal makinede swap kullanımının yüksek olması nedeniyle heap değerleri 3 GB seviyesine çıkarılmamıştır. Daha yüksek heap ayarlarının test ortamı kaynak baskısını artırarak benchmark sonuçlarını kirletebileceği değerlendirilmiştir.

---

## 8. 100 Doküman x 2068 KB — Üç Engine Birlikte

Heap 2 GB seviyesine çıkarıldıktan sonra 100 adet yaklaşık 2 MB XML dokümanı için üç engine birlikte benchmark edilmiştir.

| Engine | Query | Avg ms | P50 ms | P95 ms | P99 ms | Success | Error |
|---|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | fatura itiraz | 19.15 | 19 | 21 | 22 | 20 | 0 |
| Elasticsearch | müşteri bilgileri | 13.45 | 13 | 15 | 15 | 20 | 0 |
| Elasticsearch | ödeme durumu | 14.05 | 13 | 15 | 36 | 20 | 0 |
| OpenSearch | fatura itiraz | 52.30 | 32 | 116 | 230 | 20 | 0 |
| OpenSearch | müşteri bilgileri | 0.00 | 0 | 0 | 0 | 0 | 20 |
| OpenSearch | ödeme durumu | 0.00 | 0 | 0 | 0 | 0 | 20 |
| Solr | fatura itiraz | 3.25 | 3 | 5 | 12 | 20 | 0 |
| Solr | müşteri bilgileri | 1.90 | 2 | 3 | 3 | 20 | 0 |
| Solr | ödeme durumu | 1.60 | 1 | 3 | 3 | 20 | 0 |

Gözlem:

```text
Elasticsearch ve Solr tüm sorguları hatasız tamamlamıştır.
OpenSearch yalnızca ilk sorguda başarılı olmuş, sonraki iki sorguda tüm measurement iteration’larda hata üretmiştir.
```

Başarı oranı:

| Engine | Başarılı Ölçüm | Hatalı Ölçüm | Durum |
|---|---:|---:|---|
| Elasticsearch | 60/60 | 0/60 | Başarılı |
| OpenSearch | 20/60 | 40/60 | Kararsız / başarısız |
| Solr | 60/60 | 0/60 | Başarılı |

Bu sonuç, OpenSearch’ün 2 GB heap ile 100 adet 2 MB RAW_XML search senaryosunda kararlı davranmadığını göstermektedir.

---

## 9. 50 Doküman x 2068 KB — Üç Engine Birlikte

OpenSearch davranışını daha kontrollü incelemek için doküman sayısı 50’ye düşürülerek aynı test tekrarlanmıştır.

| Engine | Query | Avg ms | P50 ms | P95 ms | P99 ms | Success | Error |
|---|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | fatura itiraz | 18.15 | 17 | 25 | 29 | 20 | 0 |
| Elasticsearch | müşteri bilgileri | 14.95 | 13 | 16 | 40 | 20 | 0 |
| Elasticsearch | ödeme durumu | 13.65 | 13 | 18 | 22 | 20 | 0 |
| OpenSearch | fatura itiraz | 49.53 | 48 | 169 | 169 | 19 | 1 |
| OpenSearch | müşteri bilgileri | 0.00 | 0 | 0 | 0 | 0 | 20 |
| OpenSearch | ödeme durumu | 0.00 | 0 | 0 | 0 | 0 | 20 |
| Solr | fatura itiraz | 15.20 | 5 | 64 | 81 | 20 | 0 |
| Solr | müşteri bilgileri | 2.00 | 2 | 4 | 5 | 20 | 0 |
| Solr | ödeme durumu | 1.85 | 2 | 3 | 5 | 20 | 0 |

Gözlem:

```text
50 adet 2 MB XML dokümanı seviyesinde de OpenSearch kararlı çalışmamıştır.
İlk sorguda 19/20 başarılı ölçüm üretmiş, sonraki iki sorguda tamamen hata vermiştir.
```

Solr tarafında ilk sorguda yüksek outlier değerleri gözlenmiştir. Bu durumun OpenSearch kaynaklı heap/GC baskısının sistem geneline etkisiyle ilişkili olabileceği düşünülmüş ve Elasticsearch + Solr izolasyon testi yapılmıştır.

---

## 10. 50 Doküman x 2068 KB — Elasticsearch + Solr İzolasyon Testi

OpenSearch devre dışı bırakılarak yalnızca Elasticsearch ve Solr test edilmiştir.

| Engine | Query | Avg ms | P50 ms | P95 ms | P99 ms | Success | Error |
|---|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | fatura itiraz | 19.80 | 17 | 20 | 75 | 20 | 0 |
| Elasticsearch | müşteri bilgileri | 14.10 | 13 | 16 | 27 | 20 | 0 |
| Elasticsearch | ödeme durumu | 12.75 | 12 | 17 | 20 | 20 | 0 |
| Solr | fatura itiraz | 3.65 | 3 | 11 | 11 | 20 | 0 |
| Solr | müşteri bilgileri | 3.40 | 2 | 10 | 15 | 20 | 0 |
| Solr | ödeme durumu | 2.20 | 2 | 3 | 3 | 20 | 0 |

Gözlem:

```text
OpenSearch devre dışı bırakıldığında Solr tarafındaki yüksek outlier değerleri belirgin şekilde azalmıştır.
Bu sonuç, önceki testte OpenSearch yükünün sistem genelinde bellek/GC baskısı oluşturarak sonraki ölçümleri kısmen etkilemiş olabileceğini düşündürmektedir.
```

---

## 11. 100 Doküman x 2068 KB — Elasticsearch + Solr İzolasyon Testi

Raporlamaya geçmeden önce ana hedef senaryoda OpenSearch devre dışı bırakılarak yalnızca Elasticsearch ve Solr test edilmiştir.

Test koşulu:

```text
100 doküman
2068 KB XML / doküman
RAW_XML
metadata-only response
OpenSearch devre dışı
```

| Engine | Query | Avg ms | P50 ms | P95 ms | P99 ms | Success | Error |
|---|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | fatura itiraz | 19.15 | 16 | 21 | 78 | 20 | 0 |
| Elasticsearch | müşteri bilgileri | 13.45 | 12 | 14 | 35 | 20 | 0 |
| Elasticsearch | ödeme durumu | 14.55 | 12 | 23 | 40 | 20 | 0 |
| Solr | fatura itiraz | 2.25 | 1 | 6 | 8 | 20 | 0 |
| Solr | müşteri bilgileri | 2.90 | 2 | 5 | 16 | 20 | 0 |
| Solr | ödeme durumu | 1.45 | 1 | 3 | 4 | 20 | 0 |

Özet:

| Engine | Ortalama Avg ms | Ortalama P50 ms | Ortalama P95 ms | Başarı |
|---|---:|---:|---:|---|
| Elasticsearch | 15.72 | 13.33 | 19.33 | 60/60 |
| Solr | 2.20 | 1.33 | 4.67 | 60/60 |

Gözlem:

```text
OpenSearch devre dışı bırakıldığında, Elasticsearch ve Solr 100 adet yaklaşık 2 MB XML dokümanı üzerinde RAW_XML benchmarkını hatasız tamamlamıştır.
Solr, aynı koşulda Elasticsearch’e göre daha düşük latency üretmiştir.
```

---

## 12. RAW_XML Benchmark Detaylı Sonuç Tablosu

Aşağıdaki tablo, RAW_XML modunda yapılan tüm benchmark denemelerini tek tabloda özetler.

Tabloda kullanılan kısaltmalar:

| Alan | Açıklama |
|---|---|
| `Docs` | Testte kullanılan workflow dokümanı sayısı |
| `XML KB` | Tekil XML doküman boyutu |
| `Scope` | Test kapsamı |
| `Avg` | Ortalama latency |
| `Min` | En düşük latency |
| `Max` | En yüksek latency |
| `P50` | Median latency |
| `P95` | 95. yüzdelik latency |
| `P99` | 99. yüzdelik latency |
| `Success` | Başarılı measurement sayısı |
| `Error` | Hatalı measurement sayısı |
| `Hit` | Son başarılı sorguda bulunan toplam eşleşme sayısı |

> Not: `samplesMs` değerleri ham ölçüm listeleridir. Rapor okunabilirliğini korumak için bu tabloda ham `samplesMs` listeleri verilmemiştir. Bu listeler API response çıktılarında mevcuttur ve ileride CSV/JSON export ile saklanmalıdır.

| Test | Scope | Docs | XML KB | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
|---:|---|---:|---:|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | All engines | 10 | 34 | Elasticsearch | fatura itiraz | 3.10 | 3 | 5 | 3 | 3 | 5 | 20 | 0 | 10 |
| 1 | All engines | 10 | 34 | Elasticsearch | müşteri bilgileri | 3.05 | 3 | 4 | 3 | 3 | 4 | 20 | 0 | 10 |
| 1 | All engines | 10 | 34 | Elasticsearch | ödeme durumu | 2.65 | 2 | 4 | 3 | 3 | 4 | 20 | 0 | 10 |
| 1 | All engines | 10 | 34 | OpenSearch | fatura itiraz | 9.95 | 3 | 108 | 4 | 11 | 108 | 20 | 0 | 10 |
| 1 | All engines | 10 | 34 | OpenSearch | müşteri bilgileri | 3.30 | 2 | 6 | 3 | 5 | 6 | 20 | 0 | 10 |
| 1 | All engines | 10 | 34 | OpenSearch | ödeme durumu | 3.55 | 3 | 6 | 3 | 6 | 6 | 20 | 0 | 10 |
| 1 | All engines | 10 | 34 | Solr | fatura itiraz | 1.25 | 1 | 2 | 1 | 2 | 2 | 20 | 0 | 10 |
| 1 | All engines | 10 | 34 | Solr | müşteri bilgileri | 1.20 | 1 | 3 | 1 | 2 | 3 | 20 | 0 | 10 |
| 1 | All engines | 10 | 34 | Solr | ödeme durumu | 1.50 | 1 | 11 | 1 | 1 | 11 | 20 | 0 | 10 |
| 2 | All engines | 10 | 515 | Elasticsearch | fatura itiraz | 5.20 | 4 | 10 | 5 | 6 | 10 | 20 | 0 | 10 |
| 2 | All engines | 10 | 515 | Elasticsearch | müşteri bilgileri | 5.20 | 4 | 7 | 5 | 7 | 7 | 20 | 0 | 10 |
| 2 | All engines | 10 | 515 | Elasticsearch | ödeme durumu | 5.30 | 5 | 6 | 5 | 6 | 6 | 20 | 0 | 10 |
| 2 | All engines | 10 | 515 | OpenSearch | fatura itiraz | 20.95 | 9 | 151 | 14 | 18 | 151 | 20 | 0 | 10 |
| 2 | All engines | 10 | 515 | OpenSearch | müşteri bilgileri | 7.70 | 7 | 10 | 7 | 10 | 10 | 20 | 0 | 10 |
| 2 | All engines | 10 | 515 | OpenSearch | ödeme durumu | 7.70 | 6 | 10 | 7 | 10 | 10 | 20 | 0 | 10 |
| 2 | All engines | 10 | 515 | Solr | fatura itiraz | 1.00 | 1 | 1 | 1 | 1 | 1 | 20 | 0 | 10 |
| 2 | All engines | 10 | 515 | Solr | müşteri bilgileri | 1.00 | 1 | 1 | 1 | 1 | 1 | 20 | 0 | 10 |
| 2 | All engines | 10 | 515 | Solr | ödeme durumu | 0.95 | 0 | 1 | 1 | 1 | 1 | 20 | 0 | 10 |
| 3 | All engines | 100 | 515 | Elasticsearch | fatura itiraz | 6.30 | 5 | 14 | 6 | 10 | 14 | 20 | 0 | 100 |
| 3 | All engines | 100 | 515 | Elasticsearch | müşteri bilgileri | 8.60 | 5 | 41 | 6 | 11 | 41 | 20 | 0 | 100 |
| 3 | All engines | 100 | 515 | Elasticsearch | ödeme durumu | 4.90 | 4 | 5 | 5 | 5 | 5 | 20 | 0 | 100 |
| 3 | All engines | 100 | 515 | OpenSearch | fatura itiraz | 8.30 | 7 | 11 | 8 | 11 | 11 | 20 | 0 | 100 |
| 3 | All engines | 100 | 515 | OpenSearch | müşteri bilgileri | 7.95 | 6 | 11 | 7 | 10 | 11 | 20 | 0 | 100 |
| 3 | All engines | 100 | 515 | OpenSearch | ödeme durumu | 8.00 | 6 | 11 | 7 | 10 | 11 | 20 | 0 | 100 |
| 3 | All engines | 100 | 515 | Solr | fatura itiraz | 0.95 | 0 | 2 | 1 | 1 | 2 | 20 | 0 | 100 |
| 3 | All engines | 100 | 515 | Solr | müşteri bilgileri | 0.95 | 0 | 2 | 1 | 1 | 2 | 20 | 0 | 100 |
| 3 | All engines | 100 | 515 | Solr | ödeme durumu | 0.40 | 0 | 1 | 0 | 1 | 1 | 20 | 0 | 100 |
| 4 | All engines | 10 | 1032 | Elasticsearch | fatura itiraz | 8.80 | 7 | 14 | 8 | 13 | 14 | 20 | 0 | 10 |
| 4 | All engines | 10 | 1032 | Elasticsearch | müşteri bilgileri | 12.20 | 8 | 53 | 9 | 20 | 53 | 20 | 0 | 10 |
| 4 | All engines | 10 | 1032 | Elasticsearch | ödeme durumu | 8.95 | 7 | 16 | 8 | 14 | 16 | 20 | 0 | 10 |
| 4 | All engines | 10 | 1032 | OpenSearch | fatura itiraz | 13.60 | 11 | 18 | 14 | 15 | 18 | 20 | 0 | 10 |
| 4 | All engines | 10 | 1032 | OpenSearch | müşteri bilgileri | 14.10 | 11 | 26 | 14 | 15 | 26 | 20 | 0 | 10 |
| 4 | All engines | 10 | 1032 | OpenSearch | ödeme durumu | 12.70 | 11 | 14 | 13 | 14 | 14 | 20 | 0 | 10 |
| 4 | All engines | 10 | 1032 | Solr | fatura itiraz | 0.50 | 0 | 2 | 0 | 1 | 2 | 20 | 0 | 10 |
| 4 | All engines | 10 | 1032 | Solr | müşteri bilgileri | 0.35 | 0 | 2 | 0 | 1 | 2 | 20 | 0 | 10 |
| 4 | All engines | 10 | 1032 | Solr | ödeme durumu | 0.30 | 0 | 3 | 0 | 3 | 3 | 20 | 0 | 10 |
| 5 | All engines | 10 | 2068 | Elasticsearch | fatura itiraz | 16.90 | 12 | 69 | 13 | 26 | 69 | 20 | 0 | 10 |
| 5 | All engines | 10 | 2068 | Elasticsearch | müşteri bilgileri | 12.50 | 11 | 25 | 11 | 20 | 25 | 20 | 0 | 10 |
| 5 | All engines | 10 | 2068 | Elasticsearch | ödeme durumu | 12.25 | 11 | 20 | 11 | 19 | 20 | 20 | 0 | 10 |
| 5 | All engines | 10 | 2068 | OpenSearch | fatura itiraz | 24.45 | 24 | 30 | 24 | 25 | 30 | 20 | 0 | 10 |
| 5 | All engines | 10 | 2068 | OpenSearch | müşteri bilgileri | 25.40 | 25 | 28 | 25 | 26 | 28 | 20 | 0 | 10 |
| 5 | All engines | 10 | 2068 | OpenSearch | ödeme durumu | 25.25 | 24 | 26 | 25 | 26 | 26 | 20 | 0 | 10 |
| 5 | All engines | 10 | 2068 | Solr | fatura itiraz | 1.05 | 1 | 2 | 1 | 1 | 2 | 20 | 0 | 10 |
| 5 | All engines | 10 | 2068 | Solr | müşteri bilgileri | 1.20 | 1 | 3 | 1 | 2 | 3 | 20 | 0 | 10 |
| 5 | All engines | 10 | 2068 | Solr | ödeme durumu | 0.05 | 0 | 1 | 0 | 0 | 1 | 20 | 0 | 10 |
| 6 | All engines | 100 | 2068 | Elasticsearch | fatura itiraz | 19.15 | 17 | 22 | 19 | 21 | 22 | 20 | 0 | 100 |
| 6 | All engines | 100 | 2068 | Elasticsearch | müşteri bilgileri | 13.45 | 13 | 15 | 13 | 15 | 15 | 20 | 0 | 100 |
| 6 | All engines | 100 | 2068 | Elasticsearch | ödeme durumu | 14.05 | 12 | 36 | 13 | 15 | 36 | 20 | 0 | 100 |
| 6 | All engines | 100 | 2068 | OpenSearch | fatura itiraz | 52.30 | 22 | 230 | 32 | 116 | 230 | 20 | 0 | 100 |
| 6 | All engines | 100 | 2068 | OpenSearch | müşteri bilgileri | 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 20 | 0 |
| 6 | All engines | 100 | 2068 | OpenSearch | ödeme durumu | 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 20 | 0 |
| 6 | All engines | 100 | 2068 | Solr | fatura itiraz | 3.25 | 1 | 12 | 3 | 5 | 12 | 20 | 0 | 100 |
| 6 | All engines | 100 | 2068 | Solr | müşteri bilgileri | 1.90 | 1 | 3 | 2 | 3 | 3 | 20 | 0 | 100 |
| 6 | All engines | 100 | 2068 | Solr | ödeme durumu | 1.60 | 1 | 3 | 1 | 3 | 3 | 20 | 0 | 100 |
| 7 | All engines | 50 | 2068 | Elasticsearch | fatura itiraz | 18.15 | 16 | 29 | 17 | 25 | 29 | 20 | 0 | 50 |
| 7 | All engines | 50 | 2068 | Elasticsearch | müşteri bilgileri | 14.95 | 12 | 40 | 13 | 16 | 40 | 20 | 0 | 50 |
| 7 | All engines | 50 | 2068 | Elasticsearch | ödeme durumu | 13.65 | 12 | 22 | 13 | 18 | 22 | 20 | 0 | 50 |
| 7 | All engines | 50 | 2068 | OpenSearch | fatura itiraz | 49.53 | 21 | 169 | 48 | 169 | 169 | 19 | 1 | 50 |
| 7 | All engines | 50 | 2068 | OpenSearch | müşteri bilgileri | 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 20 | 0 |
| 7 | All engines | 50 | 2068 | OpenSearch | ödeme durumu | 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 20 | 0 |
| 7 | All engines | 50 | 2068 | Solr | fatura itiraz | 15.20 | 1 | 81 | 5 | 64 | 81 | 20 | 0 | 50 |
| 7 | All engines | 50 | 2068 | Solr | müşteri bilgileri | 2.00 | 1 | 5 | 2 | 4 | 5 | 20 | 0 | 50 |
| 7 | All engines | 50 | 2068 | Solr | ödeme durumu | 1.85 | 1 | 5 | 2 | 3 | 5 | 20 | 0 | 50 |
| 8 | ES + Solr isolation | 50 | 2068 | Elasticsearch | fatura itiraz | 19.80 | 15 | 75 | 17 | 20 | 75 | 20 | 0 | 50 |
| 8 | ES + Solr isolation | 50 | 2068 | Elasticsearch | müşteri bilgileri | 14.10 | 12 | 27 | 13 | 16 | 27 | 20 | 0 | 50 |
| 8 | ES + Solr isolation | 50 | 2068 | Elasticsearch | ödeme durumu | 12.75 | 11 | 20 | 12 | 17 | 20 | 20 | 0 | 50 |
| 8 | ES + Solr isolation | 50 | 2068 | Solr | fatura itiraz | 3.65 | 1 | 11 | 3 | 11 | 11 | 20 | 0 | 50 |
| 8 | ES + Solr isolation | 50 | 2068 | Solr | müşteri bilgileri | 3.40 | 1 | 15 | 2 | 10 | 15 | 20 | 0 | 50 |
| 8 | ES + Solr isolation | 50 | 2068 | Solr | ödeme durumu | 2.20 | 1 | 3 | 2 | 3 | 3 | 20 | 0 | 50 |
| 9 | ES + Solr isolation | 100 | 2068 | Elasticsearch | fatura itiraz | 19.15 | 14 | 78 | 16 | 21 | 78 | 20 | 0 | 100 |
| 9 | ES + Solr isolation | 100 | 2068 | Elasticsearch | müşteri bilgileri | 13.45 | 11 | 35 | 12 | 14 | 35 | 20 | 0 | 100 |
| 9 | ES + Solr isolation | 100 | 2068 | Elasticsearch | ödeme durumu | 14.55 | 11 | 40 | 12 | 23 | 40 | 20 | 0 | 100 |
| 9 | ES + Solr isolation | 100 | 2068 | Solr | fatura itiraz | 2.25 | 1 | 8 | 1 | 6 | 8 | 20 | 0 | 100 |
| 9 | ES + Solr isolation | 100 | 2068 | Solr | müşteri bilgileri | 2.90 | 1 | 16 | 2 | 5 | 16 | 20 | 0 | 100 |
| 9 | ES + Solr isolation | 100 | 2068 | Solr | ödeme durumu | 1.45 | 1 | 4 | 1 | 3 | 4 | 20 | 0 | 100 |

### Tablo Üzerinden Kısa Okuma

Bu tabloya göre:

1. Elasticsearch tüm RAW_XML testlerinde genel olarak kararlı çalışmıştır.
2. Solr, başarılı tüm senaryolarda en düşük latency değerlerini üretmiştir.
3. OpenSearch küçük ve orta ölçekli testlerde çalışsa da 2 MB XML dokümanlarıyla yapılan 50 ve 100 dokümanlık testlerde kararlı sonuç üretmemiştir.
4. OpenSearch’ün hata verdiği testlerde `successCount=0`, `errorCount=20` değerleri görülmüştür.
5. Elasticsearch + Solr izolasyon testlerinde iki engine de 100 adet 2 MB XML dokümanı üzerinde hatasız çalışmıştır.
6. Solr’ın düşük latency değerleri temel relevance kontrolünde de Elasticsearch ile uyumlu top-5 sonuçlar üretmesiyle desteklenmiştir.

---

## 13. Temel Relevance Kontrolü

RAW_XML benchmark sonuçlarında Solr’ın Elasticsearch’e göre belirgin biçimde daha düşük latency değerleri üretmesi nedeniyle, performans farkının arama sonucunun anlamlılığından kopuk olup olmadığını kontrol etmek için temel bir relevance sanity check yapılmıştır.

Bu kontrol, production seviyesinde kapsamlı bir relevance değerlendirmesi değildir. Amaç, Elasticsearch ve Solr’ın aynı sorgular için benzer üst sonuçları döndürüp döndürmediğini görmektir.

Test koşulu:

```text
Doküman sayısı: 100
XML boyutu: 2068 KB / doküman
Arama modu: RAW_XML
Response tipi: Metadata-only
Karşılaştırılan engine’ler: Elasticsearch, Solr
Limit: 5
```

Test edilen sorgular:

```text
fatura itiraz
müşteri bilgileri
ödeme durumu
```

### 13.1. `fatura itiraz` Sorgusu

| Sıra | Elasticsearch | Solr |
|---:|---|---|
| 1 | WF_BILLING_1 | WF_BILLING_1 |
| 2 | WF_BILLING_6 | WF_BILLING_6 |
| 3 | WF_BILLING_11 | WF_BILLING_11 |
| 4 | WF_BILLING_16 | WF_BILLING_16 |
| 5 | WF_BILLING_21 | WF_BILLING_21 |

Gözlem:

```text
İki engine de aynı ilk 5 workflow sonucunu döndürmüştür.
Sonuçların tamamı Billing domain’indedir.
```

### 13.2. `müşteri bilgileri` Sorgusu

| Sıra | Elasticsearch | Solr |
|---:|---|---|
| 1 | WF_CUSTOMER_2 | WF_CUSTOMER_2 |
| 2 | WF_CUSTOMER_7 | WF_CUSTOMER_7 |
| 3 | WF_CUSTOMER_12 | WF_CUSTOMER_12 |
| 4 | WF_CUSTOMER_17 | WF_CUSTOMER_17 |
| 5 | WF_CUSTOMER_22 | WF_CUSTOMER_22 |

Gözlem:

```text
İki engine de aynı ilk 5 workflow sonucunu döndürmüştür.
Sonuçların tamamı Customer domain’indedir.
```

### 13.3. `ödeme durumu` Sorgusu

| Sıra | Elasticsearch | Solr |
|---:|---|---|
| 1 | WF_PAYMENT_5 | WF_PAYMENT_5 |
| 2 | WF_PAYMENT_10 | WF_PAYMENT_10 |
| 3 | WF_PAYMENT_15 | WF_PAYMENT_15 |
| 4 | WF_PAYMENT_20 | WF_PAYMENT_20 |
| 5 | WF_PAYMENT_25 | WF_PAYMENT_25 |

Gözlem:

```text
İki engine de aynı ilk 5 workflow sonucunu döndürmüştür.
Sonuçların tamamı Payment domain’indedir.
```

### 13.4. Değerlendirme

Temel relevance kontrolünde Elasticsearch ve Solr, üç sorgu için de aynı top-5 workflow listesini döndürmüştür.

Bu sonuç, Solr’ın RAW_XML metadata-only benchmark testlerinde ürettiği düşük latency değerlerinin temel arama davranışından tamamen kopuk olmadığını göstermektedir.

Ancak bu kontrol sınırlıdır. Çünkü:

```text
Sadece 3 sorgu kullanılmıştır.
Sentetik veri seti kullanılmıştır.
Gerçek kullanıcı query logları kullanılmamıştır.
Hit@K, MRR, Precision@K gibi relevance metrikleri hesaplanmamıştır.
Score değerleri engine’ler arasında doğrudan karşılaştırılmamıştır.
```

Bu nedenle bu bölüm yalnızca temel doğrulama olarak değerlendirilmelidir.

Doğru yorum:

```text
Solr, bu RAW_XML metadata-only testlerinde yalnızca düşük latency üretmekle kalmamış, temel relevance kontrolünde de Elasticsearch ile uyumlu üst sonuçlar döndürmüştür.
Buna rağmen nihai relevance değerlendirmesi için daha geniş query seti ve beklenen sonuç listesiyle sistematik ölçüm yapılmalıdır.
```

Not:

```text
Elasticsearch ve Solr score değerleri doğrudan karşılaştırılmamalıdır.
Her search engine kendi scoring mekanizmasını ve ölçeğini kullanır.
Bu nedenle bu kontrolde score değerleri değil, top-5 workflowCode uyumu dikkate alınmıştır.
```

## 14. Genel Değerlendirme

RAW_XML benchmark testlerinden elde edilen temel sonuçlar aşağıdaki gibidir.

1. Sistem, 10 adet 34 KB XML dokümanından 100 adet 2 MB XML dokümanına kadar kademeli olarak test edilmiştir.

2. Elasticsearch, tüm ana RAW_XML testlerinde başarılı çalışmıştır. 100 adet 2 MB XML izolasyon testinde ortalama latency yaklaşık 15.72 ms seviyesindedir.

3. Solr, tüm başarılı testlerde düşük latency üretmiştir. 100 adet 2 MB XML izolasyon testinde ortalama latency yaklaşık 2.20 ms seviyesindedir.

4. OpenSearch, küçük ve orta ölçekli testlerde çalışmasına rağmen 2 MB XML dokümanlarıyla yapılan 50 ve 100 dokümanlık RAW_XML search testlerinde kararlı davranmamıştır.

5. OpenSearch için önce reindex aşamasında `circuit_breaking_exception` gözlenmiştir. Heap 2 GB seviyesine çıkarıldıktan sonra reindex başarılı olmuştur, ancak search aşamasında hata davranışı devam etmiştir.

6. OpenSearch devre dışı bırakılarak yapılan izolasyon testlerinde Elasticsearch ve Solr daha temiz sonuç üretmiştir.

7. Local ortamda swap kullanımının yüksek olması nedeniyle heap değerleri 3 GB seviyesine çıkarılmamıştır. Daha yüksek heap ayarları, test ortamı kaynak baskısını artırarak benchmark sonuçlarını kirletebilir.

8. Temel relevance kontrolünde Elasticsearch ve Solr, `fatura itiraz`, `müşteri bilgileri` ve `ödeme durumu` sorgularında aynı top-5 workflow listesini döndürmüştür. Bu sonuç, Solr’ın düşük latency değerlerinin temel sonuç kalitesinden kopuk olmadığını göstermektedir.
---

## 15. İlk Teknik Sonuç

Bu aşamadaki RAW_XML benchmark sonuçlarına göre:

```text
Elasticsearch, mevcut sistem baseline’ı olarak büyük XML dokümanları üzerinde kararlı çalışmaktadır.
Solr, aynı RAW_XML senaryosunda düşük latency değerleriyle güçlü bir alternatif olarak görünmektedir.
OpenSearch ise mevcut local kaynak ve heap ayarları altında 2 MB ham XML dokümanlarıyla yapılan daha büyük testlerde kararlı sonuç üretmemiştir.
```

Ancak bu sonuçlar production kararı olarak değerlendirilmemelidir.

Çünkü:

```text
Testler local single-node Docker ortamında yapılmıştır.
Gerçek production cluster yapısı kullanılmamıştır.
Response metadata-only’dır.
Gerçek kullanıcı query logları kullanılmamıştır.
Full XML response maliyeti ölçülmemiştir.
Indexleme bulk API ile optimize edilmemiştir.
```

Bu nedenle sonuçlar şu şekilde konumlandırılmalıdır:

```text
Bu sonuçlar, benchmark altyapısının çalıştığını ve RAW_XML senaryosunda ilk karşılaştırmalı teknik gözlemleri göstermektedir.
Nihai karar için production-like ortam, daha gerçekçi query seti ve full XML response senaryoları ayrıca test edilmelidir.
```

---

## 16. Sonraki Adımlar

Bu rapordan sonra önerilen geliştirme adımları:

1. README dosyasına RAW_XML benchmark sonuçlarının kısa özetini eklemek.
2. OpenSearch circuit breaker ve heap gözlemini teknik not olarak README’ye geçirmek.
3. Benchmark response modeline `lastErrorMessage` alanı eklemek.
4. Hatalı measurement iteration’larda exception mesajını kaydetmek.
5. EXTRACTED_DOCUMENT modunu aynı veri setlerinde sınırlı şekilde test etmek.
6. Search-only response ile full XML response senaryolarını ayrı ayrı ölçmek.
7. Benchmark sonuçlarını CSV/JSON olarak export eden basit bir çıktı mekanizması eklemek.
8. Büyük veri setleri için Elasticsearch/OpenSearch `_bulk` ve Solr batch indexing optimizasyonunu değerlendirmek.

---

## 17. Kısa Özet

Bu çalışma kapsamında RAW_XML modunda Elasticsearch, OpenSearch ve Solr üzerinde kademeli benchmark testleri yapılmıştır.

En önemli bulgular:

```text
100 adet yaklaşık 2 MB XML dokümanı, PostgreSQL üzerinde başarıyla üretilmiştir.
Elasticsearch ve Solr bu veri setinde RAW_XML search işlemini hatasız tamamlamıştır.
OpenSearch, 2 GB heap ile 50 ve 100 dokümanlık 2 MB RAW_XML testlerinde kararlı sonuç üretmemiştir.
Solr, metadata-only RAW_XML aramalarda en düşük latency değerlerini üretmiştir.
Elasticsearch, baseline olarak kararlı ve kabul edilebilir latency değerleri göstermiştir.
Temel relevance kontrolünde Elasticsearch ve Solr üç sorguda da aynı top-5 workflow sonuçlarını döndürmüştür.
```

Bu aşamada proje, RAW_XML benchmark senaryosu için çalışan ve raporlanabilir bir PoC seviyesine ulaşmıştır.