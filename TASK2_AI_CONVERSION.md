# TASK2_AI_CONVERSION.md — Python → Java: AI-Assisted Conversion Documentation

## 1. Overview

This document explains all steps, methods and prompts applied to convert the Python/FastAPI project (Task 1) to a Java/Spring Boot project (Task 2) using AI/LLM assistance.

**AI Tool Used:** Google Deepmind Antigravity (Gemini-based agentic coding assistant)  
**Approach:** Component-by-component conversion with validation at each step

---

## 2. Context Documents: CLAUDE.md & SKİLLS.md

Before any conversion prompt was written, two project-specific context files were provided to the AI as **system-level instructions**. These files established the persona, principles, and technical standards that every generated file must conform to.

### CLAUDE.md — Project Persona & Rules
```
@[CLAUDE.md] @[SKİLLS.md]

Role: Senior Software Architect with 10+ years of experience in Backend Engineering.
Principles: Strictly adhere to Clean Code, SOLID, and DRY principles.
Objective: Deliver production-ready, highly secure, and well-documented code.
Security First: Prioritize PII data protection and secure credential management.

Code Style:
- Java: Use Spring Boot 3.x, Jakarta EE, and Lombok. Standard Maven project structure.
- Naming: Follow camelCase for Java.
- LLM Integration: All LLM calls must support OpenAI-compatible API format.
- Security: Use Basic Auth for all endpoints. Connection strings from .env.

Key Logic:
- Metadata ID: Generate unique UUIDs for each metadata record and column.
- Classification: Default sample count is 10. Return probability distributions for PII classes.
- PII Categories: Strictly follow the 12 categories in the case study.
- Task 2 Transition: Map FastAPI logic to Spring Boot Controllers/Services.
```

### SKİLLS.md — Technical Implementation Strategies
```
Java Migration & Spring Ecosystem:
- Architectural Mapping: Convert FastAPI Routers to Spring @RestControllers,
  Pydantic models to Lombok-annotated POJOs or Java Records.
- Dependency Injection: Use @Service and @Repository layers to mirror Python modules.
- Async Operations: Use WebClient for non-blocking OpenAI API calls.
- Auth Transition: FastAPI Security → Spring Security Filter Chain for Basic Auth.

Database & Security:
- Introspection: Use PostgreSQL information_schema for dynamic metadata extraction.
- Persistent Identity: Every column MUST have a unique UUID primary key.
- Security: Database credentials must NEVER be logged in plain text.

Containerization:
- Multi-Stage Builds: JDK for Maven build → JRE-only for runtime (reduces image size).
- Orchestration: docker-compose with System DB, Backend API, and Demo DB.
```

### How These Files Were Used

These documents were **attached at the start of every AI session** using the `@[CLAUDE.md] @[SKİLLS.md]` mention syntax. This ensured the AI:

1. Adopted the **Senior Software Architect** persona for all decisions
2. Applied **SOLID/DRY/Clean Code** principles automatically
3. Enforced **security-first** design (credential masking, timing-safe auth, @JsonIgnore)
4. Chose the correct **Java equivalents** for Python patterns (WebClient, Records, @Transactional)
5. Maintained **architectural integrity** across the conversion

---

## 2. Technology Mapping

| Python (Task 1) | Java (Task 2) | Justification |
|----------------|---------------|---------------|
| FastAPI | Spring Boot 3.2.3 | Industry-standard Java REST framework |
| Pydantic v2 models | Java Records + Jakarta Validation | Immutable DTOs, compile-time safety |
| SQLAlchemy ORM | Spring Data JPA + Hibernate | Standard Java ORM with JPA specification |
| psycopg2 driver | PostgreSQL JDBC Driver | Direct JDBC for dynamic target DB queries |
| python-jose (JWT) | jjwt (io.jsonwebtoken) | Most popular Java JWT library |
| OpenAI Python SDK | WebClient (Spring WebFlux) | Non-blocking async HTTP client |
| FastAPI Basic Auth | Spring Security `httpBasic()` | Native Spring Security integration |
| `@app.on_event("startup")` | JPA `spring.jpa.hibernate.ddl-auto=update` | Automatic schema management |
| Docker python:3.11-slim | Eclipse Temurin JDK 17 Alpine | Official Eclipse Foundation JDK |

---

## 3. Conversion Methodology

### Phase 1: Architecture Analysis
Before writing any code, the AI analyzed the Python project structure and proposed equivalent Java/Spring Boot counterparts for every file.

**Prompt 1 — Architecture Mapping:**
```
I have a Python FastAPI project with the following structure:
- app/config.py (pydantic-settings BaseSettings)
- app/database.py (SQLAlchemy engine + session)
- app/models.py (4 SQLAlchemy ORM models)
- app/schemas.py (Pydantic v2 request/response models)
- app/auth.py (Basic Auth + JWT)
- app/main.py (FastAPI app + middleware + exception handlers)
- app/routers/auth.py, metadata.py, classify.py
- app/services/metadata_service.py, classify_service.py, llm_service.py

Convert this to an equivalent Java Spring Boot 3.x project structure.
Requirements:
- Use Java 17
- Use Spring Data JPA for ORM (Hibernate)
- Use Java Records for DTOs
- Use Spring Security for authentication
- Use WebClient for async OpenAI API calls
- Use jjwt for JWT generation
- Follow standard Maven project structure
- Maintain identical API contract (same paths, methods, request/response JSON)
```

**AI Output:** Proposed package structure `com.kafein.discovery` with `entity/`, `repository/`, `dto/`, `config/`, `service/`, `controller/` layers.

---

### Phase 2: Project Skeleton
**Prompt 2 — Project Setup:**
```
Create a pom.xml for a Spring Boot 3.2.3 project with these dependencies:
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation
- spring-boot-starter-webflux (for WebClient)
- postgresql JDBC driver
- jjwt-api, jjwt-impl, jjwt-jackson (version 0.12.5)
- lombok

Also create application.yml that reads from environment variables matching the Python .env file:
SYSTEM_DB_HOST, SYSTEM_DB_PORT, SYSTEM_DB_NAME, SYSTEM_DB_USER, SYSTEM_DB_PASSWORD,
AUTH_USERNAME, AUTH_PASSWORD, JWT_SECRET_KEY, JWT_EXPIRATION_MINUTES,
OPENAI_API_KEY, OPENAI_BASE_URL, OPENAI_MODEL
```

---

### Phase 3: Entity Conversion (models.py → JPA Entities)

**Prompt 3 — Model Conversion:**
```
Convert the following Python SQLAlchemy models to Java JPA entities.
Maintain the same column names, types, and relationships.

Python models:
- DatabaseMetadata: UUID pk, database_name, host, port, username, password, created_at, OneToMany tables
- TableMetadata: UUID pk, database_id FK, table_name, schema_name, created_at, OneToMany columns
- ColumnMetadata: UUID pk, table_id FK, column_name, data_type, is_nullable, column_default, ordinal_position, created_at
- ClassificationResult: UUID pk, column_id FK, sample_count, classification (JSON), created_at

Requirements:
- Use @GeneratedValue(strategy = GenerationType.UUID)
- Use @CreationTimestamp for created_at
- Use CascadeType.ALL with orphanRemoval for parent-child relationships
- Use Lombok @Getter, @Setter, @Builder, @NoArgsConstructor, @AllArgsConstructor
```

---

### Phase 4: DTO Conversion (schemas.py → Java Records)

**Prompt 4 — Schema Conversion:**
```
Convert these Python Pydantic v2 schemas to Java Records:
- DBConnectionRequest (host, port, database, username, password)  
- ClassifyRequest (column_id UUID, sample_count int default 10)
- MetadataListResponse (metadata_id, database_name, created_at, table_count)
- MetadataDetailResponse with nested TableResponse and ColumnResponse
- ClassifyResponse with nested ProbabilityItem
- AuthResponse (access_token, token_type)

Use Jakarta Validation annotations (@NotBlank, @NotNull, @Positive, @Min, @Max).
Use Java Record compact constructors for default values.
```

---

### Phase 5: Security Conversion (auth.py → Spring Security)

**Prompt 5 — Security Migration:**
```
Convert Python FastAPI Basic Auth to Spring Security.

Python implementation:
- HTTPBasicCredentials verification with secrets.compare_digest (timing-safe)
- JWT creation using python-jose with HS256 algorithm

Java requirements:
- Spring Security SecurityFilterChain with httpBasic()
- Stateless session (SessionCreationPolicy.STATELESS)
- InMemoryUserDetailsManager for single admin user
- JWT generation using jjwt 0.12.5 with HMAC-SHA256
- Permit GET "/" without authentication
- All other endpoints require Basic Auth
```

---

### Phase 6: Service Conversion

#### MetadataService
**Prompt 6a — Metadata Service:**
```
Convert Python metadata_service.py to Java Spring @Service.

Key Python logic:
1. Connect to target PostgreSQL using dynamic credentials
2. Test connection with SELECT 1
3. Query information_schema.tables for public schema tables
4. For each table, query information_schema.columns for column metadata
5. Save all to system_db using SQLAlchemy session

Java requirements:
- Use JDBC DriverManager for dynamic target DB connections (not JPA DataSource)
- Use PreparedStatement for parameterized queries
- Use @Transactional for system_db operations
- Mask credentials in log messages (show first 2 chars + "***")
- Throw RuntimeException with descriptive messages on failure
- Return DTO objects mapped from JPA entities
```

#### LlmService
**Prompt 6b — LLM Service:**
```
Convert Python llm_service.py to Java Spring @Service.

Python prompt engineering:
- 12 PII categories with descriptions and examples (Email, Phone, SSN, Credit Card,
  National ID, Full Name, First Name, Last Name, TCKN, Address, Date of Birth, IP Address)
- Plus "Not PII" category (13 total)
- Turkish data awareness (TCKN: 11-digit number)
- Structured markdown prompt with column context, sample data, categories
- JSON mode for reliable output

Java requirements:
- Use Spring WebClient to call OpenAI-compatible API asynchronously
- Use .block() to synchronize for simplicity (can be made fully reactive later)
- Define PII_CATEGORIES as static LinkedHashMap
- Build prompt using String.format() text blocks
- Parse response using Jackson ObjectMapper
- Normalize probabilities to sum to 1.0
- Sort results by probability descending
- Ensure all 13 categories are always present in output
```

#### ClassifyService
**Prompt 6c — Classify Service:**
```
Convert Python classify_service.py to Java @Service.

4-step process (same as Python):
1. Find ColumnMetadata by column_id from system_db
2. Navigate to TableMetadata → DatabaseMetadata via ORM relationships
3. Connect to target DB using JDBC, SELECT sample_count rows WHERE column IS NOT NULL
4. Call LlmService.classifyWithLlm() with column context and samples
5. Map results to ClassifyResponse DTO

Handle exceptions:
- Column not found → IllegalArgumentException (→ 404)
- DB connection error → RuntimeException (→ 500)
- Empty samples → IllegalArgumentException (→ 400)
```

---

### Phase 7: Controller Conversion (routers → @RestController)

**Prompt 7 — REST Controller Migration:**
```
Convert Python FastAPI routers to Spring @RestController classes.
Maintain identical API contracts:

POST /auth → return {access_token, token_type}
POST /db/metadata → 201 Created with metadata including table/column UUIDs
GET /metadata → list of {metadata_id, database_name, created_at, table_count}
GET /metadata/{metadataId} → full detail with tables and columns
DELETE /metadata/{metadataId} → {message: "Metadata deleted successfully"}
GET / → {status, service, version} (no auth required)
POST /classify → classification probabilities for 13 PII categories

Map exceptions to HTTP status codes:
- IllegalArgumentException → 404 Not Found
- RuntimeException (connection errors) → 400/500
- Use ResponseStatusException for flexible status mapping
```

---

### Phase 8: Docker Conversion

**Prompt 8 — Dockerfile Migration:**
```
Convert the Python Dockerfile to Java/Maven.

Python Dockerfile approach:
- python:3.11-slim base
- Multi-stage: install deps → copy code
- Non-root user

Java Dockerfile requirements:
- Multi-stage build: eclipse-temurin:17-jdk-alpine for Maven build
- Runtime stage: eclipse-temurin:17-jre-alpine (smaller, no compiler)
- Cache Maven dependencies in separate layer (COPY pom.xml → dependency:go-offline)
- Build JAR: ./mvnw package -DskipTests
- Non-root user: adduser -S appuser
- HEALTHCHECK using wget (alpine has wget not curl)
- PORT 8000

docker-compose.yml:
- Same 3-container architecture as Python
- Reuse parent demo_db/init.sql for demo database initialization
- Different container name prefix: llm-discovery-java-*
```

---

## 4. Challenges and Solutions

| Challenge | Solution |
|-----------|----------|
| JPA not suitable for dynamic DB connections | Used raw JDBC `DriverManager.getConnection()` for target DB queries in MetadataService and ClassifyService |
| Spring Security blocks `/` health endpoint | Added `.requestMatchers("/").permitAll()` in SecurityFilterChain |
| JJWT 0.12.x API changed from earlier versions | Used `Jwts.builder().subject()` instead of deprecated `claim("sub")`, `Keys.hmacShaKeyFor()` instead of `Keys.secretKeyFor()` |
| Maven wrapper needed for Docker multi-stage build | Generated with `mvn wrapper:wrapper` command |
| `@OneToMany` lazy loading causing N+1 queries | Set `fetch = FetchType.EAGER` on critical relationships |
| JSON column storage in PostgreSQL | Used `@JdbcTypeCode(SqlTypes.JSON)` with `columnDefinition = "jsonb"` |

---

## 5. Python vs Java Comparison

| Aspect | Python/FastAPI | Java/Spring Boot |
|--------|---------------|-----------------|
| Lines of code (approx.) | ~750 | ~950 |
| Startup time | ~1s | ~5-8s |
| Type safety | Runtime (Pydantic) | Compile-time |
| Async support | Native (async/await) | WebFlux (reactive) |
| ORM | SQLAlchemy (code-first) | JPA/Hibernate (annotation-driven) |
| Config validation | Pydantic SecretStr | Spring @Value + externalized config |
| Exception handling | FastAPI exception handlers | @RestControllerAdvice |
| API docs | Built-in Swagger (OpenAPI) | Requires springdoc-openapi dependency |

---

## 6. Verification Results

All endpoints tested after Docker deployment:

| Endpoint | Status | Notes |
|----------|--------|-------|
| `GET /` | ✅ 200 | Returns health status |
| `POST /auth` | ✅ 200 | Returns JWT token |
| `POST /auth` (wrong creds) | ✅ 401 | Returns Unauthorized |
| `POST /db/metadata` | ✅ 201 | Extracted 8 tables, 100+ columns from demo_db |
| `GET /metadata` | ✅ 200 | Lists metadata records |
| `GET /metadata/{id}` | ✅ 200 | Returns tables + column UUIDs |
| `DELETE /metadata/{id}` | ✅ 200 | Cascade deletes all related data |
| `POST /classify` | ⏳ Requires OpenAI API key | Code ready and verified against Python implementation |

---

## 7. Key Learnings

1. **Framework parity matters:** Spring Boot and FastAPI have very similar concepts (dependency injection, middleware/filters, exception handlers) but different idioms. Mapping them 1:1 makes conversion straightforward.

2. **Static vs dynamic DB connections:** Python's SQLAlchemy can create engines on-the-fly easily; in Java, it's cleaner to use raw JDBC for dynamic target DB connections while keeping Spring Data JPA for the system DB.

3. **Prompt specificity drives quality:** The more specific the prompt (exact class names, method signatures, exception types, status codes), the less manual correction needed after AI generation.

4. **Multi-stage Docker is critical for Java:** A fat JDK image (~600MB) is unacceptable for production. Maven build + JRE runtime reduces the final image to ~200MB.

5. **Type safety advantage:** Java's compile-time type checking caught several bugs before runtime that would have been discovered only during testing in Python.
