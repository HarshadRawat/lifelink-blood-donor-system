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



