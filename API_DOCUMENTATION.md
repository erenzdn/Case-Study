# API Documentation — LLM-Based Database Data Discovery System

## Base URL

```
http://localhost:8000
```

## Authentication

Tüm endpoint'ler (health check hariç) **HTTP Basic Auth** ile korunmaktadır.

| Kullanıcı Adı | Şifre |
|----------------|-------|
| `admin` | `admin123` |

Swagger UI'da sağ üstteki **Authorize** butonuna tıklayarak giriş yapabilirsiniz.

---

## Endpoints

---

### 1. `GET /` — Health Check

**Amaç:** Uygulamanın çalışıp çalışmadığını kontrol etmek.

**Auth:** ❌ Gerekmiyor

**Request:**
```bash
curl http://localhost:8000/
```

**Response (200):**
```json
{
  "status": "healthy",
  "service": "LLM-Based Database Data Discovery System",
  "version": "1.0.0"
}
```

---

### 2. `POST /auth` — Kimlik Doğrulama

**Amaç:** Kullanıcıyı doğrulayıp JWT token döndürmek. Diğer tüm endpoint'leri kullanmadan önce buradan giriş yapılmalıdır.

**Auth:** ✅ Basic Auth (username + password)

**Request:**
```bash
curl -X POST http://localhost:8000/auth \
  -u admin:admin123
```

**Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

**Hata Durumları:**

| Kod | Durum | Açıklama |
|-----|-------|----------|
| 401 | Unauthorized | Yanlış kullanıcı adı veya şifre |

---

### 3. `POST /db/metadata` — Veritabanı Metadata Çıkarma & Saklama

**Amaç:** Bir PostgreSQL veritabanına bağlanarak tüm tablo ve kolon bilgilerini otomatik keşfetmek ve system veritabanına kaydetmek.

**Auth:** ✅ Basic Auth

**Nasıl Çalışır:**
1. Gönderdiğiniz bağlantı bilgileriyle hedef PostgreSQL'e bağlanır
2. `information_schema` sorguları ile tüm tabloları ve kolonları çeker
3. Her kolona benzersiz bir UUID atar
4. Tüm bilgileri system veritabanına kaydeder

**Request:**
```bash
curl -X POST http://localhost:8000/db/metadata \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "host": "demo_db",
    "port": 5432,
    "database": "demo_database",
    "username": "postgres",
    "password": "postgres"
  }'
```

**Request Body:**

| Alan | Tip | Zorunlu | Açıklama |
|------|-----|---------|----------|
| `host` | string | ✅ | Veritabanı sunucu adresi |
| `port` | integer | ❌ | Port (varsayılan: 5432) |
| `database` | string | ✅ | Veritabanı adı |
| `username` | string | ✅ | Veritabanı kullanıcı adı |
| `password` | string | ✅ | Veritabanı şifresi |

**Response (201 Created):**
```json
{
  "metadata_id": "8937e1f9-950b-41b2-b3c4-981b0e803290",
  "database_name": "demo_database",
  "table_count": 8,
  "tables": [
    {
      "table_id": "da615ffe-db5b-409f-b588-ebd7dcd7f44d",
      "table_name": "customers",
      "schema_name": "public",
      "columns": [
        {
          "column_id": "ef66ce53-4a58-49e8-9490-d2b26342a7c5",
          "column_name": "first_name",
          "data_type": "character varying",
          "is_nullable": "NO",
          "ordinal_position": 3
        }
      ]
    }
  ]
}
```

**Hata Durumları:**

| Kod | Durum | Açıklama |
|-----|-------|----------|
| 400 | Bad Request | Veritabanına bağlanılamadı veya metadata çıkarılamadı |
| 401 | Unauthorized | Kimlik doğrulama başarısız |

---

### 4. `GET /metadata` — Metadata Listeleme

**Amaç:** Daha önce taranan tüm veritabanlarının özet listesini görmek.

**Auth:** ✅ Basic Auth

**Request:**
```bash
curl -X GET http://localhost:8000/metadata \
  -u admin:admin123
```

**Response (200):**
```json
[
  {
    "metadata_id": "8937e1f9-950b-41b2-b3c4-981b0e803290",
    "database_name": "demo_database",
    "created_at": "2026-03-01T14:30:39.959293",
    "table_count": 8
  }
]
```

**Response Alanları:**

| Alan | Tip | Açıklama |
|------|-----|----------|
| `metadata_id` | UUID | Tarama kaydının benzersiz ID'si |
| `database_name` | string | Taranan veritabanının adı |
| `created_at` | datetime | Tarama tarihi ve saati |
| `table_count` | integer | Bulunan tablo sayısı |

---

### 5. `GET /metadata/{metadata_id}` — Metadata Detay

**Amaç:** Belirli bir tarama kaydının tüm detayını görmek — tablolar, kolonlar, veri tipleri ve kolon UUID'leri.

> **Önemli:** `/classify` endpoint'ini kullanmak için gereken `column_id` değerlerini bu endpoint'ten alırsınız.

**Auth:** ✅ Basic Auth

**Request:**
```bash
curl -X GET http://localhost:8000/metadata/8937e1f9-950b-41b2-b3c4-981b0e803290 \
  -u admin:admin123
```

**Response (200):**
```json
{
  "metadata_id": "8937e1f9-950b-41b2-b3c4-981b0e803290",
  "database_name": "demo_database",
  "created_at": "2026-03-01T14:30:39.959293",
  "tables": [
    {
      "table_id": "7920a769-1805-4cad-801f-17719e5aa407",
      "table_name": "customers",
      "schema_name": "public",
      "columns": [
        {
          "column_id": "cc234500-1c58-4b2b-9823-169e46335f72",
          "column_name": "email",
          "data_type": "character varying",
          "is_nullable": "NO",
          "ordinal_position": 5
        },
        {
          "column_id": "ea48e17a-a47a-4a55-b9d0-72ecd6cc07fa",
          "column_name": "phone_number",
          "data_type": "character varying",
          "is_nullable": "YES",
          "ordinal_position": 6
        }
      ]
    }
  ]
}
```

**Hata Durumları:**

| Kod | Durum | Açıklama |
|-----|-------|----------|
| 404 | Not Found | Belirtilen metadata_id bulunamadı |

---

### 6. `DELETE /metadata/{metadata_id}` — Metadata Silme

**Amaç:** Bir tarama kaydını ve ilişkili tüm verileri (tablolar, kolonlar, sınıflandırma sonuçları) silmek.

**Auth:** ✅ Basic Auth

**Request:**
```bash
curl -X DELETE http://localhost:8000/metadata/8937e1f9-950b-41b2-b3c4-981b0e803290 \
  -u admin:admin123
```

**Response (200):**
```json
{
  "message": "Metadata 8937e1f9-950b-41b2-b3c4-981b0e803290 deleted successfully",
  "status": "success"
}
```

**Hata Durumları:**

| Kod | Durum | Açıklama |
|-----|-------|----------|
| 404 | Not Found | Belirtilen metadata_id bulunamadı |

---

### 7. `POST /classify` — LLM ile PII Sınıflandırma

**Amaç:** Bir kolondaki verilerin kişisel veri (PII) olup olmadığını LLM kullanarak analiz edip, olasılık dağılımı döndürmek.

> **Not:** Bu endpoint çalışması için `.env` dosyasında geçerli bir `OPENAI_API_KEY` olmalıdır.

**Auth:** ✅ Basic Auth

**Nasıl Çalışır:**
1. `column_id` ile kolon, tablo ve veritabanı bilgilerine ulaşır
2. Hedef veritabanına bağlanıp belirtilen sayıda örnek veri çeker
3. Kolon adı + veri tipi + örnek verileri LLM'e gönderir
4. LLM her PII kategorisi için 0.0 - 1.0 arası olasılık döndürür

**Request:**
```bash
curl -X POST http://localhost:8000/classify \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "column_id": "cc234500-1c58-4b2b-9823-169e46335f72",
    "sample_count": 10
  }'
```

**Request Body:**

| Alan | Tip | Zorunlu | Varsayılan | Açıklama |
|------|-----|---------|------------|----------|
| `column_id` | UUID | ✅ | — | Sınıflandırılacak kolonun ID'si (`GET /metadata/{id}`'den alınır) |
| `sample_count` | integer | ❌ | 10 | Çekilecek örnek veri sayısı (1-100) |

**Response (200):**
```json
{
  "column_id": "cc234500-1c58-4b2b-9823-169e46335f72",
  "column_name": "email",
  "table_name": "customers",
  "database_name": "demo_database",
  "sample_count": 5,
  "samples": [
    "ahmet.yilmaz@email.com",
    "fatma.kaya@gmail.com",
    "mehmet.demir@hotmail.com",
    "ayse.sahin@yahoo.com",
    "can.ozkan@outlook.com"
  ],
  "classifications": [
    {"category": "Email Address", "probability": 0.95},
    {"category": "Not PII", "probability": 0.02},
    {"category": "Full Name", "probability": 0.01},
    {"category": "Phone Number", "probability": 0.01},
    {"category": "First Name", "probability": 0.01}
  ]
}
```

**PII Kategorileri:**

| # | Kategori | Açıklama |
|---|----------|----------|
| 1 | Email Address | Kişisel/iş e-posta adresleri |
| 2 | Phone Number | Mobil, sabit hat, uluslararası |
| 3 | Social Security Number (SSN) | ABD Sosyal Güvenlik Numarası |
| 4 | Credit Card Number | Ödeme kartı numaraları |
| 5 | National ID Number | Pasaport, ehliyet numaraları |
| 6 | Full Name | Tam ad-soyad |
| 7 | First Name | Sadece isim |
| 8 | Last Name / Surname | Sadece soyisim |
| 9 | TCKN (Turkish Citizenship Number) | TC Kimlik Numarası (11 haneli) |
| 10 | Home Address | Ev adresi |
| 11 | Date of Birth | Doğum tarihi |
| 12 | IP Address | Internet Protocol adresi |
| 13 | Not PII | Kişisel veri içermeyen veriler |

**Hata Durumları:**

| Kod | Durum | Açıklama |
|-----|-------|----------|
| 404 | Not Found | Belirtilen column_id bulunamadı |
| 500 | Internal Server Error | LLM API bağlantı hatası veya geçersiz API key |

---

## Kullanım Akışı

```
1. POST /auth          → Giriş yap (JWT token al)
2. POST /db/metadata   → Hedef veritabanını tara
3. GET  /metadata      → Taramaları listele
4. GET  /metadata/{id} → Kolon ID'lerini al
5. POST /classify      → İstediğin kolonu LLM ile sınıflandır
6. DELETE /metadata/{id} → Temizle (opsiyonel)
```

## Swagger UI

Tüm endpoint'leri interaktif olarak test etmek için: **http://localhost:8000/docs**
