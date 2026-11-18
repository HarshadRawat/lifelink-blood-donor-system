# LifeLink — Security Architecture & Privacy Policy

This document details the security principles, cryptographic controls, authorization mechanisms, and healthcare privacy guidelines implemented across LifeLink.

---

## 1. Authentication & Cryptographic Controls

### Password Hashing
* User credentials are encrypted using `BCryptPasswordEncoder` with a default strength factor of 10.
* Raw passwords are never stored in plaintext, logged, or returned in API responses.

### Stateless JWT Implementation
* Tokens are generated using the `jjwt` library (version `0.12.6`).
* Digital signatures use HMAC-SHA algorithms with a minimum 256-bit or 384-bit cryptographic key.
* Tokens contain claims for `sub` (User ID) and `role` (`ROLE_HOSPITAL`, `ROLE_DONOR`, `ROLE_ADMIN`).
### Short-Lived Access Tokens & Refresh Token Rotation (Task 9)
* **Short-Lived Access Tokens**: Access tokens are valid for 15 minutes (`900,000 ms`), limiting the window of risk if an access token is intercepted.
* **Refresh Tokens**: Cryptographically secure 256-bit random tokens valid for 7 days. Stored in MySQL `refresh_tokens` strictly as SHA-256 hashes (`token_hash`) to protect against database exfiltration.
* **Token Rotation**: Every exchange at `/api/auth/refresh` immediately invalidates the used refresh token and issues both a new short-lived access token and a fresh refresh token.
* **Reuse Detection & Token Family Invalidation**: If an already-revoked refresh token is submitted, the system flags a token theft compromise, immediately revokes all active tokens for that user account, and requires re-authentication.
* **Session Revocation (Logout)**: `/api/auth/logout` explicitly revokes the presented refresh token.
* **Automated Client Renewal**: The React frontend (`api/client.js`) intercepts 401 Unauthorized responses, transparently acquires a new token pair via `/api/auth/refresh`, updates `sessionStorage`, and retries the original request once.

### Account Lockout & Brute-Force Defense
* Tracks consecutive failed login attempts on `users.failed_login_attempts`.
* After 5 consecutive failed attempts, the account is locked for 15 minutes (`users.account_locked_until`), returning HTTP 409/423 with time remaining.
* A successful authentication immediately resets `failed_login_attempts = 0` and clears `account_locked_until`.

---

## 2. Role-Based Access Control (RBAC) & Method Security

* **Filter Security**: `SecurityConfig` enforces URL patterns:
  * `/api/auth/**` $\rightarrow$ `permitAll()`
  * `/swagger-ui/**`, `/v3/api-docs/**` $\rightarrow$ `permitAll()`
  * All other endpoints $\rightarrow$ `authenticated()`
* **Method Security**: Enabled globally via `@EnableMethodSecurity`. Controllers use `@PreAuthorize("hasRole('HOSPITAL')")` or `@PreAuthorize("hasRole('DONOR')")` to safeguard specific actions.

---

## 3. IDOR Prevention & Context-Derived Principals

To prevent Insecure Direct Object Reference (IDOR) attacks:
1. **Never Trust Client-Supplied IDs**: User identity is derived strictly from the Spring Security context:
   ```java
   Long userId = (Long) authentication.getPrincipal();
   ```
2. **Request Ownership Verification**:
   When a hospital accesses matches or donor contacts for an emergency request, the service layer queries the database and verifies ownership:
   ```java
   if (!emergencyRequest.getHospital().getId().equals(hospital.getId())) {
       throw new SecurityException("Hospital not authorized to view matches for this request");
   }
   ```
   An adversary cannot access another hospital's request simply by guessing or tampering with request/match IDs.

---

## 4. Healthcare Privacy & Consent-Driven Contact Sharing

In compliance with healthcare data protection principles:
* **Pre-Acceptance PII Masking**: Unmatched, notified, or pending donor records mask phone numbers and email addresses as `null`.
* **Explicit Informed Consent**: Acceptance requires the donor to grant consent specifically allowing LifeLink to share their registered telephone and email with that demanding hospital.
* **Consent Persistence**: Consent is stored as `contact_sharing_consent = true` on the match record.
* **Hospital-Exclusive Scope**: Only the hospital that created the emergency request can view the contact information. Donors cannot view each other's details.
* **Audit Logging for Contact Unlocks**: Whenever an authorized hospital accesses an accepted donor's unmasked contact details (`getDonorContact`), an immutable record is inserted into `contact_unlock_audit_logs` storing `hospital_user_id`, `donor_id`, `match_id`, `emergency_request_id`, `unlocked_at`, and client `ip_address` (derived from `X-Forwarded-For` or socket).

---

## 5. Hospital Administrative Verification Workflow

To prevent platform abuse and spoofed emergency broadcasts:
* Hospital profiles default to `verified = false` upon self-registration.
* Unverified hospitals cannot create emergency requests (`createRequest`), returning HTTP 409 Conflict (`BusinessRuleException`).
* Administrators review pending registrations via `GET /api/admin/hospitals/pending` and approve/reject via `PATCH /api/admin/hospitals/{id}/verify`.

---

## 6. WebSocket Connection & Mid-Session Expiry Defense

WebSocket connections over STOMP do not transmit HTTP headers on subsequent message frames. LifeLink solves this using `WebSocketAuthInterceptor`:
* **Initial Handshake**: Intercepts the `CONNECT` frame, extracts `Authorization: Bearer <token>`, validates the signature, sets the authenticated `Principal`, and caches `tokenExpiresAt` in STOMP session attributes.
* **Mid-Session Frame Expiration**: On all subsequent inbound STOMP frames (`SUBSCRIBE`, `SEND`), checks `Instant.now().isAfter(tokenExpiresAt)`. If expired, a `MessageDeliveryException` is thrown, dropping the frame or connection and forcing the client to reconnect with a fresh rotated access token.

---

## 7. HTTP Security Headers & CI Vulnerability Scanning

* **Hardening Response Headers**: Spring Security injects:
  * `Content-Security-Policy`: Restricts resource sources to `'self'`, safe script/style policies, and explicit WebSocket connect targets.
  * `X-Content-Type-Options: nosniff`: Mitigates MIME type sniffing attacks.
  * `X-Frame-Options: SAMEORIGIN`: Protects against clickjacking.
* **Automated CI Security Scans**: GitHub Actions runs Aqua Security Trivy filesystem scans on pull requests and branch merges, failing on unaddressed `CRITICAL` or `HIGH` CVEs.

---

## 8. Secret Management & GitHub Hygiene

* **No Hardcoded Secrets**: Production deployments must supply `DB_PASSWORD` and `JWT_SECRET` via environment variables or secret vaults.
* **`.env` Exclusion**: `.gitignore` strictly excludes `.env` and `.env.*.local` while preserving `.env.example`.
* **Git History Sanitization**: If a developer previously committed a secret locally, adding it to `.gitignore` does not remove it from Git history. The secret must be rotated immediately and Git history purged using `git-filter-repo` or BFG Repo-Cleaner before public push.
