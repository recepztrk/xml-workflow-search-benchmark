# RAW_XML Benchmark Sonuçları
Bu doküman, XML Workflow Search Benchmark projesinde yapılan RAW_XML benchmark denemelerini test bazlı olarak özetler. Amaç; PostgreSQL üzerinde tutulan büyük XML workflow dokümanlarında Elasticsearch, OpenSearch ve Apache Solr servislerinin arama davranışını kontrollü biçimde karşılaştırmaktır.
RAW_XML modu, mevcut sisteme en yakın senaryodur. XML parse edilmeden search engine’e aktarılır ve arama ham XML içeriği üzerinde yapılır.
> Bu sonuçlar production kararı değildir. Testler lokal single-node Docker ortamında, sentetik XML verisiyle ve metadata-only response ile yapılmıştır.
## 1. Test Ortamı ve Kapsam
| Bileşen | Değer |
|---|---|
| Uygulama | Spring Boot API |
| Veri deposu | PostgreSQL |
| Search engine’ler | Elasticsearch, OpenSearch, Apache Solr |
| Test modu | RAW_XML |
| Response tipi | Metadata-only |
| Küçük testler | 5 warm-up, 20 measurement |
| Ölçek doğrulama testleri | 10 warm-up, 100 measurement |
| Büyük XML boyutu | screenCount=1200, yaklaşık 2068 KB |
## 2. Veri Boyutu Üretimi
XML boyutu, sentetik workflow generator içindeki `screenCount` parametresi artırılarak büyütülmüştür. Bu işlem yalnızca metadata değerini değiştirmez; XML içine daha fazla screen, field, action, validation ve transition bloğu yazıldığı için `xmlContent` gerçek anlamda büyür.
| screenCount | Yaklaşık XML Boyutu |
|---:|---:|
| 20 | 34 KB |
| 300 | 515 KB |
| 600 | 1032 KB |
| 1200 | 2068 KB |
## 3. Metrikler
| Metrik | Açıklama |
|---|---|
| Avg | Başarılı ölçümlerin ortalama süresi |
| Min | En düşük başarılı ölçüm süresi |
| Max | En yüksek başarılı ölçüm süresi |
| P50 | Median latency |
| P95 | 95. yüzdelik latency |
| P99 | 99. yüzdelik latency |
| Success | Başarılı measurement sayısı |
| Error | Hatalı measurement sayısı |
| Hit | Son başarılı sorguda bulunan toplam eşleşme sayısı |
`samplesMs` ham ölçüm listesi rapor okunabilirliği için tablolara eklenmemiştir. Gerektiğinde API response çıktılarından veya ileride CSV/JSON export çıktılarından izlenmelidir.
## 4. Test Bazlı Detaylı Sonuçlar
### Test 1 — 10 doküman x 34 KB — tüm engine’ler
- Kapsam: `All engines`
- Doküman sayısı: `10`
- Tekil XML boyutu: `34 KB`
- Warm-up: `5`
- Measurement: `20`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 3.10 | 3 | 5 | 3 | 3 | 5 | 20 | 0 | 10 |
  | Elasticsearch | müşteri bilgileri | 3.05 | 3 | 4 | 3 | 3 | 4 | 20 | 0 | 10 |
  | Elasticsearch | ödeme durumu | 2.65 | 2 | 4 | 3 | 3 | 4 | 20 | 0 | 10 |
  | OpenSearch | fatura itiraz | 9.95 | 3 | 108 | 4 | 11 | 108 | 20 | 0 | 10 |
  | OpenSearch | müşteri bilgileri | 3.30 | 2 | 6 | 3 | 5 | 6 | 20 | 0 | 10 |
  | OpenSearch | ödeme durumu | 3.55 | 3 | 6 | 3 | 6 | 6 | 20 | 0 | 10 |
  | Solr | fatura itiraz | 1.25 | 1 | 2 | 1 | 2 | 2 | 20 | 0 | 10 |
  | Solr | müşteri bilgileri | 1.20 | 1 | 3 | 1 | 2 | 3 | 20 | 0 | 10 |
  | Solr | ödeme durumu | 1.50 | 1 | 11 | 1 | 1 | 11 | 20 | 0 | 10 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 2.93 | 3.00 | 3.00 | 4.33 | 60 | 0 |
| OpenSearch | 5.60 | 3.33 | 7.33 | 40.00 | 60 | 0 |
| Solr | 1.32 | 1.00 | 1.67 | 5.33 | 60 | 0 |

Kısa yorum: Smoke test niteliğindedir. Üç engine de hatasız çalışmış, ancak küçük veri seti nedeniyle performans kararı için tek başına yeterli değildir.

### Test 2 — 10 doküman x 515 KB — tüm engine’ler
- Kapsam: `All engines`
- Doküman sayısı: `10`
- Tekil XML boyutu: `515 KB`
- Warm-up: `5`
- Measurement: `20`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 5.20 | 4 | 10 | 5 | 6 | 10 | 20 | 0 | 10 |
  | Elasticsearch | müşteri bilgileri | 5.20 | 4 | 7 | 5 | 7 | 7 | 20 | 0 | 10 |
  | Elasticsearch | ödeme durumu | 5.30 | 5 | 6 | 5 | 6 | 6 | 20 | 0 | 10 |
  | OpenSearch | fatura itiraz | 20.95 | 9 | 151 | 14 | 18 | 151 | 20 | 0 | 10 |
  | OpenSearch | müşteri bilgileri | 7.70 | 7 | 10 | 7 | 10 | 10 | 20 | 0 | 10 |
  | OpenSearch | ödeme durumu | 7.70 | 6 | 10 | 7 | 10 | 10 | 20 | 0 | 10 |
  | Solr | fatura itiraz | 1.00 | 1 | 1 | 1 | 1 | 1 | 20 | 0 | 10 |
  | Solr | müşteri bilgileri | 1.00 | 1 | 1 | 1 | 1 | 1 | 20 | 0 | 10 |
  | Solr | ödeme durumu | 0.95 | 0 | 1 | 1 | 1 | 1 | 20 | 0 | 10 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 5.23 | 5.00 | 6.33 | 7.67 | 60 | 0 |
| OpenSearch | 12.12 | 9.33 | 12.67 | 57.00 | 60 | 0 |
| Solr | 0.98 | 1.00 | 1.00 | 1.00 | 60 | 0 |

Kısa yorum: XML boyutu artırıldığında Elasticsearch ve OpenSearch tarafında latency artışı gözlenmiştir. OpenSearch’te fatura itiraz sorgusunda yüksek outlier görülmüştür.

### Test 3 — 100 doküman x 515 KB — tüm engine’ler
- Kapsam: `All engines`
- Doküman sayısı: `100`
- Tekil XML boyutu: `515 KB`
- Warm-up: `5`
- Measurement: `20`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 6.30 | 5 | 14 | 6 | 10 | 14 | 20 | 0 | 100 |
  | Elasticsearch | müşteri bilgileri | 8.60 | 5 | 41 | 6 | 11 | 41 | 20 | 0 | 100 |
  | Elasticsearch | ödeme durumu | 4.90 | 4 | 5 | 5 | 5 | 5 | 20 | 0 | 100 |
  | OpenSearch | fatura itiraz | 8.30 | 7 | 11 | 8 | 11 | 11 | 20 | 0 | 100 |
  | OpenSearch | müşteri bilgileri | 7.95 | 6 | 11 | 7 | 10 | 11 | 20 | 0 | 100 |
  | OpenSearch | ödeme durumu | 8.00 | 6 | 11 | 7 | 10 | 11 | 20 | 0 | 100 |
  | Solr | fatura itiraz | 0.95 | 0 | 2 | 1 | 1 | 2 | 20 | 0 | 100 |
  | Solr | müşteri bilgileri | 0.95 | 0 | 2 | 1 | 1 | 2 | 20 | 0 | 100 |
  | Solr | ödeme durumu | 0.40 | 0 | 1 | 0 | 1 | 1 | 20 | 0 | 100 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 6.60 | 5.67 | 8.67 | 20.00 | 60 | 0 |
| OpenSearch | 8.08 | 7.33 | 10.33 | 11.00 | 60 | 0 |
| Solr | 0.77 | 0.67 | 1.00 | 1.67 | 60 | 0 |

Kısa yorum: Doküman sayısı 100’e çıkarılmıştır. Üç engine de hatasız çalışmıştır. OpenSearch bu testte daha stabil görünmüştür.

### Test 4 — 10 doküman x 1032 KB — tüm engine’ler
- Kapsam: `All engines`
- Doküman sayısı: `10`
- Tekil XML boyutu: `1032 KB`
- Warm-up: `5`
- Measurement: `20`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 8.80 | 7 | 14 | 8 | 13 | 14 | 20 | 0 | 10 |
  | Elasticsearch | müşteri bilgileri | 12.20 | 8 | 53 | 9 | 20 | 53 | 20 | 0 | 10 |
  | Elasticsearch | ödeme durumu | 8.95 | 7 | 16 | 8 | 14 | 16 | 20 | 0 | 10 |
  | OpenSearch | fatura itiraz | 13.60 | 11 | 18 | 14 | 15 | 18 | 20 | 0 | 10 |
  | OpenSearch | müşteri bilgileri | 14.10 | 11 | 26 | 14 | 15 | 26 | 20 | 0 | 10 |
  | OpenSearch | ödeme durumu | 12.70 | 11 | 14 | 13 | 14 | 14 | 20 | 0 | 10 |
  | Solr | fatura itiraz | 0.50 | 0 | 2 | 0 | 1 | 2 | 20 | 0 | 10 |
  | Solr | müşteri bilgileri | 0.35 | 0 | 2 | 0 | 1 | 2 | 20 | 0 | 10 |
  | Solr | ödeme durumu | 0.30 | 0 | 3 | 0 | 3 | 3 | 20 | 0 | 10 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 9.98 | 8.33 | 15.67 | 27.67 | 60 | 0 |
| OpenSearch | 13.47 | 13.67 | 14.67 | 19.33 | 60 | 0 |
| Solr | 0.38 | 0.00 | 1.67 | 2.33 | 60 | 0 |

Kısa yorum: Tekil XML boyutu yaklaşık 1 MB seviyesine çıkarılmıştır. Solr’da 0 ms ölçümleri görülmeye başlamıştır; bu milisaniye ölçüm çözünürlüğünün sınırıdır.

### Test 5 — 10 doküman x 2068 KB — tüm engine’ler
- Kapsam: `All engines`
- Doküman sayısı: `10`
- Tekil XML boyutu: `2068 KB`
- Warm-up: `5`
- Measurement: `20`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 16.90 | 12 | 69 | 13 | 26 | 69 | 20 | 0 | 10 |
  | Elasticsearch | müşteri bilgileri | 12.50 | 11 | 25 | 11 | 20 | 25 | 20 | 0 | 10 |
  | Elasticsearch | ödeme durumu | 12.25 | 11 | 20 | 11 | 19 | 20 | 20 | 0 | 10 |
  | OpenSearch | fatura itiraz | 24.45 | 24 | 30 | 24 | 25 | 30 | 20 | 0 | 10 |
  | OpenSearch | müşteri bilgileri | 25.40 | 25 | 28 | 25 | 26 | 28 | 20 | 0 | 10 |
  | OpenSearch | ödeme durumu | 25.25 | 24 | 26 | 25 | 26 | 26 | 20 | 0 | 10 |
  | Solr | fatura itiraz | 1.05 | 1 | 2 | 1 | 1 | 2 | 20 | 0 | 10 |
  | Solr | müşteri bilgileri | 1.20 | 1 | 3 | 1 | 2 | 3 | 20 | 0 | 10 |
  | Solr | ödeme durumu | 0.05 | 0 | 1 | 0 | 0 | 1 | 20 | 0 | 10 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 13.88 | 11.67 | 21.67 | 38.00 | 60 | 0 |
| OpenSearch | 25.03 | 24.67 | 25.67 | 28.00 | 60 | 0 |
| Solr | 0.77 | 0.67 | 1.00 | 2.00 | 60 | 0 |

Kısa yorum: Yaklaşık 2 MB XML boyutu ilk kez doğrulanmıştır. Üç engine de 10 dokümanda hatasız çalışmıştır.

### Test 6 — 100 doküman x 2068 KB — tüm engine’ler
- Kapsam: `All engines`
- Doküman sayısı: `100`
- Tekil XML boyutu: `2068 KB`
- Warm-up: `5`
- Measurement: `20`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 19.15 | 17 | 22 | 19 | 21 | 22 | 20 | 0 | 100 |
  | Elasticsearch | müşteri bilgileri | 13.45 | 13 | 15 | 13 | 15 | 15 | 20 | 0 | 100 |
  | Elasticsearch | ödeme durumu | 14.05 | 12 | 36 | 13 | 15 | 36 | 20 | 0 | 100 |
  | OpenSearch | fatura itiraz | 52.30 | 22 | 230 | 32 | 116 | 230 | 20 | 0 | 100 |
  | OpenSearch | müşteri bilgileri | 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 20 | 0 |
  | OpenSearch | ödeme durumu | 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 20 | 0 |
  | Solr | fatura itiraz | 3.25 | 1 | 12 | 3 | 5 | 12 | 20 | 0 | 100 |
  | Solr | müşteri bilgileri | 1.90 | 1 | 3 | 2 | 3 | 3 | 20 | 0 | 100 |
  | Solr | ödeme durumu | 1.60 | 1 | 3 | 1 | 3 | 3 | 20 | 0 | 100 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 15.55 | 15.00 | 17.00 | 24.33 | 60 | 0 |
| OpenSearch | 17.43 | 10.67 | 38.67 | 76.67 | 20 | 40 |
| Solr | 2.25 | 2.00 | 3.67 | 6.00 | 60 | 0 |

Kısa yorum: 100 adet yaklaşık 2 MB XML üzerinde Elasticsearch ve Solr hatasız çalışmıştır. OpenSearch yalnızca ilk sorguda başarılı olmuş; sonraki iki sorguda hata üretmiştir.

### Test 7 — 50 doküman x 2068 KB — tüm engine’ler
- Kapsam: `All engines`
- Doküman sayısı: `50`
- Tekil XML boyutu: `2068 KB`
- Warm-up: `5`
- Measurement: `20`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 18.15 | 16 | 29 | 17 | 25 | 29 | 20 | 0 | 50 |
  | Elasticsearch | müşteri bilgileri | 14.95 | 12 | 40 | 13 | 16 | 40 | 20 | 0 | 50 |
  | Elasticsearch | ödeme durumu | 13.65 | 12 | 22 | 13 | 18 | 22 | 20 | 0 | 50 |
  | OpenSearch | fatura itiraz | 49.53 | 21 | 169 | 48 | 169 | 169 | 19 | 1 | 50 |
  | OpenSearch | müşteri bilgileri | 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 20 | 0 |
  | OpenSearch | ödeme durumu | 0.00 | 0 | 0 | 0 | 0 | 0 | 0 | 20 | 0 |
  | Solr | fatura itiraz | 15.20 | 1 | 81 | 5 | 64 | 81 | 20 | 0 | 50 |
  | Solr | müşteri bilgileri | 2.00 | 1 | 5 | 2 | 4 | 5 | 20 | 0 | 50 |
  | Solr | ödeme durumu | 1.85 | 1 | 5 | 2 | 3 | 5 | 20 | 0 | 50 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 15.58 | 14.33 | 19.67 | 30.33 | 60 | 0 |
| OpenSearch | 16.51 | 16.00 | 56.33 | 56.33 | 19 | 41 |
| Solr | 6.35 | 3.00 | 23.67 | 30.33 | 60 | 0 |

Kısa yorum: 50 adet 2 MB XML seviyesinde de OpenSearch kararsızdır. Solr ilk sorguda outlier üretmiştir; bu nedenle izolasyon testi yapılmıştır.

### Test 8 — 50 doküman x 2068 KB — Elasticsearch + Solr izolasyon
- Kapsam: `ES + Solr isolation`
- Doküman sayısı: `50`
- Tekil XML boyutu: `2068 KB`
- Warm-up: `5`
- Measurement: `20`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 19.80 | 15 | 75 | 17 | 20 | 75 | 20 | 0 | 50 |
  | Elasticsearch | müşteri bilgileri | 14.10 | 12 | 27 | 13 | 16 | 27 | 20 | 0 | 50 |
  | Elasticsearch | ödeme durumu | 12.75 | 11 | 20 | 12 | 17 | 20 | 20 | 0 | 50 |
  | Solr | fatura itiraz | 3.65 | 1 | 11 | 3 | 11 | 11 | 20 | 0 | 50 |
  | Solr | müşteri bilgileri | 3.40 | 1 | 15 | 2 | 10 | 15 | 20 | 0 | 50 |
  | Solr | ödeme durumu | 2.20 | 1 | 3 | 2 | 3 | 3 | 20 | 0 | 50 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 15.55 | 14.00 | 17.67 | 40.67 | 60 | 0 |
| Solr | 3.08 | 2.33 | 8.00 | 9.67 | 60 | 0 |

Kısa yorum: OpenSearch devre dışı bırakıldığında Solr’daki outlier değerleri belirgin şekilde azalmıştır.

### Test 9 — 100 doküman x 2068 KB — Elasticsearch + Solr izolasyon
- Kapsam: `ES + Solr isolation`
- Doküman sayısı: `100`
- Tekil XML boyutu: `2068 KB`
- Warm-up: `5`
- Measurement: `20`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 19.15 | 14 | 78 | 16 | 21 | 78 | 20 | 0 | 100 |
  | Elasticsearch | müşteri bilgileri | 13.45 | 11 | 35 | 12 | 14 | 35 | 20 | 0 | 100 |
  | Elasticsearch | ödeme durumu | 14.55 | 11 | 40 | 12 | 23 | 40 | 20 | 0 | 100 |
  | Solr | fatura itiraz | 2.25 | 1 | 8 | 1 | 6 | 8 | 20 | 0 | 100 |
  | Solr | müşteri bilgileri | 2.90 | 1 | 16 | 2 | 5 | 16 | 20 | 0 | 100 |
  | Solr | ödeme durumu | 1.45 | 1 | 4 | 1 | 3 | 4 | 20 | 0 | 100 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 15.72 | 13.33 | 19.33 | 51.00 | 60 | 0 |
| Solr | 2.20 | 1.33 | 4.67 | 9.33 | 60 | 0 |

Kısa yorum: Ana 100 doküman x 2 MB izolasyon testinde Elasticsearch ve Solr 60/60 başarılıdır. Solr daha düşük latency üretmiştir.

### Test 10 — 100 doküman x 2068 KB — Elasticsearch + Solr, 100 measurement
- Kapsam: `ES + Solr isolation`
- Doküman sayısı: `100`
- Tekil XML boyutu: `2068 KB`
- Warm-up: `10`
- Measurement: `100`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 13.26 | 11 | 29 | 12 | 17 | 20 | 100 | 0 | 100 |
  | Elasticsearch | müşteri bilgileri | 11.69 | 10 | 18 | 11 | 14 | 16 | 100 | 0 | 100 |
  | Elasticsearch | ödeme durumu | 12.16 | 11 | 29 | 12 | 15 | 19 | 100 | 0 | 100 |
  | Solr | fatura itiraz | 0.99 | 0 | 4 | 1 | 2 | 3 | 100 | 0 | 100 |
  | Solr | müşteri bilgileri | 1.10 | 0 | 6 | 1 | 3 | 5 | 100 | 0 | 100 |
  | Solr | ödeme durumu | 0.61 | 0 | 2 | 1 | 1 | 2 | 100 | 0 | 100 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 12.37 | 11.67 | 15.33 | 18.33 | 300 | 0 |
| Solr | 0.90 | 1.00 | 2.00 | 3.33 | 300 | 0 |

Kısa yorum: 20 measurement sonucunu doğrulamak için 100 measurement ile tekrar test edilmiştir. Sonuç yönü değişmemiştir.

### Test 11 — 250 doküman x 2068 KB — Elasticsearch + Solr, 100 measurement
- Kapsam: `ES + Solr isolation`
- Doküman sayısı: `250`
- Tekil XML boyutu: `2068 KB`
- Warm-up: `10`
- Measurement: `100`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 13.88 | 11 | 43 | 12 | 21 | 29 | 100 | 0 | 250 |
  | Elasticsearch | müşteri bilgileri | 11.27 | 9 | 21 | 11 | 13 | 18 | 100 | 0 | 250 |
  | Elasticsearch | ödeme durumu | 11.41 | 10 | 29 | 11 | 14 | 21 | 100 | 0 | 250 |
  | Solr | fatura itiraz | 0.40 | 0 | 2 | 0 | 1 | 1 | 100 | 0 | 250 |
  | Solr | müşteri bilgileri | 0.24 | 0 | 3 | 0 | 1 | 2 | 100 | 0 | 250 |
  | Solr | ödeme durumu | 0.14 | 0 | 1 | 0 | 1 | 1 | 100 | 0 | 250 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 12.19 | 11.33 | 16.00 | 22.67 | 300 | 0 |
| Solr | 0.26 | 0.00 | 1.00 | 1.33 | 300 | 0 |

Kısa yorum: Yaklaşık 517 MB ham XML hacmi altında Elasticsearch ve Solr hatasız çalışmıştır. Solr değerleri milisaniye çözünürlüğünün altına yaklaşmıştır.

### Test 12 — 500 doküman x 2068 KB — Elasticsearch + Solr, 100 measurement
- Kapsam: `ES + Solr isolation`
- Doküman sayısı: `500`
- Tekil XML boyutu: `2068 KB`
- Warm-up: `10`
- Measurement: `100`
  | Engine | Query | Avg | Min | Max | P50 | P95 | P99 | Success | Error | Hit |
  |---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
  | Elasticsearch | fatura itiraz | 14.08 | 10 | 64 | 12 | 19 | 44 | 100 | 0 | 500 |
  | Elasticsearch | müşteri bilgileri | 12.18 | 9 | 33 | 11 | 18 | 24 | 100 | 0 | 500 |
  | Elasticsearch | ödeme durumu | 11.41 | 9 | 22 | 11 | 17 | 20 | 100 | 0 | 500 |
  | Solr | fatura itiraz | 0.07 | 0 | 1 | 0 | 1 | 1 | 100 | 0 | 500 |
  | Solr | müşteri bilgileri | 0.01 | 0 | 1 | 0 | 0 | 0 | 100 | 0 | 500 |
  | Solr | ödeme durumu | 0.06 | 0 | 3 | 0 | 0 | 1 | 100 | 0 | 500 |

Özet tablo:

| Engine | Ortalama Avg | Ortalama P50 | Ortalama P95 | Ortalama P99 | Başarı | Hata |
|---|---:|---:|---:|---:|---:|---:|
| Elasticsearch | 12.56 | 11.33 | 18.00 | 29.33 | 300 | 0 |
| Solr | 0.05 | 0.00 | 0.33 | 0.67 | 300 | 0 |

Kısa yorum: Yaklaşık 1 GB ham XML hacmi altında iki engine de hatasız çalışmıştır. Solr çok hızlı görünmekle birlikte çok sayıda 0 ms değeri, ölçüm çözünürlüğü sınırını göstermektedir.

## 5. Temel Relevance Kontrolü
Solr’ın düşük latency değerlerinin arama sonucunun anlamlılığından kopuk olup olmadığını görmek için Elasticsearch ve Solr, 100 adet yaklaşık 2 MB XML dokümanı üzerinde aynı üç sorgu ile karşılaştırılmıştır. Üç sorguda da iki engine aynı top-5 workflow listesini döndürmüştür.
| Sorgu | Elasticsearch Top-5 | Solr Top-5 |
|---|---|---|
| fatura itiraz | WF_BILLING_1, WF_BILLING_6, WF_BILLING_11, WF_BILLING_16, WF_BILLING_21 | WF_BILLING_1, WF_BILLING_6, WF_BILLING_11, WF_BILLING_16, WF_BILLING_21 |
| müşteri bilgileri | WF_CUSTOMER_2, WF_CUSTOMER_7, WF_CUSTOMER_12, WF_CUSTOMER_17, WF_CUSTOMER_22 | WF_CUSTOMER_2, WF_CUSTOMER_7, WF_CUSTOMER_12, WF_CUSTOMER_17, WF_CUSTOMER_22 |
| ödeme durumu | WF_PAYMENT_5, WF_PAYMENT_10, WF_PAYMENT_15, WF_PAYMENT_20, WF_PAYMENT_25 | WF_PAYMENT_5, WF_PAYMENT_10, WF_PAYMENT_15, WF_PAYMENT_20, WF_PAYMENT_25 |

Bu kontrol production seviyesinde relevance değerlendirmesi değildir. Ancak Solr’ın düşük latency değerlerinin temel sonuç kalitesinden tamamen kopuk olmadığını gösterir.
## 6. Genel Değerlendirme
- Elasticsearch, küçük ve büyük tüm RAW_XML testlerinde kararlı baseline davranışı göstermiştir. 500 adet yaklaşık 2 MB XML dokümanı üzerinde de 300/300 başarılı ölçüm alınmıştır.
- Solr, tüm başarılı senaryolarda Elasticsearch’e göre belirgin biçimde daha düşük latency üretmiştir. 100/250/500 dokümanlık ölçek doğrulama testlerinde hata üretmemiştir.
- Solr sonuçlarında 250 ve 500 dokümanlık testlerde çok sayıda 0 ms ölçüm görülmüştür. Bu, Solr’ın gerçekten sıfır sürede çalıştığı anlamına gelmez; ölçüm milisaniye çözünürlüğünde tutulduğu için 1 ms altındaki süreler 0 olarak görünmektedir. Bu nedenle Solr performansı yorumlanırken ölçüm çözünürlüğü sınırı açıkça belirtilmelidir.
- Son testlerde aynı sorguların 100 kez tekrarlanması, JVM/JIT ısınması, OS filesystem cache, Lucene segmentlerinin bellekte sıcak hale gelmesi ve Solr query/result cache etkisi yaratmış olabilir. Bu nedenle 100/250/500 dokümanlık son testler warm-cache repeated-query benchmark olarak değerlendirilmelidir.
- OpenSearch küçük ve orta ölçekli testlerde çalışmıştır; ancak yaklaşık 2 MB XML dokümanlarıyla yapılan 50 ve 100 dokümanlık RAW_XML testlerinde kararlı sonuç üretmemiştir. 1 GB heap ile reindex sırasında circuit breaker görülmüş, 2 GB heap ile reindex başarılı olsa da search aşamasındaki hata davranışı devam etmiştir.
- RAW_XML yaklaşımı mevcut sistemi temsil etmek için doğru başlangıçtır; ancak uzun vadede XML’in parse edilerek daha anlamlı EXTRACTED_DOCUMENT yapısıyla indexlenmesi relevance ve kontrol edilebilirlik açısından ayrıca test edilmelidir.
## 7. Sonuç
Bu aşamada proje, RAW_XML metadata-only benchmark senaryosu için çalışan ve raporlanabilir bir PoC seviyesine ulaşmıştır. Elasticsearch kararlı baseline olarak korunabilir; Solr bu problem özelinde güçlü ve düşük latency üreten bir alternatif olarak öne çıkmaktadır; OpenSearch ise mevcut lokal kaynak ve heap ayarları altında büyük RAW_XML yüklerinde riskli görünmektedir. Nihai karar için production-like cluster ortamı, gerçek query logları, full XML response maliyeti ve sistematik relevance ölçümleri gereklidir.
## 8. Sonraki Adımlar
1. EXTRACTED_DOCUMENT modunu 100/250/500 dokümanlık seçili veri setlerinde test etmek.
2. Full XML response ile metadata-only response maliyetini ayırmak.
3. CSV/JSON benchmark export mekanizması eklemek.
4. Daha gerçekçi ve çeşitli query seti oluşturmak.
5. Relevance metrikleri için beklenen sonuç listesi hazırlamak.
6. Bulk indexing optimizasyonunu değerlendirmek.
7. Production-like cluster ortamında doğrulama yapmak.
