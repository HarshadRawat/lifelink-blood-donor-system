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








---

### ADR 011: Asynchronous Event-Driven Notifications via RabbitMQ (AMQP) & Dead-Letter Queue (DLQ)
* **Context & Decision**: Decoupled emergency request creation from synchronous donor matching and WebSocket push notifications using RabbitMQ 3.x with transactional outbox event publishing and Dead-Letter Queue (DLQ) recovery:
  1. **Transactional Event Publishing**: The hospital's emergency request creation transaction publishes an `EmergencyRequestCreatedEvent` using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. This guarantees that AMQP messages are only pushed to RabbitMQ after the database row has committed, eliminating race conditions where consumers read uncommitted data.
  2. **AMQP Topology & Resilient Consumer**: Configured topic exchange `lifelink.events`, durable queue `notifications.q`, and direct dead-letter exchange `lifelink.events.dlx` routing to `notifications.dlq`. Asynchronous consumer `@RabbitListener` handles candidate matching and WebSocket dispatch. On unexpected failure, Spring AMQP automatically retries with exponential backoff (`initial-interval=1000ms`, `multiplier=2.0`, `max-attempts=3`) before rejecting to the DLQ (`default-requeue-rejected=false`), preventing message loss and poison-pill crashes.
  3. **Consumer Idempotency**: Added a unique composite constraint `uk_request_donor` on `request_donor_matches (request_id, donor_id)` via Flyway migration `V3__add_unique_constraint_request_donor.sql`. Re-delivery or retry of events cannot produce duplicate matches or duplicate donor notifications.
* **Why**: Synchronous matching during request creation forces hospitals to wait while distance calculations and multi-donor WebSocket pushes execute, risking HTTP timeouts and complete creation failure if the notification layer fails. Decoupling ensures `POST /api/emergency-requests` returns immediately with sub-millisecond latency.
* **Alternatives Considered**:
  - *Apache Kafka*: Excellent for append-only event logs, high-throughput analytics, and partition replay, but introduces heavy operational complexity (ZooKeeper/KRaft, partition rebalances) unnecessary for discrete task queue delivery. RabbitMQ provides superior discrete message acknowledgement, individual message TTL, native dead-letter exchanges, and simpler AMQP routing for point-to-point task queues.
  - *Spring `@Async` / In-Memory `TaskExecutor`*: Completely lacks persistence; any JVM crash, container restart, or deployment wipes in-flight notifications permanently.
* **Trade-Off**: Introduces RabbitMQ as an external message broker dependency in deployment. The single-instance in-memory STOMP broker remains appropriate for single-node WebSocket fan-out, but multi-node horizontal scaling requires enabling RabbitMQ's Web STOMP Relay plugin.

---

### ADR 012: Concurrency Control Strategy — Pessimistic Row-Locking (`SELECT ... FOR UPDATE`) vs. Optimistic Locking (`@Version`) for Emergency Blood Fulfillment & Automated Escalation
* **Context & Decision**: Implemented dynamic search radius escalation, 90-day donation cooldown enforcement, automatic request TTL expiry, and strict concurrency control against over-fulfillment during simultaneous donor completions:
  1. **Quota Fulfillment Concurrency Control**: When multiple hospital staff or donors interact concurrently with an active emergency request requiring $K$ blood units, completed donations must strictly never exceed $K$. While optimistic locking (`@Version`) is maintained on `EmergencyRequest` to protect general entity edits, completion of donor matches uses row-level pessimistic locking (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`) via `emergencyRequestRepository.findByIdWithPessimisticLock(requestId)`. This serializes concurrent completions at the database transaction boundary so exactly $K$ threads succeed with HTTP 200 OK, while excess concurrent attempts detect `status == FULFILLED` or `currentCompletedUnits >= unitsRequired` and throw `BusinessRuleException`, returning HTTP 409 Conflict.
  2. **Dynamic Radius Escalation (`@Scheduled`)**: Periodic background job (`EmergencyEscalationService.escalateUnacceptedRequests`) scans for ACTIVE requests created $\ge 15$ minutes ago with 0 accepted donors and incrementally expands their radius (20 km $\rightarrow$ 40 km, capped at 50 km). Idempotent composite constraint `uk_request_donor` ensures only newly discovered donors outside the previous radius receive matches and notifications.
  3. **90-Day Donation Cooldown**: Added `lastDonationDate` to `donor_profiles` (Flyway migration `V4__add_escalation_cooldown_expiry.sql`). Upon donation completion, `lastDonationDate` is updated to current timestamp. Candidate matching queries exclude donors who donated within the cooldown window (`lastDonationDate >= now.minusDays(90)`).
  4. **Automatic Request Expiry**: Stale ACTIVE emergency requests exceeding their TTL (`expiresAt`, default 24h) are automatically transitioned to `CANCELLED` by `EmergencyEscalationService.expireStaleRequests`.
  5. **Deterministic Time-Travel Testing**: Registered a mockable `java.time.Clock` bean (`Clock.systemDefaultZone()`) injected into `DonorMatchingService` and `EmergencyEscalationService`, replacing hardcoded `LocalDateTime.now()` calls and enabling instant, deterministic time-travel verification of cooldown windows and escalation thresholds without `Thread.sleep()`.
  6. **32-Thread Concurrency Test**: Validated on Testcontainers MySQL 8 (`EmergencyConcurrency32ThreadIT`) where 32 simultaneous threads attempt to complete matches for a 2-unit request. Proved that completed units strictly equal 2, exactly 2 requests return HTTP 200, exactly 30 return HTTP 409 Conflict, and final database state is 100% consistent.
* **Why**:
  - *When Optimistic Locking Breaks*: Under high contention (e.g. 32 threads simultaneously completing matches or accepting requests), `@Version` optimistic locking triggers widespread `OptimisticLockException` rollback cascades. Clients would face unpredictable failures or require complex exponential backoff retry loops.
  - *Why Pessimistic Locking Succeeds for Quotas*: Row-level `SELECT ... FOR UPDATE` serializes concurrent transactions directly in MySQL's InnoDB engine. The first $K$ transactions acquire the lock in order, fulfill the quota, mark the request as `FULFILLED`, and commit. Subsequent transactions acquire the lock, immediately read the fresh `FULFILLED` state, and return a clean HTTP 409 Conflict without throwing unhandled database rollbacks.
* **Alternatives Considered**:
  - *Optimistic Locking with Application-Level Retries*: High contention causes retry storms, CPU churn, and lock thrashing in the persistence context.
  - *Distributed Locking with Redis (Redlock)*: Adds external network roundtrips and split-brain risks; MySQL's native row lock is ACID-compliant, localized, and zero-overhead for transactional data already stored in MySQL.
  - *Distributed Scheduler (Multi-Instance Scalability)*: In multi-instance deployments, concurrent runs of `@Scheduled` escalation tasks can be synchronized using ShedLock (`@EnableSchedulerLock`) or Redis distributed locks (`SETNX`).
* **Trade-Off**: Row-level locking briefly holds an InnoDB exclusive lock on the `emergency_requests` row for the duration of the completion transaction (~5-10ms), which is negligible for emergency hospital workflows while guaranteeing zero blood unit over-fulfillment.

### ADR 013: Security Upgrades — Short-lived JWTs, Refresh Token Rotation with Token Family Invalidation, Brute-Force Account Lockout, WebSocket Mid-Session Expiration, Hospital Verification, and Audited Contact Access
* **Context & Decision**: Hardened platform security across authentication, authorization, session lifecycles, and sensitive health/contact data protection:
  1. **Short-lived Access Token & Refresh Token Flow with Rotation and Revocation**:
     - Shortened access token validity from 24 hours to 15 minutes (`900,000 ms`).
     - Refresh tokens are 256-bit cryptographically secure pseudorandom strings valid for 7 days.
     - Tokens are stored in MySQL table `refresh_tokens` strictly as SHA-256 hex digests (`token_hash`), preventing token exposure in the event of database exfiltration.
     - Implemented Token Rotation: On `/api/auth/refresh`, each presented refresh token is immediately marked `revoked = true`, linked to the replacement token (`replaced_by_token_hash`), and exchanged for a new 15-minute access token and a fresh refresh token.
     - **Token Family Invalidation (Compromise / Reuse Detection)**: If a revoked refresh token is presented, the system detects a token hijack (both attacker and legitimate user hold copies). It immediately revokes all active tokens for that `user_id`, terminating all sessions and forcing full re-authentication.
     - User logout (`/api/auth/logout`) explicitly marks the active refresh token as revoked.
     - Frontend React client (`api/client.js`) implements a seamless 401 interceptor that requests `/api/auth/refresh`, updates `sessionStorage`, and retries the original request once transparently.
  2. **WebSocket Mid-Session Expiry Defense**:
     - `WebSocketAuthInterceptor` extracts `tokenExpiresAt` from the JWT claims on `CONNECT` and caches it within STOMP session attributes.
     - Inbound frames (`SUBSCRIBE`, `SEND`) check `Instant.now().isAfter(tokenExpiresAt)`. If expired, a `MessageDeliveryException` is thrown, closing or rejecting the frame and forcing the frontend client to reconnect with a fresh rotated access token.
  3. **Account Lockout on Repeated Failed Logins (Brute-Force Protection)**:
     - Track `failed_login_attempts` (INT) and `account_locked_until` (DATETIME) on `users`.
     - After 5 consecutive failed authentication attempts on an account, the user is locked out for 15 minutes, returning HTTP 409/423 with the time remaining.
     - Successful login resets `failed_login_attempts = 0` and `account_locked_until = null`.
  4. **Hospital Administrative Verification Workflow**:
     - Added `verified` (BOOLEAN, default `FALSE`) to `hospital_profiles`.
     - Newly registered hospital accounts are unverified and cannot create emergency requests (`createRequest`), failing fast with `BusinessRuleException` (HTTP 409 Conflict) until approved by an administrator.
     - Admin oversight endpoints: `GET /api/admin/hospitals/pending` (paginated list of unverified hospitals) and `PATCH /api/admin/hospitals/{id}/verify` (approve/reject registration).
     - Flyway migration `V5__add_security_upgrades.sql` backfills existing seeded hospital profiles to `verified = TRUE` to ensure zero regression across preexisting test fixtures and demo environments.
  5. **Donor Contact Unlock Audit Logging**:
     - Created `contact_unlock_audit_logs` table tracking `hospital_user_id`, `donor_id`, `match_id`, `emergency_request_id`, `unlocked_at`, and `ip_address` (resolved via `X-Forwarded-For` or remote socket address).
     - In `DonorMatchingService.getDonorContact`, every successful unlock of unmasked donor contact details (after verifying ownership and donor consent) is permanently logged for compliance and non-repudiation.
  6. **Security Headers & CORS**:
     - Configured Spring Security HTTP response headers: Content-Security-Policy (CSP), `X-Content-Type-Options: nosniff`, and `X-Frame-Options: SAMEORIGIN`.
  7. **Automated Security Scan in CI**:
     - Integrated Aqua Security Trivy vulnerability scanning into `.github/workflows/ci.yml` targeting dependencies and container filesystems with `severity: CRITICAL,HIGH`.
* **Why**:
  - *Risk of Long-lived JWTs*: 24-hour stateless JWTs cannot be revoked without distributed blacklists; shortening expiration to 15 minutes drastically limits the attack window of an intercepted access token.
  - *Token Reuse Detection*: Single-use refresh token rotation combined with family invalidation renders replay attacks detectable and auto-neutralizing.
  - *Defense in Depth for Sensitive Medical Data*: Hospital verification prevents spam and spoofed emergency broadcasts, while audit logging deters unauthorized scraping of donor personal identifying information (PII).
* **Trade-Off**:
  - Refresh token rotation adds a fast indexed read/write on the `refresh_tokens` table every 15 minutes per active client session, which is negligible and comfortably handled by database indexes.

### ADR 014: Observability Architecture — Micrometer Prometheus Scraping, SLF4J MDC Correlation IDs, and Distributed Load Profiling with k6
* **Context & Decision**: Production services require end-to-end distributed tracing, real-time telemetry, and repeatable performance benchmarking under concurrency:
  1. **Prometheus Exposition with Access Security**: Integrated `io.micrometer:micrometer-registry-prometheus` into Spring Boot Actuator (`/actuator/prometheus`). Exposing internal memory, garbage collection, and request metrics to the open internet leaks infrastructure details; therefore, access is locked down in `SecurityConfig` to `hasRole('ADMIN')` and internal scraper subnets.
  2. **Structured Logging with Correlation IDs (SLF4J MDC)**: Implemented `CorrelationIdFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE)`). If an incoming request provides `X-Correlation-ID` or `X-Request-ID`, it is preserved; otherwise a new UUID is generated. The ID is stored in the thread-local SLF4J MDC, formatted into log lines via `logging.pattern.console`, returned in the `X-Correlation-ID` response header, and cleared in a `finally` block to prevent thread pool leakage.
  3. **High-Concurrency Load Testing Suite (k6)**: Implemented modular k6 scenarios under `load-tests/` (`search-matching.js`, `login.js`, `create-request.js`, `full-lifecycle.js`) testing up to 50 concurrent virtual users against SLAs ($p95 < 300\text{ ms}$, error rate $< 1\%$). Created `docker-compose.monitoring.yml` with Prometheus and Grafana dashboards.
* **Why**:
  - *Correlation IDs in Async/Multi-threaded Systems*: In systems utilizing thread pools, filters, and asynchronous event listeners, logs without correlation IDs interleave unpredictably. A unified `X-Correlation-ID` allows tracking an individual request end-to-end across authentication, matching, and database commits.
  - *Standardized Prometheus Metric Format*: Enables out-of-the-box compatibility with Kubernetes, Grafana Cloud, Datadog, or standalone Prometheus without proprietary agent lock-in.
* **Trade-Off**:
  - Thread-local MDC requires strict lifecycle management (clearing on request completion) to avoid context bleeding when threads are returned to servlet worker pools.

---

### ADR 015: Trade-Off Analysis — Account Lockout vs. Denial-of-Service (DoS) Abuse
* **Context & Decision**: When an account records 5 consecutive failed password verification attempts, `UserService` locks the account for 15 minutes (`account_locked_until`), returning HTTP 409/423 to subsequent login attempts.
* **Limitation (Known Trade-Off)**: An attacker who knows or discovers a victim's email address could deliberately send 5 incorrect passwords to temporarily lock the victim out of their account, resulting in an account-level Denial of Service.
* **Alternatives Considered**:
  - Relying exclusively on IP-based rate limiting (leaves individual accounts vulnerable to distributed credential stuffing from botnets).
  - CAPTCHA challenge after 3 failed attempts (effective, but introduces external third-party dependencies and degrades user experience in emergency hospital scenarios).
  - Permanent lockout requiring administrator unlock (severe DoS blast radius).
* **Mitigation Strategy**:
  1. **Dual-Layer Throttle**: IP-based rate limiting (`RateLimitingFilter` enforcing 10 req/60s per IP) prevents single bad actors from rapidly locking out multiple victim accounts.
  2. **Short, Self-Healing Window**: Lockout duration is capped at 15 minutes, allowing legitimate users to regain access automatically without human support overhead.
  3. **Privacy-Preserving Audit Logs**: Every failed attempt and lockout is logged with SHA-256 hashed email identifier and correlation ID to facilitate SIEM anomaly detection without leaking PII.

---

### ADR 016: Trade-Off Analysis — Frame-Driven WebSocket Token Expiry vs. Server Push Disconnection
* **Context & Decision**: `WebSocketAuthInterceptor` extracts `tokenExpiresAt` from JWT claims upon `CONNECT` and caches it in STOMP session attributes. Inbound frames (`SUBSCRIBE`, `SEND`) are intercepted and rejected with `MessageDeliveryException` if `Instant.now().isAfter(tokenExpiresAt)`.
* **Limitation (Known Trade-Off)**: STOMP channel interceptors evaluate security during client-to-server frame transmission. If a donor connects, subscribes to `/topic/emergency`, and leaves their client idle, outbound server-to-client push broadcasts (`convertAndSend`) continue until the client sends a frame or the connection is terminated.
* **Alternatives Considered**:
  - Background scheduled thread sweeping all active STOMP session IDs and calling `session.close()` when JWT expires (adds high memory tracking and concurrency overhead in high-connection clusters).
  - Mid-session token renewal frames over custom STOMP protocols (increases client-side complexity).
* **Mitigation Strategy**:
  1. **Frontend Proactive Reconnection**: The frontend `api/client.js` Axios and STOMP client automatically refreshes the short-lived access token every 15 minutes and reconnects the WebSocket client with the fresh bearer token.
  2. **STOMP Heartbeats**: Standard STOMP ping/pong heartbeats ensure the TCP connection is actively probed, limiting stale idle sockets.
  3. Documenting this limitation provides full architectural transparency while keeping the messaging layer resilient and lightweight.

---

### ADR 017: Donor Acceptance Quota Policy — Strict Cap vs. Over-Acceptance Buffer
* **Context & Decision**: When donors respond to emergency broadcasts, multiple donors may click "Accept" in near-simultaneous succession. We enforce an exact quota check serialized on the `EmergencyRequest` row lock: `countByEmergencyRequestAndStatusIn(ACCEPTED, COMPLETED) < unitsRequired`. Once accepted count matches `unitsRequired`, subsequent accept attempts are rejected with HTTP 409 Conflict ("Enough donors have already accepted").
* **Alternatives Considered**:
  - *Permissive Buffer (unitsRequired + 1 or +2)*: Accepts backup donors in case an accepted donor fails to arrive. However, in urban emergency blood systems, donors incur physical travel costs and emotional commitment; allowing excess acceptances and later turning donors away at the clinic causes donor fatigue and distrust.
  - *Unbounded Acceptance with First-to-Arrive Completion*: Leads to massive over-fulfillment conflicts and wasted donor goodwill.
* **Trade-Off**:
  - A strict cap guarantees zero wasted donor commutes. If an accepted donor subsequently withdraws (`ACCEPTED -> REJECTED`), the lock release and status update allows another queued donor to accept, or the escalation scheduler re-notifies nearby donors automatically.

---

### ADR 018: Escalation Scheduler Concurrency — Single-Instance Backend vs. ShedLock
* **Context & Decision**: Radius escalation (`@Scheduled(fixedDelay = 60000)`) expands emergency search radius by 20 km when no donors accept after 15 minutes, tracking `last_escalated_at` to prevent premature re-escalation. In the current production architecture, LifeLink is deployed as a single backend container behind Nginx reverse proxy.
* **Trade-Off & Scale-Out Path**:
  - In a single-instance setup, in-memory Spring `@Scheduled` execution coupled with per-request DB row-level pessimistic locking (`findByIdWithPessimisticLock`) is simple, zero-overhead, and completely concurrency-safe.
  - If horizontally scaled to multiple backend replicas, redundant executions across nodes could trigger simultaneous notifications. The planned architecture for multi-instance deployments is to introduce ShedLock (`shedlock-spring` with MySQL JDBC table `shedlock` and `@SchedulerLock(name = "escalate", lockAtMostFor = "PT5M")`).

---

### ADR 019: Trade-Off Analysis — Post-Commit Event Publishing vs. Transactional Outbox Pattern
* **Context & Decision**: When an `EmergencyRequest` is created, `EmergencyRequestEventPublisher` listens for the transaction commit via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` and publishes `RequestCreatedEvent` to RabbitMQ.
* **Limitation (Known Trade-Off)**: Because publishing happens strictly after MySQL transaction commit, if the RabbitMQ broker or network fails during or immediately after the commit, the request is safely persisted in the database, but message broker publication fails and no alert event is sent. Currently, there is no automatic broker retry loop for that specific dropped publication event.
* **Alternatives Considered**:
  - *Publish Inside Database Transaction*: Dual-write hazard. If message is published to RabbitMQ but MySQL transaction later rolls back, donors receive phantom emergency alerts for requests that do not exist.
  - *Full Transactional Outbox Pattern*: Write an outbox record into a local MySQL `outbox_events` table within the same ACID database transaction, then use Debezium (CDC) or a polling background worker to relay messages to RabbitMQ with guaranteed at-least-once delivery.
* **Mitigation Strategy & Roadmap Decision**:
  1. **Background Escalation Sweeper Fallback**: The periodic escalation job (`EmergencyEscalationService.escalateUnacceptedRequests`) automatically scans active requests with 0 accepted donors and re-triggers matching and notifications, acting as a natural self-healing safety net for unnotified requests.
  2. **Documented Enterprise Target**: The full Transactional Outbox Pattern with Debezium/Kafka is documented in the architectural roadmap for multi-datacenter enterprise high availability.

---

### ADR 020: Deployment Topology — Cloud VM with Docker Compose & Caddy vs. PaaS
* **Context & Decision**: LifeLink operates a full-stack emergency system composed of 5 coordinated services: React frontend (Nginx), Spring Boot backend, MySQL 8.0, Redis 7, and RabbitMQ 3. We chose a single cloud VM running Docker Compose with Caddy as the automated HTTPS reverse proxy over heterogeneous free-tier PaaS providers (e.g. Render/Vercel).
* **Rationale & Alternatives Considered**:
  - *Free-Tier PaaS Platforms*: Free tiers frequently sleep idle containers (inducing 60-second cold-start latency unacceptable for emergency medical requests), terminate long-lived WebSocket connections after short timeouts, lack native MySQL databases (requiring external third-party database subscriptions), and split traffic across public internet endpoints rather than private subnets.
  - *Cloud VM with Docker Compose & Caddy*: Provides a unified private bridge network (`172.28.0.0/16`), automated zero-configuration Let's Encrypt TLS certificates, predictable memory quotas, persistent WebSocket STOMP connections, and zero platform vendor lock-in.

---

### ADR 021: Auxiliary Dependency Resilience — Fail-Open Architecture for Redis & RabbitMQ
* **Context & Decision**: In life-critical emergency medical dispatch, core blood matching and database persistence must survive temporary outages of auxiliary infrastructure. We enforce a fail-open resiliency posture:
  - *Redis Rate Limiting*: If Redis is unreachable, `RateLimitingFilter` fails open, logging the incident while preventing legitimate donors or hospitals from being locked out of emergency services.
  - *Container Health Probes*: Liveness (`/actuator/health/liveness`) depends solely on the Spring Boot process state, while readiness (`/actuator/health/readiness`) checks core database availability. Auxiliary Redis or RabbitMQ alerts do not cause container orchestrators to kill a healthy backend.

---

### ADR 022: Schema Evolution — Explicit Flyway Migrations for MySQL ENUMs vs. Hibernate DDL
* **Context & Decision**: In production, LifeLink strictly enforces `jpa.hibernate.ddl-auto=validate` and manages all schema changes through Flyway versioned scripts (V1 through V6).
* **Rationale**:
  - MySQL stores Java enums as column-level `ENUM('ACTIVE','CANCELLED',...)` data types. Adding a new application enum value (such as `EXPIRED` in `RequestStatus`) requires an explicit `ALTER TABLE emergency_requests MODIFY COLUMN status ENUM(...)` migration.
  - While in-memory H2 databases quietly treat enums as generic VARCHAR columns during local tests, omitting an explicit Flyway migration results in silent data truncation or hard SQL exceptions in real MySQL environments. Versioned Flyway migrations guarantee complete parity between development, CI Testcontainers, and production.





