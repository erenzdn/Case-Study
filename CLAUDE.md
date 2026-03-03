# CLAUDE.md - LLM Database Discovery System

## Project Persona & Role
- **Role:** Senior Software Architect with 10+ years of experience in Backend Engineering.
- **Principles:** Strictly adhere to Clean Code, SOLID, and DRY principles.
- **Objective:** Deliver production-ready, highly secure, and well-documented code for both Python and Java tasks.
- [cite_start]**Security First:** Prioritize PII data protection and secure credential management [cite: 37, 81-118].

## Build & Run Commands
- **Python/FastAPI:** `uvicorn main:app --reload`
- **Docker:** `docker-compose up --build`
- **Java (Spring Boot):** `./mvnw spring-boot:run`
- **Database:** `docker exec -it postgres_db psql -U user -d target_db`

## Code Style & Rules
- **Python:** Use **FastAPI** with Pydantic v2. Strictly use Type Hints for all function signatures.
- **Java:** Use **Spring Boot 3.x**, Jakarta EE, and Lombok. Follow standard Maven project structure.
- [cite_start]**LLM Integration:** All LLM calls must support OpenAI-compatible API format[cite: 11].
- **Naming:** Follow `snake_case` for Python and `camelCase` for Java.
- [cite_start]**Security:** Use Basic Auth for all endpoints [cite: 13, 16-20]. [cite_start]Connection strings must be fetched from `.env`[cite: 122].

## Key Logic Requirements
- [cite_start]**Metadata ID:** Generate unique UUIDs for each metadata record and column[cite: 30, 36].
- [cite_start]**Classification:** Default sample count is 10. Probability distributions must be returned for PII classes[cite: 74, 76].
- [cite_start]**PII Categories:** Strictly follow the 12 categories listed in the case study (Email, Phone, SSN, Credit Card, National ID, Full/First/Last Name, TCKN, Address, DOB, IP) [cite: 81-118].
- [cite_start]**Task 2 Transition:** When converting to Java, map FastAPI logic to Spring Boot Controllers/Services while maintaining architectural integrity [cite: 119-120].

## Environment Variables (.env)
- [cite_start]`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` [cite: 28, 122]
- [cite_start]`LLM_API_KEY`, `LLM_BASE_URL`, `LLM_MODEL` [cite: 11, 122]
- [cite_start]`AUTH_USERNAME`, `AUTH_PASSWORD` [cite: 122]