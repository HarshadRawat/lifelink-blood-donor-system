# LifeLink — Emergency Blood Donor Matching

[![CI](https://github.com/HarshadRawat/lifelink-blood-donor-system/actions/workflows/ci.yml/badge.svg)](https://github.com/HarshadRawat/lifelink-blood-donor-system/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/HarshadRawat/lifelink-blood-donor-system/branch/main/graph/badge.svg)](https://codecov.io/gh/HarshadRawat/lifelink-blood-donor-system)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.2-61dafb.svg)](https://react.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg?logo=docker&logoColor=white)](DOCKER.md)
[![Tests](https://img.shields.io/badge/Tests-256%20Passed-success.svg)](#testing)

> **Live Demo**: [https://lifelink.onrender.com](https://lifelink.onrender.com) &nbsp;|&nbsp; **Swagger API Docs**: [https://lifelink-backend.onrender.com/swagger-ui.html](https://lifelink-backend.onrender.com/swagger-ui.html) &nbsp;|&nbsp; **Walkthrough Video**: [Watch 3-Min Demo](https://youtu.be/placeholder-demo) &nbsp;|&nbsp; **Full Architecture Report**: [docs/IMPLEMENTATION.md](docs/IMPLEMENTATION.md)

LifeLink is an emergency blood coordination platform built with Java Spring Boot and React. During acute blood shortages, hospitals face critical latency due to manual telephone directories, physiological compatibility mismatches, and donor privacy concerns. LifeLink automates the coordination lifecycle by pairing a **Two-Tier B-Tree Geolocation Pre-Filter** with physiological **ABO/Rh compatibility**, **RabbitMQ asynchronous event dispatch**, and **STOMP WebSockets** to alert qualified donors within 20 km in sub-seconds—while safeguarding donor contact privacy through an audited, consent-driven disclosure workflow.

---

## Demo Logins (Demo Site Only)

A pre-seeded demonstration cluster in Central Bengaluru with active emergency requests and nearby compatible/incompatible donors:

| Role | Email | Password | Pre-Configured State |
| :--- | :--- | :--- | :--- |
| **Hospital** | `hospital@lifelink.com` | `Hospital123!` | City Care Hospital (Central Bengaluru), 1 active A+ emergency request |
| **Donor (Direct Match)** | `alice.donor@lifelink.com` | `Donor123!` | A+ donor, 0.5 km away, pending emergency broadcast alert |
| **Donor (Universal)** | `bob.donor@lifelink.com` | `Donor123!` | O- donor, 0.8 km away, accepted with contact sharing consent granted |
| **Donor (Incompatible)** | `charlie.donor@lifelink.com` | `Donor123!` | B+ donor, 4.5 km away, filtered out by ABO compatibility engine |

---

## Features

* **Sub-Second Geolocation Matching**: Two-tier query engine (indexed latitude/longitude bounding-box + exact spherical Haversine refinement) evaluates 100,000 donors in 12.5 ms (9.3x faster than in-memory scans).
* **Physiological ABO/Rh Compatibility**: Enforces clinical blood transfusion rules (e.g. O- universal donor, AB+ universal recipient) directly within database index predicates.
* **Event-Driven Asynchronous Notifications**: Decouples hospital request creation from notification dispatch via RabbitMQ with exponential retries and dead-letter queue (DLQ) fault tolerance.
* **Real-Time Push Alerts over WebSockets**: Sub-second in-browser notifications delivered via Spring STOMP WebSockets with mid-session JWT expiration defense.
* **Privacy-By-Design Contact Disclosure**: Donor phone numbers and emails are masked by default; unmasked contact details require explicit donor consent and generate permanent immutable audit logs.
* **Concurrency Quota Protection**: Row-level pessimistic locking (`SELECT ... FOR UPDATE`) prevents blood unit over-fulfillment under simultaneous multi-donor acceptance (proven with 32 concurrent threads).
* **Defense-in-Depth Security**: Short-lived access tokens (15m), refresh token rotation with token family reuse invalidation, account lockout brute-force defense, and automated Trivy vulnerability scanning.

---

## Architecture

```mermaid
flowchart TD
    subgraph ClientLayer ["Client Layer"]
        UI["React 19 + Vite SPA\n(Tailwind CSS)"]
        WS["STOMP WebSocket Client\n(SockJS)"]
    end

    subgraph GatewayLayer ["Reverse Proxy Gateway"]
        Nginx["Nginx Reverse Proxy\n(Port 80 / 443)"]
    end

    subgraph BackendLayer ["Spring Boot 4 Application"]
        AuthFilter["Security Filters\n(JWT, RateLimiting, CorrelationId)"]
        Controllers["REST Controllers\n(Swagger / OpenAPI)"]
        MatchingEngine["Two-Tier Matching Engine\n(Bounding Box + Haversine)"]
        Escalation["@Scheduled Escalation\n(Radius 20km → 40km, Cooldown)"]
        AuditService["Audit & Security Service\n(Refresh Token, Lockout, PII Logs)"]
    end

    subgraph MessagingLayer ["Asynchronous Messaging & Broker"]
        RabbitMQ["RabbitMQ 3.x\n(lifelink.events → notifications.q + DLQ)"]
        WSBroker["Spring STOMP Broker\n(/user/queue/notifications)"]
    end

    subgraph DataLayer ["Data & Cache Tier"]
        MySQL[("MySQL 8.0 (InnoDB)\nB-Tree Indexes + Flyway Migrations")]
        Redis[("Redis 7 (Alpine)\nDistributed Rate Limiting & Cache")]
    end

    UI -->|HTTP / REST| Nginx
    WS -->|WSS / STOMP| Nginx
    Nginx --> AuthFilter
    AuthFilter --> Controllers
    Controllers --> MatchingEngine
    Controllers --> AuditService
    MatchingEngine -->|Transactional Event| RabbitMQ
    RabbitMQ -->|@RabbitListener| WSBroker
    WSBroker -->|Push Alert| WS
    Controllers --> MySQL
    MatchingEngine --> MySQL
    AuthFilter --> Redis
    Controllers --> Redis
```

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend Framework** | Java 17, Spring Boot 4.1.1 (WebMVC, Security, Data JPA, Actuator, AMQP) |
| **Frontend SPA** | React 19.2, Vite 8.3, Tailwind CSS, Lucide Icons, SockJS, STOMP.js |
| **Databases & Cache** | MySQL 8.0 (InnoDB, B-Tree indexes, Flyway migrations), Redis 7 (Alpine) |
| **Message Broker** | RabbitMQ 3.x (Management, direct exchanges, dead-letter queues) |
| **Observability** | Micrometer, Prometheus v3, Grafana 11, SLF4J MDC Correlation IDs |
| **Testing & CI/CD** | Testcontainers (MySQL 8, RabbitMQ, Redis), JUnit 5, Mockito, Vitest, k6, GitHub Actions, Trivy |
| **Containerization** | Docker, Docker Compose, Multi-stage builds, Non-root runtime user (UID 10001), JRE-only image with curl healthchecks |

---

## Run Locally (3 Commands)

### Prerequisites
* Git, Docker & Docker Desktop running.

```bash
# 1. Clone repository
git clone https://github.com/HarshadRawat/lifelink-blood-donor-system.git
cd lifelink-blood-donor-system

# 2. Copy environment file
cp .env.example .env

# 3. Start complete stack (MySQL, Redis, RabbitMQ, Backend, Frontend)
docker compose up --build
```

* **Web Application**: [http://localhost](http://localhost)
* **Swagger API Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **Actuator Health Probe**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
* **Prometheus & Grafana (Optional)**: `docker compose -f docker-compose.monitoring.yml up -d` ([http://localhost:3001](http://localhost:3001), user: `admin`, pass: `admin`)

---

## Testing

LifeLink maintains **256 automated tests** with 100% pass rate:

```bash
# Run full backend test suite (includes Testcontainers MySQL, RabbitMQ & Redis)
cd blood-donor-system
./mvnw test

# Run frontend Vitest suite
cd frontend
npm test
```

| Test Category | Framework | Test Count | Key Scenarios Covered |
| :--- | :--- | :---: | :--- |
| **Extreme Concurrency** | Testcontainers MySQL 8.0 | 2 | 32 simultaneous threads completing matches on 2-unit request; strictly 2 OKs, 30 Conflicts, 0 over-fulfillment |
| **Asynchronous Messaging** | Testcontainers RabbitMQ 3 | 2 | `RequestCreated` event publish, `@RabbitListener` match creation, DLQ retry routing |
| **Distributed Rate Limiting & Cache** | Testcontainers Redis 7 | 6 | Multi-instance sliding-window throttling, cache eviction on update |
| **Security & Authentication** | Mockito / MockMvc | 38 | 15m JWTs, refresh token rotation, token reuse family invalidation, 5-attempt account lockout, WebSocket mid-session expiry |
| **Core Domain Services** | Spring Boot Test / Mockito | 158 | Two-tier 20km Haversine pre-filtering, 90-day donation cooldown, dynamic radius escalation (20km → 40km), audit logging |
| **Frontend SPA** | Vitest / Testing Library | 50 | Dashboards, auth context, token rotation interceptor, protected routes |
| **Total Automated Tests** | | **256** | **100% Green (0 Failures, 0 Errors, 0 Skipped)** |

---

## Performance

Empirical performance evaluation on **100,000 synthetic donor records** on MySQL 8.0 (full benchmark report in [docs/BENCHMARK.md](docs/BENCHMARK.md)):

| Metric | Before Optimization (In-Memory Haversine) | After Optimization (Two-Tier B-Tree Pre-Filter) | Impact |
| :--- | :---: | :---: | :---: |
| **Median Latency ($p50$)** | 116.21 ms | **12.53 ms** | **9.3x faster** |
| **95th Percentile ($p95$)** | 188.78 ms | **21.35 ms** | **8.8x faster** |
| **JVM Memory Allocation** | 35,424 records transferred | **162 records transferred** | **99.5% reduction** |
| **Matching Parity** | 131 qualified donors | **131 qualified donors** | **100% parity** |
| **k6 Concurrency Throughput** | — | **428.4 req/s** (50 VUs, $p95 = 18.75\text{ ms}$) | **Zero SLA violations** |

---

## Challenges and Decisions

Detailed architectural reasoning and trade-offs are logged across 14 Architectural Decision Records in [DECISIONS.md](DECISIONS.md). Three key challenges solved:

1. **Concurrency Quota Control: Row-Level Locking vs. Optimistic Locking ([ADR 012](DECISIONS.md#adr-012))**:
   * *Problem*: High contention when 32 donors simultaneously accept matches for a 2-unit request causes `@Version` optimistic locking to throw unhandled `OptimisticLockException` rollback storms.
   * *Solution*: Implemented row-level pessimistic locking (`SELECT ... FOR UPDATE` via `entityManager.refresh(..., PESSIMISTIC_WRITE)` on `emergency_requests.unitsFulfilled`). Exactly 2 transactions fulfill units; the remaining 30 acquire the lock, detect quota fulfillment, and return a clean HTTP 409 Conflict.
2. **100,000 Donor Geolocation Query Latency ([ADR 008](DECISIONS.md#adr-008))**:
   * *Problem*: In-memory Haversine distance scans across 100k database records took 188 ms and loaded 35k rows into heap memory, causing GC pressure.
   * *Solution*: Implemented a Two-Tier Geolocation Filter using mathematical bounding-box pre-filtering in SQL (`idx_donor_lat_lng` composite B-Tree index) followed by JVM Haversine refinement on only 162 candidates, cutting latency to 12.5 ms (9.3x speedup) with zero GIS dependencies.
3. **Decoupled Asynchronous Notifications with Dead-Letter Queues ([ADR 011](DECISIONS.md#adr-011))**:
   * *Problem*: Synchronous WebSocket and email dispatch during emergency request creation blocked hospital HTTP threads and caused request rollbacks if notification delivery failed.
   * *Solution*: Decoupled dispatch using RabbitMQ with `@TransactionalEventListener(phase = AFTER_COMMIT)`. Added composite unique constraints `uk_request_donor` to ensure idempotent consumer processing and configured dead-letter queues (`notifications.dlq`) with exponential retry backoff.

---

## Roadmap

- [x] **Task 0**: Data cleanup, secret scanning, and rate limiter proxy hardening.
- [x] **Task 1**: GitHub Actions CI pipeline, JaCoCo coverage badges, GHCR container publishing.
- [x] **Task 2**: Flyway versioned migrations (V1-V5) and composite indexes.
- [x] **Task 3**: Testcontainers integration testing with real MySQL 8.0.
- [x] **Task 4**: Production cloud deployment and realistic Bangalore demo data seeder.
- [x] **Task 5**: Two-tier geolocation optimization and 100,000 donor benchmark suite.
- [x] **Task 6**: Redis distributed rate limiting and profile caching with TTL.
- [x] **Task 7**: RabbitMQ asynchronous event-driven notifications with DLQ retries.
- [x] **Task 8**: Escalation scheduling, 90-day cooldown, and 32-thread concurrency locking.
- [x] **Task 9**: 15m JWTs, refresh token rotation with reuse detection, account lockout, audit logs.
- [x] **Task 10**: Micrometer Prometheus metrics, correlation IDs (MDC), and k6 load tests.
- [x] **Task 11**: Scannable README, Mermaid architecture, and decisions showcase.
- [ ] **Task 12**: Resume, LinkedIn, and interview preparation.
