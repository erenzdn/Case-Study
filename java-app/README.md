# LLM-Based Database Data Discovery System — Java/Spring Boot (Task 2)

Task 1 (Python/FastAPI) projesinin Java/Spring Boot'a AI destekli dönüşümü.  
Dönüşüm süreci ve kullanılan promptlar için: [`TASK2_AI_CONVERSION.md`](../TASK2_AI_CONVERSION.md)

## 🛠 Technology Stack

| Katman | Teknoloji |
|--------|-----------|
| Framework | Spring Boot 3.2.3 |
| Language | Java 17 |
| ORM | Spring Data JPA + Hibernate |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| HTTP Client | Spring WebFlux WebClient |
| Database | PostgreSQL 16 + HikariCP |
| API Docs | springdoc-openapi (Swagger UI) |
| Build | Maven 3.9 |
| Container | Docker + Docker Compose |

## 📁 Proje Yapısı

```
java-app/
├── src/main/java/com/kafein/discovery/
│   ├── DiscoveryApplication.java        # Spring Boot entry point
│   ├── config/
│   │   ├── SecurityConfig.java          # Spring Security + JWT generation
│   │   ├── JwtAuthenticationFilter.java # Bearer token validation filter
│   │   ├── OpenAIConfig.java            # WebClient for OpenAI API
│   │   ├── OpenApiConfig.java           # Swagger UI configuration
│   │   ├── RequestLoggingFilter.java    # Request/response logging middleware
│   │   └── GlobalExceptionHandler.java  # @RestControllerAdvice error handler
│   ├── entity/
│   │   ├── DatabaseMetadata.java        # Target DB connection + tables
│   │   ├── TableMetadata.java           # Table info + columns
│   │   ├── ColumnMetadata.java          # Column info + classifications
│   │   └── ClassificationResult.java   # LLM classification output
│   ├── repository/
│   │   ├── DatabaseMetadataRepository.java
│   │   └── ColumnMetadataRepository.java
│   ├── dto/
│   │   ├── request/DBConnectionRequest.java
│   │   ├── request/ClassifyRequest.java
│   │   ├── response/AuthResponse.java
│   │   ├── response/MetadataListResponse.java
│   │   ├── response/MetadataDetailResponse.java
│   │   └── response/ClassifyResponse.java
│   ├── service/
│   │   ├── MetadataService.java         # JDBC-based DB introspection
│   │   ├── ClassifyService.java         # Classification orchestration
│   │   └── LlmService.java             # OpenAI API client + prompt engineering
│   └── controller/
│       ├── AuthController.java          # POST /auth
│       ├── MetadataController.java      # GET|POST|DELETE /metadata
│       ├── ClassifyController.java      # POST /classify
│       └── HealthController.java        # GET /
├── src/main/resources/
│   └── application.yml                  # Spring Boot configuration
├── Dockerfile                           # Multi-stage build (JDK → JRE)
├── docker-compose.yml                   # 3-container setup
├── .env.example                         # Environment variable template
└── pom.xml                              # Maven dependencies
```

## ⚡ Kurulum ve Çalıştırma

### Ön Gereksinimler

- Docker & Docker Compose
- OpenAI API Key (veya OpenAI-compatible API) — `/classify` endpoint'i için

### 1. Ortam Değişkenlerini Yapılandırma

```bash
cp .env.example .env
```

`.env` dosyasını açıp `OPENAI_API_KEY` değerini kendi key'inizle değiştirin:

```env
OPENAI_API_KEY=sk-your-actual-api-key-here
```

### 2. Docker Compose ile Başlatma

```bash
docker compose up --build -d
```

3 container başlar:
- **llm-discovery-java-app** — Spring Boot API (port **8000**)
- **llm-discovery-java-system-db** — Sistem veritabanı (port 5433)
- **llm-discovery-java-demo-db** — Demo veritabanı (port 5434)

### 3. Doğrulama

```bash
# Sağlık kontrolü
curl http://localhost:8000/

# Container durumları
docker compose ps
```

- **Swagger UI**: http://localhost:8000/swagger-ui/index.html
- **API Docs**: http://localhost:8000/v3/api-docs

## 🔐 Authentication Akışı

```
1. POST /auth  (Basic Auth: admin / admin123)  →  access_token (JWT) alırsın
2. Swagger'da 🔒 Authorize → bearerAuth → "Bearer <token>" gir
3. Tüm korumalı endpoint'leri kullanabilirsin
4. Token 60 dakika geçerli — süre dolunca /auth'dan yeni token al
```

## 📡 API Endpoints

| Method | Endpoint | Auth | Açıklama |
|--------|----------|------|----------|
| `GET` | `/` | Public | Health check |
| `POST` | `/auth` | Basic Auth | JWT token al |
| `POST` | `/db/metadata` | Bearer JWT | Hedef DB'yi keşfet ve kaydet |
| `GET` | `/metadata` | Bearer JWT | Kayıtlı metadata'ları listele |
| `GET` | `/metadata/{id}` | Bearer JWT | Metadata detayı (tablo + kolon) |
| `DELETE` | `/metadata/{id}` | Bearer JWT | Metadata sil (cascade) |
| `POST` | `/classify` | Bearer JWT | Kolon verisini LLM ile sınıflandır |

### Örnek: Metadata Çıkarma

```bash
# 1. Token al
TOKEN=$(curl -s -X POST http://localhost:8000/auth \
  -u admin:admin123 | jq -r '.access_token')

# 2. Demo DB metadata'sını çek
curl -X POST http://localhost:8000/db/metadata \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "host": "demo_db",
    "port": 5432,
    "database": "demo_database",
    "username": "postgres",
    "password": "postgres"
  }'
```

### Örnek: PII Sınıflandırma

```bash
# column_id değerini GET /metadata/{id} response'undan alın
curl -X POST http://localhost:8000/classify \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "column_id": "your-column-uuid-here",
    "sample_count": 10
  }'
```

## 🛑 Durdurma

```bash
docker compose down

# Volume'ları da silmek için
docker compose down -v
```

## 📖 AI Dönüşüm Dokümantasyonu

Python → Java dönüşümünde kullanılan AI prompts, CLAUDE.md ve SKİLLS.md kullanımı, teknoloji eşleştirmesi ve karşılaşılan zorluklar için:  
📄 [`../TASK2_AI_CONVERSION.md`](../TASK2_AI_CONVERSION.md)
