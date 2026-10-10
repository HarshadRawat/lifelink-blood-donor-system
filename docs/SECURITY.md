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
* Configurable token lifespan (default 24 hours).
* Validation enforces signature integrity, token expiration, and malformed header rejection.

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

---

## 5. WebSocket Connection Security

WebSocket connections over STOMP do not transmit HTTP headers on subsequent message frames. LifeLink solves this using `WebSocketAuthInterceptor`:
* Intercepts the initial `CONNECT` frame.
* Extracts the `Authorization: Bearer <token>` header.
* Validates the JWT signature.
* Sets the authenticated `Principal` in session attributes for targeted queue delivery.

---

## 6. Secret Management & GitHub Hygiene

* **No Hardcoded Secrets**: Production deployments must supply `DB_PASSWORD` and `JWT_SECRET` via environment variables or secret vaults.
* **`.env` Exclusion**: `.gitignore` strictly excludes `.env` and `.env.*.local` while preserving `.env.example`.
* **Git History Sanitization**: If a developer previously committed a secret locally, adding it to `.gitignore` does not remove it from Git history. The secret must be rotated immediately and Git history purged using `git-filter-repo` or BFG Repo-Cleaner before public push.
