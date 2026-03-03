# LLM-Based Database Data Discovery System

PostgreSQL veritabanlarındaki verilerin LLM (Large Language Model) kullanılarak otomatik keşfi ve PII (Personally Identifiable Information) sınıflandırması yapan bir sistem.

## 🚀 Özellikler

- **Metadata Extraction**: PostgreSQL veritabanlarına bağlanarak tablo ve kolon bilgilerini otomatik çıkarma
- **PII Classification**: LLM kullanarak 12 farklı PII kategorisinde veri sınıflandırma
- **RESTful API**: FastAPI ile geliştirilmiş 6 endpoint
- **Docker Support**: Docker Compose ile tek komutla çalıştırma
- **Basic Authentication**: HTTP Basic Auth ile güvenlik

## 📋 PII Kategorileri

| # | Kategori | Açıklama |
|---|----------|----------|
| 1 | Email Address | Kişisel/iş e-posta adresleri |
| 2 | Phone Number | Mobil, sabit hat, uluslararası telefon numaraları |
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

## 🛠 Technology Stack

- **Backend**: Python 3.11, FastAPI
- **Database**: PostgreSQL 16
- **ORM**: SQLAlchemy 2.0
- **LLM**: OpenAI Compatible API
- **Containerization**: Docker & Docker Compose
- **Authentication**: HTTP Basic Auth + JWT

## 📁 Proje Yapısı

```
kafein-CaseStudy/
├── app/
│   ├── __init__.py
│   ├── main.py                  # FastAPI entry point
│   ├── config.py                # Settings & env configuration
│   ├── database.py              # DB engine & session management
│   ├── auth.py                  # Authentication logic
│   ├── models.py                # SQLAlchemy models
│   ├── schemas.py               # Pydantic schemas
│   ├── routers/
│   │   ├── auth.py              # /auth endpoint
│   │   ├── metadata.py          # Metadata CRUD endpoints
│   │   └── classify.py          # /classify endpoint
│   └── services/
│       ├── metadata_service.py  # Metadata extraction logic
│       ├── classify_service.py  # Classification orchestration
│       └── llm_service.py       # OpenAI API client
├── demo_db/
│   └── init.sql                 # Demo database init script
├── docker-compose.yml
├── Dockerfile
├── requirements.txt
├── .env.example
├── .env
└── README.md
```

## ⚡ Kurulum ve Çalıştırma

### Ön Gereksinimler

- Docker & Docker Compose
- OpenAI API Key (veya OpenAI-compatible API)

### 1. Ortam Değişkenlerini Yapılandırma

`.env.example` dosyasını `.env` olarak kopyalayın ve gerekli değerleri doldurun:

```bash
cp .env.example .env
```

`.env` dosyasını düzenleyin ve **OPENAI_API_KEY** değerini kendi API key'iniz ile değiştirin:

```env
# OpenAI Compatible API Configuration
OPENAI_API_KEY=sk-your-actual-api-key-here
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4o-mini
```

### 2. Docker Compose ile Çalıştırma

```bash
docker-compose up --build -d
```

Bu komut 3 container başlatır:
- **llm-discovery-app** — FastAPI uygulaması (port 8000)
- **llm-discovery-system-db** — Sistem veritabanı (port 5433)
- **llm-discovery-demo-db** — Demo veritabanı (port 5434)

### 3. Kontrol

```bash
# Container durumlarını kontrol edin
docker-compose ps

# Log'ları görüntüleyin
docker-compose logs -f app
```

Uygulama başlatıldıktan sonra:
- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc
- **Health Check**: http://localhost:8000/

## 📡 API Endpoints

### 1. Authentication
```bash
# Authenticate and get token
curl -X POST http://localhost:8000/auth \
  -u admin:admin123
```

### 2. Extract Database Metadata
```bash
# Connect to demo database and extract metadata
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

### 3. List Stored Metadata
```bash
curl -X GET http://localhost:8000/metadata \
  -u admin:admin123
```

### 4. Get Metadata Details
```bash
curl -X GET http://localhost:8000/metadata/{metadata_id} \
  -u admin:admin123
```

### 5. Classify Column Data
```bash
# Replace {column_id} with actual column UUID
curl -X POST http://localhost:8000/classify \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "column_id": "{column_id}",
    "sample_count": 10
  }'
```

### 6. Delete Metadata
```bash
curl -X DELETE http://localhost:8000/metadata/{metadata_id} \
  -u admin:admin123
```

## 🔄 Tipik Kullanım Akışı

1. **Auth**: `POST /auth` ile kimlik doğrulama yapın
2. **Metadata Çıkarma**: `POST /db/metadata` ile demo veritabanına bağlanın
3. **Metadata Listeleme**: `GET /metadata` ile kaydedilen metadata'ları listeleyin
4. **Metadata Detay**: `GET /metadata/{id}` ile tablo ve kolon bilgilerine erişin
5. **Sınıflandırma**: `POST /classify` ile bir kolonun PII sınıflandırmasını yapın
6. **Temizlik**: `DELETE /metadata/{id}` ile metadata'yı silin

## 🔧 Geliştirme

Docker olmadan yerel olarak çalıştırmak için:

```bash
# Sanal ortam oluşturun
python -m venv venv
source venv/bin/activate  # Linux/Mac
# veya
.\venv\Scripts\activate   # Windows

# Bağımlılıkları yükleyin
pip install -r requirements.txt

# Uygulamayı çalıştırın
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

> **Not**: Yerel çalıştırma için PostgreSQL veritabanlarının çalışır durumda olması ve `.env` dosyasındaki bağlantı bilgilerinin doğru yapılandırılması gerekir.

## 🧪 Unit Testler

Testler `pytest` ile çalışır, harici DB bağlantısı gerektirmez (mock kullanılır).

```bash
pip install pytest pytest-mock
pytest
```

| Test Dosyası | Kapsam | Test Sayısı |
|-------------|--------|-------------|
| `tests/test_auth.py` | `verify_credentials()`, `create_access_token()` | 9 |
| `tests/test_llm_service.py` | Prompt building, 13 PII kategorisi, TCKN | 7 |
| `tests/test_metadata_service.py` | Credential masking, connection string | 5 |

## 🛑 Durdurma

```bash
docker-compose down

# Volume'ları da silmek için
docker-compose down -v
```
