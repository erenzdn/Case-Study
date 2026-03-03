# SKILLS.md - Technical Expertise & Implementation Strategies

## 1. Data Classification & LLM Strategy (Task 1)
- [cite_start]**Sampling Logic**: When performing classification, always extract exactly 10 representative rows from the target table to provide context to the LLM[cite: 74, 79].
- [cite_start]**Prompt Engineering**: Instruct the LLM to analyze data patterns and return a probability distribution across the 12 defined PII categories [cite: 76, 81-118].
- [cite_start]**Turkish Specifics (TCKN)**: For Turkish Citizenship Numbers, ensure the LLM or a helper function validates the 11-digit numeric constraint and checksum logic [cite: 106-108].
- [cite_start]**Output Schema**: Force the LLM to output valid JSON to ensure the FastAPI backend can parse probability scores correctly [cite: 76-77].

## 2. Database & Metadata Engineering (Task 1)
- [cite_start]**Introspection**: Use SQLAlchemy `inspect` or PostgreSQL `information_schema` to dynamically fetch table names, column types, and constraints [cite: 21, 26, 31-33].
- **Persistent Identity**: Every discovered column MUST be assigned a unique `UUID`. [cite_start]This UUID is the primary key for all classification operations[cite: 30, 36, 73].
- [cite_start]**Security**: Database credentials for target servers must be encrypted or handled as sensitive transients, never logged in plain text[cite: 37, 122].

## 3. Java Migration & Spring Ecosystem (Task 2)
- [cite_start]**Architectural Mapping**: Convert FastAPI Routers to **Spring @RestControllers** and Pydantic models to **Lombok-annotated POJOs** or **Java Records** [cite: 119-120].
- **Dependency Injection**: Use Spring's `@Service` and `@Repository` layers to mirror the modular logic developed in the Python version.
- [cite_start]**Async Operations**: Use **WebClient** for non-blocking calls to OpenAI-compatible APIs, maintaining the performance standards of the original FastAPI implementation[cite: 11, 119].
- [cite_start]**Auth Transition**: Migration from FastAPI Security to **Spring Security Filter Chain** for Basic Authentication[cite: 13, 16, 119].

## 4. Containerization & Deployment
- [cite_start]**Multi-Stage Builds**: Optimize Dockerfiles for both Python and Java to reduce image size[cite: 12, 123].
- [cite_start]**Orchestration**: Use `docker-compose` to manage the System DB (PostgreSQL), the Backend API, and the target Demo DB[cite: 12, 123, 126].