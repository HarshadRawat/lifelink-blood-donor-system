# LifeLink — Architecture Decision Records (ADR)

This document records the architectural, security, and infrastructure decisions made in LifeLink. Each entry describes what was chosen, why it was chosen, alternatives considered, and the associated trade-offs.

---

### ADR 001: Architecture Style — Layered Spring Boot Monolith
* **Context & Decision**: LifeLink is implemented as a single, modular, layered Spring Boot application (monolith) with distinct controller, service, repository, and security layers, rather than a distributed microservice topology.
* **Why**: For a team of this size and the current operational requirements, a well-structured modular monolith provides unified transactional integrity, straightforward debugging, zero distributed network latency, and low deployment complexity.
* **Alternatives Considered**: Microservices separated by bounded contexts (Auth Service, Matching Service, Notification Service) with an API gateway.
* **Trade-Off**: Scaling individual hot paths independently is not possible without running multiple replicas of the whole backend behind a load balancer; this is addressed via containerized replicas and horizontal scaling planned in future tasks.

---

### ADR 002: Authentication Rate Limiting & NAT / Shared-IP Trade-Off
* **Context & Decision**: An in-memory sliding-window `RateLimitingFilter` enforces a limit of 10 requests per 60 seconds per IP address on sensitive authentication endpoints (`/api/auth/login` and `/api/auth/register`). The limit is configurable via `security.rate-limit.login.max-requests` and `security.rate-limit.login.window-seconds`.
* **Why**: Defends against automated credential-stuffing and brute-force password dictionary attacks with zero external infrastructure dependencies.
* **Alternatives Considered**: Token bucket algorithm via Bucket4j/Redis; per-username lockout; captcha challenges.
* **Trade-Off (NAT & Shared IPs)**: Users sharing a single public IP address (such as college hostels, corporate NATs, or shared mobile Wi-Fi) share the same rate-limiting quota. A burst of failed login attempts from one bad actor could temporarily throttle other legitimate users behind the same NAT. This trade-off is accepted for the initial monolith; per-user progressive delay and Redis-based rate limiting will be introduced in upcoming tasks.

---

### ADR 003: X-Forwarded-For Header Trust Boundary Against IP Spoofing
* **Context & Decision**: `RateLimitingFilter` only trusts the `X-Forwarded-For` header if the direct socket caller (`request.getRemoteAddr()`) matches a configured set of trusted reverse proxies (`security.rate-limit.trusted-proxies`, defaulting to `127.0.0.1, ::1`). Direct clients connecting without an intermediate trusted proxy have their socket IP enforced directly.
* **Why**: Without verifying the immediate caller, an external attacker could trivially send arbitrary `X-Forwarded-For` headers with rotating pseudo-random IPs to bypass rate-limiting entirely.
* **Alternatives Considered**: Blindly reading the first comma-separated address in `X-Forwarded-For`; disabling reverse proxy header extraction completely.
* **Trade-Off**: Requires explicitly configuring the reverse proxy IP / container subnet when deploying into multi-hop or cloud load balancer environments.

---

### ADR 004: Test Database Isolation — H2 vs. Testcontainers MySQL
* **Context & Decision**: Automated CI unit and integration tests currently execute against an isolated, zero-dependency in-memory H2 database using MySQL compatibility mode (`MODE=MySQL`), while production runs on MySQL 8.0.
* **Why**: Provides ultra-fast test execution in CI/CD (~25 seconds for the entire suite) without requiring a Docker daemon or external database during quick compile-test cycles.
* **Alternatives Considered**: Mocking the repository layer exclusively; running all tests directly against a dedicated remote MySQL instance.
* **Trade-Off**: H2 differs from MySQL 8 in locking semantics, index behavior, and specific SQL dialect nuances. This temporary trade-off is resolved in Task 3 where integration tests will migrate to real MySQL via Testcontainers.

---

### ADR 005: Code Coverage (JaCoCo), Container Image Registry (GHCR), and Automated Dependency Upgrades (Dependabot)
* **Context & Decision**: Integrated the JaCoCo Maven plugin (`0.8.12`) into the build lifecycle with automated coverage artifact generation in CI, configured multi-stage Docker image builds published to GitHub Container Registry (`ghcr.io`) upon merges to `main`, and activated Dependabot for weekly ecosystem maintenance across Maven (`/blood-donor-system`), npm (`/blood-donor-system/frontend`), and GitHub Actions (`/`).
* **Why**: Provides transparent and verifiable metrics on test quality (achieving 89.8% line coverage and 89.4% instruction coverage across the backend), automates immutable container delivery directly within GitHub's native ecosystem using repository-scoped tokens without external credentials, and proactively remediates dependency vulnerabilities and deprecations.
* **Alternatives Considered**: Codecov/Coveralls for hosted coverage; Docker Hub for container image publishing; Renovate Bot for dependency management.
* **Trade-Off**: Publishing images to GHCR requires strict lower-case repository and owner naming (sanitized dynamically via `$GITHUB_ENV` shell steps) and utilizes repository action runner minutes and package storage quotas.

---

### ADR 006: Database Versioning (Flyway) and Strict Production Schema Validation
* **Context & Decision**: Replaced automated Hibernate schema generation (`ddl-auto=update`) in production with explicit, versioned SQL migration scripts managed by Flyway (`V1__init.sql` for table DDL, `V2__add_indexes.sql` for query optimization composite indexes). Production runtime strictly enforces `spring.jpa.hibernate.ddl-auto=validate`.
* **Why**: Automated schema evolution via Hibernate (`ddl-auto=update`) is hazardous for production systems: it cannot execute non-destructive column/table renames, cannot prune deprecated columns, executes outside developer source control, and risks silent schema drift across replica environments. Versioned SQL migrations guarantee deterministic, repeatable database state, provide an auditable immutable ledger in `flyway_schema_history`, and allow exact tuning of composite indexes (`idx_donor_blood_avail`, `idx_donor_lat_lng`, `idx_request_status`).
* **Alternatives Considered**: Liquibase (XML/YAML/SQL changelogs); raw manual SQL scripts executed ad-hoc by operators; continuing with Hibernate DDL generation.
* **Trade-Off**: Every database modification now requires authoring a new forward migration file (`V3`, `V4`...) following strict immutability rules. Applied migrations cannot be modified in-place without triggering Flyway checksum validation failures.

---

### ADR 007: Test Database Strategy — Testcontainers MySQL 8.0 with Flyway Validation
* **Context & Decision**: Migrated critical data persistence and integration test suites (`EmergencyLifecycleIntegrationTest` and `EmergencyRequestConcurrencyTest`) to execute against a real, containerized MySQL 8.0 database managed by Testcontainers (`AbstractMySqlIT`). Spring Boot's `@ServiceConnection` dynamically injects JDBC credentials and connection pooling directly into the application context. The base class configures `@Testcontainers(disabledWithoutDocker = true)` and sets `spring.jpa.hibernate.ddl-auto=validate` alongside `spring.flyway.enabled=true`. Fast isolated unit tests continue using Mockito without container overhead.
* **Why**: An in-memory H2 database in MySQL mode exhibits key discrepancies when compared to production MySQL 8.0:
  1. **Locking Semantics**: H2 does not execute identical concurrency semantics under parallel transactions. Running `@Version` optimistic locking tests against real MySQL guarantees that Hibernate correctly handles `ObjectOptimisticLockingFailureException` under InnoDB MVCC and row-level locking.
  2. **Migration & Dialect Validation**: H2 does not strictly validate MySQL-specific Flyway DDL and index creation (such as InnoDB storage engine parameters, `DATETIME(6)`, `utf8mb4_unicode_ci` collations, and foreign key constraint cascading).
  3. **Schema Parity**: Enforcing `ddl-auto=validate` against Flyway-migrated MySQL inside Testcontainers guarantees that any divergence between JPA entity mappings and SQL migrations is caught during build time before reaching production.
* **Alternatives Considered**:
  - Retaining H2 across all integration tests: Fails to detect engine-level lock contention and dialect-specific migration errors.
  - Dedicated persistent external MySQL test instance: Prone to state leakage between test runs, concurrency collisions across parallel CI builds, and external network dependency failures.
* **Trade-Off**: Spin-up time for the MySQL Docker container adds ~5-10 seconds of startup overhead on the first containerized test class in CI. Local environments lacking an active Docker daemon gracefully skip container-dependent tests via `disabledWithoutDocker = true`, preserving fast developer inner-loop feedback.

---

### ADR 008: Deployment Topology, Demo Data Seeding, and CORS Strategy
* **Context & Decision**: Designed a multi-target deployment strategy supporting both unified single-host Docker Compose deployment (behind Nginx with SSL) and decoupled PaaS delivery (Render for containerized backend + Vercel for frontend SPA). Implemented an idempotent `DemoDataInitializer` active strictly under the `demo` profile or `DEMO_SEED_ENABLED=true` that seeds a geographic cluster in Bengaluru (1 hospital, 5 donors with varying compatibility/distance, and 1 active request with matches). Configured configurable CORS via `cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`) across both Spring Security (`CorsConfigurationSource`) and Spring WebSocket (`setAllowedOriginPatterns`).
* **Why**:
  1. A working live demonstration with pre-seeded data lets interviewers and recruiters evaluate the entire system (including geolocation calculations, ABO matrix, and WebSocket push alerts) in under 30 seconds without manually registering accounts or configuring coordinates.
  2. Decoupled frontend (Vercel) and backend (Render) deployments trigger browser CORS pre-flight (`OPTIONS`) checks; without explicit CORS headers and allowed origin patterns, cross-origin REST calls and STOMP WebSocket handshakes fail.
  3. Keeping demo data in an isolated Spring profile (`demo`) prevents test data pollution in production while ensuring automated CI builds (`backend-ci`) remain pristine.
* **Alternatives Considered**:
  - Hardcoding demo records directly in Flyway migration scripts (`V1__init.sql`): Violates production hygiene and pollutes real schemas.
  - Allowing universal wildcard CORS (`*`) without credentials: Fails when authentication headers or credentials are sent, and violates security best practices.
  - Requiring recruiters to manually register multiple accounts and enter GPS coordinates: Creates excessive friction and risks evaluator abandonment.
* **Trade-Off**: Free cloud tiers (Render, Railway) enter sleep mode after 15 minutes of inactivity; mitigated by documenting UptimeRobot ping setup on `/actuator/health` to keep instances warm.

---

### ADR 009: Two-Tier Bounding-Box Geolocation Pre-Filtering vs. Spatial Indices
* **Context & Decision**: Replaced the full in-memory Haversine distance scan across all biologically compatible donors with a Two-Tier Geolocation Pre-Filtering Architecture:
  1. **Tier 1 (Database Index Filter)**: Computes mathematical bounding box boundaries in $\mathcal{O}(1)$ time ($\Delta\phi = r/111.0$, $\Delta\lambda = r / (111.0 \times \cos\phi_{\text{hospital}})$) and queries candidate donors using a composite B-Tree index (`idx_donor_lat_lng` on `(latitude, longitude)`) combined with blood group and availability filtering.
  2. **Tier 2 (JVM Exact Refinement)**: Applies exact spherical Haversine distance calculation ($\le 20.0\text{ km}$) and sorts candidates nearest-first.
  3. **Zero-Regression Fallback**: If candidate query returns empty or is unmocked in legacy tests, falls back to the original method, maintaining 100% test compatibility.
* **Why**:
  1. **Empirical Performance**: On a 100,000 donor dataset on MySQL 8.0, the bounding box pre-filter reduced median matching latency from **116.21 ms to 12.53 ms (9.3x speedup)** and 95th percentile latency from **188.78 ms to 21.35 ms (8.8x speedup)**.
  2. **Memory & Wire Optimization**: Reduced candidate rows loaded into JVM memory from **35,424 rows to 162 rows (99.5% reduction)**, eliminating heap allocation bottlenecks and GC pressure during emergency surges.
  3. **Functional Parity**: Produces 100% identical matching output (both implementations matched exactly 131 donors within 20 km on the benchmark dataset).
* **Alternatives Considered**:
  - *MySQL SPATIAL Extensions (`GEOMETRY / POINT` with `SPATIAL INDEX` / R-Tree)*: Requires adding `hibernate-spatial` and JTS (Java Topology Suite) dependencies, forces database-specific functions (`ST_Distance_Sphere`), requires `NOT NULL` geometry definitions, and breaks zero-dependency H2 compatibility in fast unit tests.
  - *Dedicated Geospatial Store (PostGIS / Elasticsearch)*: Introduces significant infrastructure and deployment overhead for an operation efficiently handled by composite B-Tree indexes.
  - *In-Memory Spatial Index (QuadTree / R-Tree in JVM memory)*: Fails to scale across horizontal backend replicas without distributed cache invalidation and incurs heavy memory overhead on JVM startup.
* **Trade-Off**: A square bounding box circumscribes the circle of radius $r$, pulling candidate false positives in the corners ($20.0\text{ km} < d \le 28.28\text{ km}$). This trade-off is negligible because Tier 2 Haversine refinement filters ~160 rows in less than $0.05\text{ ms}$ in Java. Full benchmark logs and EXPLAIN execution plans are documented in [`docs/BENCHMARK.md`](file:///D:/blood-donor-system/docs/BENCHMARK.md).

---

### ADR 010: Distributed Rate Limiting & Multi-Tier Caching via Redis
* **Context & Decision**: Scaled LifeLink's rate limiting and hot path data access to support distributed multi-instance deployments using Redis 7:
  1. **Distributed Rate Limiting**: Centralized brute-force protection for `/api/auth/login` and `/api/auth/register` using Redis atomic `INCR` + `EXPIRE` (60-second TTL) under key pattern `rl:login:<clientIp>`. Returns HTTP 429 Too Many Requests with header `Retry-After: 60` and structured JSON error payload when requests exceed threshold (10 req/min).
  2. **Multi-Tier Caching**: Configured `RedisCacheManager` with UTF-8 String keys and Jackson JSON value serialization (`RedisSerializer.json()`), explicit TTL (10 minutes default, 15 minutes for `hospital_profiles`). Annotated read-heavy hospital profile endpoints (`getProfile`) with `@Cacheable(value = "hospital_profiles", key = "#userId")` and mutative write endpoints (`createProfile`, `updateProfile`) with `@CacheEvict`.
  3. **High-Availability Resilient Fallback (Fail-Open / Local In-Memory)**: Implemented custom `CacheErrorHandler` and wrapped Redis rate limiter operations. If Redis experiences network partition, timeout, or node crash, authentication requests automatically fall back to local in-memory sliding-window trackers without throwing HTTP 500 errors, and cache misses fall back to direct database queries.
* **Why**:
  1. **Distributed Protection**: In horizontally scaled deployments behind load balancers, an in-memory rate limiter only tracks requests per JVM node. An attacker spraying requests across $N$ instances could execute $N \times 10$ attempts before being blocked. Redis unifies the rate limit counter across all cluster instances.
  2. **Hot Path Database Offloading**: Hospital profile metadata is read frequently during donor matching, emergency request browsing, and hospital dashboard rendering, but updated rarely. Caching in Redis bypasses MySQL query parsing and network roundtrips.
* **Rate Limiting Algorithm Comparison & Trade-Offs**:
  1. **Fixed-Window Counter (Chosen)**:
     - *Algorithm*: Atomic Redis `INCR key`; if `count == 1`, set `EXPIRE key 60`. If `count > 10`, reject.
     - *Complexity*: $\mathcal{O}(1)$ time, $\mathcal{O}(1)$ space per client IP (~100 bytes in Redis).
     - *Window-Edge Burst Caveat*: A client can submit 10 requests at the very end of window $t$ ($t=59\text{s}$) and 10 requests at the start of window $t+1$ ($t=61\text{s}$), resulting in a burst of 20 requests within a 2-second span.
  2. **Sliding-Window Log (ZSET)**:
     - *Algorithm*: Stores request timestamps in Redis Sorted Sets (`ZADD`, `ZREMRANGEBYSCORE`, `ZCARD`).
     - *Pros*: Eliminates the window-edge burst vulnerability entirely.
     - *Cons*: Consumes significantly higher memory ($\mathcal{O}(N)$ timestamps per active IP) and requires multiple Redis operations or Lua scripts per request.
  3. **Token Bucket / Leaky Bucket (Bucket4j / Redis Lua)**:
     - *Algorithm*: Maintains token replenishment rates and burst capacity via Lua scripts.
     - *Cons*: Incurs Lua script execution overhead and introduces external stateful synchronization complexities.
  - *Conclusion*: Fixed-window counter with atomic TTL provides the optimal trade-off of sub-millisecond execution latency, minimal Redis memory footprint, and robust protection against brute-force attacks, augmented with transparent local in-memory fallback during Redis outages.






