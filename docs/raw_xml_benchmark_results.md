# XML Workflow Search Benchmark — Final Benchmark Raporu

## 1. Amaç

Bu rapor, XML Workflow Search Benchmark projesi kapsamında yapılan benchmark testlerini ve teknik değerlendirmeleri özetler.

Projenin amacı, PostgreSQL üzerinde tutulan büyük XML workflow dokümanlarında Elasticsearch, OpenSearch ve Apache Solr servislerinin full-text search performansını karşılaştırmaktır.

Çalışmada üç temel konu incelenmiştir:

1. Büyük XML dokümanlarında search engine performansı
2. `RAW_XML` ve `EXTRACTED_DOCUMENT` indexleme stratejilerinin etkisi
3. `METADATA_ONLY` ve `FULL_XML_RESPONSE` response modlarının latency üzerindeki etkisi

## 2. Test Ortamı

| Bileşen | Değer |
|---|---|
| Uygulama | Spring Boot API |
| Veri deposu | PostgreSQL |
| Search engine’ler | Elasticsearch, OpenSearch, Apache Solr |
| Çalışma ortamı | Lokal single-node Docker Compose |
| Veri tipi | Sentetik XML workflow dokümanları |
| Ana büyük XML boyutu | Yaklaşık 2068 KB |
| Büyük test doküman sayısı | 100 |
| Büyük test warm-up | 10 |
| Büyük test measurement | 50 |
| Ana query seti | 5 sorgu |
| Ana response tipi | Metadata-only |

Bu sonuçlar production kararı olarak değerlendirilmemelidir. Testler lokal single-node Docker ortamında, sentetik veriyle ve sınırlı query setiyle yapılmıştır.

## 3. Veri Seti

XML boyutu `screenCount` parametresiyle büyütülmüştür. `screenCount` arttıkça XML içindeki screen, field, action, validation ve transition blokları artar.

| screenCount | Yaklaşık XML Boyutu |
|---:|---:|
| 20 | 34 KB |
| 300 | 515 KB |
| 600 | 1032 KB |
| 1200 | 2068 KB |

Final büyük testler için kullanılan ana veri seti:

| Parametre | Değer |
|---|---:|
| Workflow sayısı | 100 |
| Tekil XML boyutu | Yaklaşık 2068 KB |
| Toplam ham XML hacmi | Yaklaşık 200 MB |
| Query sayısı | 5 |
| Limit | 5 |
| Warm-up iteration | 10 |
| Measurement iteration | 50 |

## 4. Arama Modları

### 4.1 RAW_XML

`RAW_XML`, mevcut sisteme en yakın senaryodur. XML dokümanı parse edilmeden search engine’e aktarılır. Arama doğrudan ham XML içeriğinin indexlenmiş hali üzerinde yapılır.

Arama alanları:

| Engine | Arama Alanı |
|---|---|
| Elasticsearch | `xmlContent` |
| OpenSearch | `xmlContent` |
| Solr | `xmlContent_txt` |

Bu yaklaşımda XML her sorguda baştan sona lineer taranmaz. Search engine indexleme sırasında XML içeriğini tokenize eder ve inverted index oluşturur.

### 4.2 EXTRACTED_DOCUMENT

`EXTRACTED_DOCUMENT`, XML’in parse edilerek normalize bir search document haline getirildiği alternatif yaklaşımdır.

Çıkarılan temel alanlar:

```text
workflowName
screenTitles
screenDescriptions
actionTexts
technicalTokens
searchText
```

Bu yaklaşımın amacı, ham XML içeriği yerine daha anlamlı alanlar üzerinde arama yapmaktır. Ancak mevcut testlerde bu yaklaşım genel performans avantajı sağlamamıştır.

## 5. Response Modları

### 5.1 METADATA_ONLY

Search sonucunda yalnızca küçük metadata alanları döndürülür:

```text
id
workflowCode
workflowName
status
domain
xmlSizeKb
score
```

Bu mod search engine latency’sini ölçmek için kullanılmıştır.

### 5.2 FULL_XML_RESPONSE

Search sonucunda metadata alanlarına ek olarak XML içeriği de response’a dahil edilir. Bu mod, büyük payload taşıma maliyetini ölçmek için kullanılmıştır.

Final testlerde `limit=5` olduğu için full response modunda yaklaşık olarak şu payload oluşmuştur:

```text
5 sonuç x yaklaşık 2 MB XML ≈ 10 MB response payload
```

## 6. Ölçüm Metrikleri

| Metrik | Açıklama |
|---|---|
| `avgMs` | Başarılı ölçümlerin ortalama süresi |
| `minMs` | En düşük başarılı ölçüm |
| `maxMs` | En yüksek başarılı ölçüm |
| `p50Ms` | Median latency |
| `p95Ms` | 95. yüzdelik latency |
| `p99Ms` | 99. yüzdelik latency |
| `successCount` | Başarılı measurement sayısı |
| `errorCount` | Hatalı measurement sayısı |
| `lastHitCount` | Son başarılı sorgudaki toplam eşleşme sayısı |
| `lastResponseSizeKb` | Son başarılı response’un yaklaşık payload boyutu |

Latency ölçümleri double precision olarak tutulmuştur. Böylece özellikle Solr gibi 1 ms altına yaklaşan sonuçlarda integer milisaniye yuvarlama etkisi azaltılmıştır.

## 7. Test Matrisi

| Aşama | Dataset | Engine’ler | Amaç |
|---|---|---|---|
| Küçük test | 10 x 34 KB | Elasticsearch, OpenSearch, Solr | Smoke test ve temel entegrasyon doğrulama |
| Orta test | 50 x 515 KB | Elasticsearch, OpenSearch, Solr | Orta ölçekte RAW/XML ve extracted farkını gözlemleme |
| Büyük test | 100 x 2068 KB | Elasticsearch, Solr | Final büyük ölçek karşılaştırması |
| Payload testi | 100 x 2068 KB | Elasticsearch, Solr | Metadata-only ve full XML response maliyetini ayırma |

OpenSearch küçük ve orta testlerde değerlendirilmiştir. Büyük final testlerde lokal kaynak koşulları ve önceki bellek baskısı gözlemleri nedeniyle Elasticsearch + Solr izolasyonu tercih edilmiştir.

## 8. Küçük ve Orta Test Özeti

Küçük ve orta testler, sistemin uçtan uca doğru çalıştığını ve üç engine’in temel senaryolarda çalışabilir olduğunu doğrulamak için yapılmıştır.

| Test | Engine | RAW_XML Avg/P95 | EXTRACTED Avg/P95 | Yorum |
|---|---:|---:|---:|---|
| Küçük | Elasticsearch | 3.87 / 7.76 ms | 3.62 / 5.32 ms | Fark düşük |
| Küçük | OpenSearch | 5.94 / 9.25 ms | 5.45 / 7.95 ms | Fark düşük |
| Küçük | Solr | 1.50 / 3.04 ms | 1.43 / 2.08 ms | Solr hızlı |
| Orta | Elasticsearch | 7.76 / 15.34 ms | 13.85 / 28.56 ms | Extracted daha yavaş |
| Orta | OpenSearch | 10.79 / 14.34 ms | 13.96 / 17.59 ms | Extracted daha yavaş |
| Orta | Solr | 0.95 / 2.23 ms | 2.64 / 1.22 ms | Outlier etkisi mevcut |

Küçük testlerde extracted yaklaşım bazı motorlarda küçük avantaj göstermiştir. Ancak orta ölçekte bu avantaj kaybolmuş, özellikle Elasticsearch ve OpenSearch tarafında extracted yaklaşım daha yüksek latency üretmiştir.

## 9. Büyük Final Test Sonuçları

Final büyük testler, 100 adet yaklaşık 2 MB XML dokümanı üzerinde Elasticsearch ve Solr ile yapılmıştır.

### 9.1 RAW_XML + METADATA_ONLY

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Error |
|---|---:|---:|---:|---:|---:|
| Elasticsearch | 12.66 ms | 11.70 ms | 19.92 ms | 27.14 ms | 0 |
| Solr | 1.12 ms | 1.04 ms | 1.79 ms | 2.44 ms | 0 |

Yorum:

- Solr, metadata-only RAW_XML aramada Elasticsearch’e göre belirgin biçimde daha düşük latency üretmiştir.
- Elasticsearch daha yüksek latency üretmiş fakat hatasız ve kararlı çalışmıştır.
- İki engine de 5 sorgu x 50 measurement = 250 başarılı ölçüm üretmiş, hata üretmemiştir.

### 9.2 EXTRACTED_DOCUMENT + METADATA_ONLY

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Error |
|---|---:|---:|---:|---:|---:|
| Elasticsearch | 27.83 ms | 20.59 ms | 49.77 ms | 71.02 ms | 0 |
| Solr | 1.20 ms | 0.98 ms | 2.11 ms | 3.92 ms | 0 |

Yorum:

- Elasticsearch tarafında `EXTRACTED_DOCUMENT`, `RAW_XML` yaklaşımına göre belirgin şekilde daha yavaş çalışmıştır.
- Solr tarafında extracted yaklaşım RAW_XML’e yakın değerler üretmiş ancak daha iyi sonuç vermemiştir.
- Bu haliyle extracted yaklaşım performans avantajı sağlamamıştır.

### 9.3 RAW_XML + FULL_XML_RESPONSE

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Ortalama Payload |
|---|---:|---:|---:|---:|---:|
| Elasticsearch | 76.08 ms | 75.14 ms | 88.07 ms | 107.59 ms | ~10340 KB |
| Solr | 66.32 ms | 63.99 ms | 75.97 ms | 87.80 ms | ~10340 KB |

Yorum:

- Full XML response modunda iki engine’in latency değerleri ciddi şekilde artmıştır.
- Solr metadata-only senaryoda yaklaşık 1 ms seviyesindeyken, full XML response senaryosunda yaklaşık 66 ms seviyesine çıkmıştır.
- Bu sonuç, büyük XML içeriğinin search response içinde döndürülmesinin search latency ölçümünü baskıladığını göstermektedir.
- Search endpointinin metadata-only çalışması, XML detayının ayrı endpoint üzerinden alınması daha doğru mimaridir.

## 10. RAW_XML ve EXTRACTED_DOCUMENT Karşılaştırması

Final büyük testte iki yaklaşımın ortalama sonuçları:

| Mod | Engine | Ortalama Avg | Ortalama P95 | Sonuç |
|---|---:|---:|---:|---|
| RAW_XML | Elasticsearch | 12.66 ms | 19.92 ms | Daha iyi |
| EXTRACTED_DOCUMENT | Elasticsearch | 27.83 ms | 49.77 ms | Daha yavaş |
| RAW_XML | Solr | 1.12 ms | 1.79 ms | Daha iyi |
| EXTRACTED_DOCUMENT | Solr | 1.20 ms | 2.11 ms | Yakın fakat daha iyi değil |

Teknik yorum:

`EXTRACTED_DOCUMENT` yaklaşımında XML parse edilerek daha anlamlı alanlara ayrılmıştır. Ancak mevcut veri üretim yapısı ve query setinde aranan terimler birçok dokümanda ve birçok alanda tekrar ettiği için bu yaklaşım arama uzayını yeterince daraltmamıştır. Ayrıca çok alanlı arama, field boost ve skor birleştirme maliyeti oluşturmuştur. Bu nedenle parse edilmiş veri yapısı beklenen performans avantajını üretmemiştir.

`RAW_XML` yaklaşımı ise tek büyük text field üzerinde inverted index kullandığı için daha sade ve kararlı bir baseline sunmuştur.

## 11. Metadata-only ve Full XML Response Karşılaştırması

| Engine | Metadata-only Avg | Full XML Avg | Artış |
|---|---:|---:|---:|
| Elasticsearch | 12.66 ms | 76.08 ms | Yaklaşık 6.0x |
| Solr | 1.12 ms | 66.32 ms | Yaklaşık 59.2x |

Bu karşılaştırma, büyük XML payload taşımanın latency üzerindeki etkisini açık şekilde göstermektedir.

Özellikle Solr tarafında search işlemi metadata-only durumda çok hızlıdır. Ancak full XML response senaryosunda latency’nin büyük bölümü response serialization, transfer ve parse maliyetinden kaynaklanır.

Önerilen mimari:

```text
Search endpoint:
- workflowCode
- workflowName
- domain
- status
- score
- xmlSizeKb

Detail endpoint:
- seçilen workflow için full XML content
```

## 12. OpenSearch Değerlendirmesi

OpenSearch küçük ve orta testlerde çalışmıştır. Ancak büyük XML yüklerinde lokal kaynak koşulları altında ana final testlere dahil edilmemiştir.

Bu kararın nedeni:

- OpenSearch’ün Elasticsearch’e benzer JVM tabanlı çalışma modeli nedeniyle lokal Docker ortamında ek bellek baskısı oluşturması
- Büyük XML testlerinde önceki denemelerde kararsızlık gözlenmesi
- Final karşılaştırmada Elasticsearch baseline ile Solr alternatifinin izole şekilde ölçülmek istenmesi

Bu sonuç OpenSearch’ün production ortamında uygun olmadığı anlamına gelmez. Sadece bu lokal PoC ortamında büyük XML yükü altında final ölçüm için tercih edilmemiştir.

## 13. Relevance Açısından İlk Değerlendirme

Bu çalışma öncelikle latency benchmark çalışmasıdır. Sistematik relevance metriği hesaplanmamıştır.

Ancak temel kontrol amacıyla aynı query’lerde dönen sonuçlar incelenmiştir. Sentetik veri setinde domain bazlı sorgular beklenen workflow gruplarını üst sıralara getirmiştir.

| Sorgu | Beklenen Domain |
|---|---|
| fatura itiraz | Billing |
| müşteri bilgileri | Customer |
| ödeme durumu | Payment |
| abonelik iptal | Subscription |
| arıza kaydı | Technical Support |

Bu kontrol, düşük latency değerlerinin tamamen anlamsız sonuçlardan kaynaklanmadığını göstermektedir. Ancak production seviyesi karar için gerçek query logları ve beklenen sonuç listeleriyle precision/recall veya NDCG gibi relevance metrikleri ayrıca ölçülmelidir.

## 14. Sınırlılıklar

Bu benchmark aşağıdaki sınırlılıklara sahiptir:

- Testler lokal single-node Docker ortamında yapılmıştır.
- Veri seti sentetiktir.
- Query seti sınırlıdır.
- Aynı query’lerin tekrar edilmesi cache etkisi oluşturabilir.
- Production cluster, shard/replica, gerçek trafik ve gerçek kullanıcı query logları test edilmemiştir.
- Relevance ölçümü sistematik metriklerle yapılmamıştır.
- Network gecikmesi, distributed cluster davranışı ve production resource isolation kapsam dışıdır.

Bu nedenle sonuçlar production kararı değil, PoC seviyesinde teknik yön gösterici benchmark sonucu olarak değerlendirilmelidir.

## 15. Genel Sonuç

Bu çalışma sonucunda XML workflow dokümanları üzerinde Elasticsearch, OpenSearch ve Apache Solr için tekrar edilebilir bir benchmark altyapısı geliştirilmiştir.

Elde edilen temel sonuçlar:

1. Apache Solr, metadata-only RAW_XML aramada en düşük latency değerlerini üretmiştir.
2. Elasticsearch, Solr’a göre daha yavaş olmakla birlikte kararlı baseline olarak çalışmıştır.
3. OpenSearch küçük ve orta testlerde gözlemlenmiş; büyük final benchmarkta lokal kaynak koşulları nedeniyle dışarıda bırakılmıştır.
4. RAW_XML yaklaşımı, mevcut sistem davranışına yakın ve kararlı bir baseline sağlamıştır.
5. EXTRACTED_DOCUMENT yaklaşımı mevcut haliyle genel performans avantajı sağlamamıştır.
6. FULL_XML_RESPONSE, latency değerlerini ciddi şekilde artırmıştır.
7. Search endpointinin metadata-only çalışması ve XML detayının ayrı endpoint ile alınması önerilmektedir.

## 16. Sonraki Adımlar

İleride yapılabilecek çalışmalar:

1. Gerçek production query loglarına yakın query seti oluşturmak
2. Sistematik relevance metriği eklemek
3. Bulk indexing optimizasyonu yapmak
4. Production-like cluster ortamında testleri tekrarlamak
5. OpenSearch’ü daha yüksek heap ve izole kaynaklarla yeniden değerlendirmek
6. EXTRACTED_DOCUMENT alanlarını daha seçici hale getirmek
7. `searchText` alanını küçültüp field-specific query senaryolarını ayrıca test etmek
8. XML detail endpoint tasarımıyla metadata-only search mimarisini tamamlamak

## 17. Final Teknik Karar

Bu PoC kapsamında en güçlü teknik öneri şudur:

```text
Arama endpointi metadata-only çalışmalı.
Büyük XML içeriği search response içinde döndürülmemeli.
Solr, metadata-only RAW_XML arama için güçlü bir alternatif olarak değerlendirilmelidir.
Elasticsearch mevcut sistem baseline’ı olarak korunabilir.
EXTRACTED_DOCUMENT yaklaşımı performans için değil, ileride relevance ve kontrol edilebilirlik amacıyla yeniden tasarlanarak ele alınmalıdır.
```
