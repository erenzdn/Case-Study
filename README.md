# LLM-Based Database Data Discovery System

PostgreSQL veritabanlarındaki verilerin LLM (Large Language Model) kullanılarak otomatik keşfi ve PII (Personally Identifiable Information) sınıflandırması.

Bu proje iki ayrı implementasyon içerir:

| | Task 1 | Task 2 |
|--|--------|--------|
| **Dil / Framework** | Python 3.11 / FastAPI | Java 17 / Spring Boot 3.2 |
| **Dizin** | `./` (kök dizin) | `./java-app/` |
| **Dokümantasyon** | Bu dosya | [java-app/README.md](java-app/README.md) |
| **AI Dönüşüm Süreci** | — | [TASK2_AI_CONVERSION.md](TASK2_AI_CONVERSION.md) |

---

## 📋 PII Kategorileri (12 + Not PII)

| # | Kategori | Açıklama |
|---|----------|----------|
| 1 | Email Address | Kişisel/iş e-posta adresleri |
| 2 | Phone Number | Mobil, sabit hat, uluslararası |
| 3 | SSN | ABD Sosyal Güvenlik Numarası |
| 4 | Credit Card Number | Ödeme kartı numaraları |
| 5 | National ID Number | Pasaport, ehliyet vb. |
| 6 | Full Name | Tam ad-soyad |
| 7 | First Name | Sadece isim |
| 8 | Last Name / Surname | Sadece soyisim |
| 9 | TCKN | TC Kimlik Numarası (11 haneli) |
| 10 | Home Address | Ev adresi |
| 11 | Date of Birth | Doğum tarihi |
| 12 | IP Address | Internet Protocol adresi |
| 13 | Not PII | Kişisel veri içermeyen veriler |

---

## Task 1 — Python / FastAPI

### 🛠 Technology Stack

| Katman | Teknoloji |
|--------|-----------|
| Backend Framework | Python 3.11, FastAPI |
| ORM | SQLAlchemy 2.0 |
| Database | PostgreSQL 16 |
| Authentication | HTTP Basic Auth + JWT (python-jose) |
| LLM Integration | OpenAI Compatible API (httpx) |
| Containerization | Docker & Docker Compose |

### 📁 Proje Yapısı

```
kafein-CaseStudy/
├── app/
│   ├── main.py                  # FastAPI entry point + middleware
│   ├── config.py                # Pydantic-settings (reads .env)
│   ├── database.py              # SQLAlchemy engine & session
│   ├── auth.py                  # Basic Auth + JWT creation
│   ├── models.py                # ORM models (4 tables)
│   ├── schemas.py               # Pydantic request/response models
│   ├── routers/
│   │   ├── auth.py              # POST /auth
│   │   ├── metadata.py          # GET|POST|DELETE /metadata
│   │   └── classify.py          # POST /classify
│   └── services/
│       ├── metadata_service.py  # PostgreSQL introspection
│       ├── classify_service.py  # Classification orchestration
│       └── llm_service.py       # OpenAI API + prompt engineering
├── demo_db/
│   └── init.sql                 # Demo DB initialization script
├── tests/                       # pytest unit tests
├── java-app/                    # Task 2 — Java/Spring Boot
├── docker-compose.yml
├── Dockerfile
├── requirements.txt
├── .env.example                 # Environment variable template
└── TASK2_AI_CONVERSION.md       # AI-assisted Java conversion docs
```

### ⚡ Kurulum ve Çalıştırma

#### Ön Gereksinimler

- Docker & Docker Compose
- OpenAI API Key (veya OpenAI-compatible API) — `/classify` için

#### Adım 1 — Ortam Değişkenlerini Yapılandırma

```bash
cp .env.example .env
```

`.env` dosyasını açıp değerleri doldurun:

| Değişken | Açıklama | Varsayılan |
|----------|----------|------------|
| `SYSTEM_DB_HOST` | System DB container adı | `system_db` |
| `SYSTEM_DB_PORT` | System DB port | `5432` |
| `SYSTEM_DB_NAME` | System DB adı | `system_database` |
| `SYSTEM_DB_USER` | System DB kullanıcısı | `postgres` |
| `SYSTEM_DB_PASSWORD` | System DB şifresi | `postgres123` |
| `AUTH_USERNAME` | API kullanıcı adı | `admin` |
| `AUTH_PASSWORD` | API şifresi | `admin123` |
| `JWT_SECRET_KEY` | JWT imzalama anahtarı (min 32 karakter) | *(değiştirin)* |
| `JWT_ALGORITHM` | JWT algoritması | `HS256` |
| `JWT_EXPIRATION_MINUTES` | Token geçerlilik süresi (dakika) | `60` |
| `OPENAI_API_KEY` | **Zorunlu** — OpenAI API anahtarı | *(kendi key'inizi girin)* |
| `OPENAI_BASE_URL` | OpenAI API endpoint | `https://api.openai.com/v1` |
| `OPENAI_MODEL` | Kullanılacak model | `gpt-4o-mini` |

#### Adım 2 — Docker Compose ile Başlatma

```bash
docker-compose up --build -d
```

3 container başlar:

| Container | Açıklama | Port |
|-----------|----------|------|
| `llm-discovery-app` | FastAPI uygulaması | **8000** |
| `llm-discovery-system-db` | Metadata depolama DB | 5433 |
| `llm-discovery-demo-db` | Keşfedilecek hedef DB | 5434 |

#### Adım 3 — Doğrulama

```bash
# Container durumu
docker-compose ps

# Health check
curl http://localhost:8000/
```

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

---

### 📡 API Endpoints — Kullanım Kılavuzu

> **Not:** Tüm örnekler `curl` ile gösterilmiştir. Swagger UI'dan da test edebilirsiniz.

#### 1. `POST /auth` — Kimlik Doğrulama

```bash
curl -X POST http://localhost:8000/auth \
  -u admin:admin123
```

**Başarılı Yanıt (200):**
```json
{ "access_token": "eyJhbGci...", "token_type": "bearer" }
```

#### 2. `POST /db/metadata` — Veritabanı Metadata Çıkarma

Demo veritabanını keşfeder, tüm tablo ve kolonları UUID ile kaydeder.

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

**Başarılı Yanıt (201):** Metadata ID, 8 tablo, 100+ kolon UUID'leri

#### 3. `GET /metadata` — Metadata Listesi

```bash
curl http://localhost:8000/metadata -u admin:admin123
```

**Yanıt:** `[{ "metadata_id", "database_name", "created_at", "table_count" }]`

#### 4. `GET /metadata/{metadata_id}` — Metadata Detayı

```bash
curl http://localhost:8000/metadata/{metadata_id} -u admin:admin123
```

**Yanıt:** Tüm tablolar, kolonlar ve `column_id`'ler (→ `/classify` için kullanılır)

#### 5. `DELETE /metadata/{metadata_id}` — Metadata Silme

```bash
curl -X DELETE http://localhost:8000/metadata/{metadata_id} -u admin:admin123
```

#### 6. `POST /classify` — LLM ile PII Sınıflandırma

`column_id`'yi `GET /metadata/{id}` yanıtından alın:

```bash
curl -X POST http://localhost:8000/classify \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "column_id": "cc234500-1c58-4b2b-9823-169e46335f72",
    "sample_count": 10
  }'
```

**Yanıt:** 13 PII kategorisinin olasılık dağılımı (toplamı 1.0)

---

### 🔄 Tipik Kullanım Senaryosu

```
1. POST /auth          → Kimlik doğrula
2. POST /db/metadata   → Demo DB'yi keşfet (8 tablo, 100+ kolon)
3. GET  /metadata      → Taramaları listele
4. GET  /metadata/{id} → column_id'leri al
5. POST /classify      → Bir kolonu LLM ile sınıflandır (OPENAI_API_KEY gerekli)
6. DELETE /metadata/{id} → Temizle (isteğe bağlı)
```

---

## Task 2 — Java / Spring Boot

Task 1'in Java/Spring Boot'a AI destekli dönüşümü.

📂 **Dizin:** [`java-app/`](java-app/)  
📖 **Kurulum:** [`java-app/README.md`](java-app/README.md)  
🤖 **AI Dönüşüm Süreci:** [`TASK2_AI_CONVERSION.md`](TASK2_AI_CONVERSION.md)

### AI Destekli Dönüşüm Özeti

Task 2, **Google Deepmind Antigravity (Gemini tabanlı)** agentic AI coding assistant kullanılarak oluşturulmuştur. Dönüşüm süreci 8 aşamadan oluşmaktadır:

| Aşama | Kapsam |
|-------|--------|
| 1 | Architecture Mapping (FastAPI → Spring Boot) |
| 2 | Project Skeleton (pom.xml, application.yml) |
| 3 | Entity Conversion (SQLAlchemy → JPA) |
| 4 | DTO Conversion (Pydantic → Java Records) |
| 5 | Security Migration (Basic Auth → Spring Security + JWT) |
| 6 | Service Conversion (metadata, classify, LLM services) |
| 7 | Controller Migration (routers → @RestController) |
| 8 | Docker Migration (multi-stage build) |

Kullanılan AI promptları, CLAUDE.md ve SKİLLS.md bağlam dosyalarının kullanımı ve karşılaşılan teknik zorluklar için: **[TASK2_AI_CONVERSION.md](TASK2_AI_CONVERSION.md)**

---

## 🧪 Unit Testler

### Task 1 — Python

```bash
pip install pytest pytest-mock
pytest
```

| Test | Kapsam |
|------|--------|
| `tests/test_auth.py` | JWT üretimi, credential doğrulama (9 test) |
| `tests/test_llm_service.py` | Prompt building, 13 PII kategorisi (7 test) |
| `tests/test_metadata_service.py` | Credential masking, connection string (5 test) |

### Task 2 — Java

```bash
cd java-app && mvn test
```

| Test | Kapsam |
|------|--------|
| `AuthControllerTest` | 200 response, bearer token, username delegation (4 test) |
| `LlmServiceTest` | 13 PII kategorisi, prompt, probability normalization (7 test) |
| `JwtAuthenticationFilterTest` | Valid/expired/invalid token, filter chain (7 test) |

---

## 🛑 Durdurma

### Task 1
```bash
docker-compose down          # Container'ları durdur
docker-compose down -v       # Volume'ları da sil
```

### Task 2
```bash
cd java-app
docker compose down
docker compose down -v
```
