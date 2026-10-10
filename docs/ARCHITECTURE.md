# LifeLink — System Architecture & Design Document

This document describes the architectural patterns, component interactions, mathematical modeling, data flow, and concurrency safeguards implemented in **LifeLink**.

---

## 1. Architectural Overview

LifeLink follows a layered, modular Spring Boot architecture decoupled from a modern React 19 single-page application (SPA).

```
┌────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND LAYER                            │
│    React 19 Components • Context Providers • Vite Proxy • Lucide Icons │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTP REST (JSON) / WebSocket STOMP
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              SECURITY LAYER                            │
│  JwtFilter (OncePerRequest) • WebSocketAuthInterceptor • RBAC Guards   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Authenticated Principal (Long userId)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              CONTROLLER LAYER                          │
│   AuthController • EmergencyRequestController • NotificationController │
│   DonorProfileController • HospitalProfileController • OpenAPI Docs    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ DTOs (Request / Response)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                              SERVICE LAYER                             │
│   • DonorMatchingService: Haversine distance & ABO/Rh matrix matching  │
│   • EmergencyRequestService: Dynamic JPA Specification filtering       │
│   • NotificationService: STOMP push notifications                      │
│   • JwtService: HMAC-SHA key signing & token verification              │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ JPA Entities (@Version Locking)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             PERSISTENCE LAYER                          │
│   Spring Data JPA Repositories • Hibernate ORM • MySQL 8 (InnoDB)       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Core Service Responsibilities

### A. Donor Matching Engine (`DonorMatchingService`)
The matching engine executes an algorithmic multi-stage evaluation pipeline to locate compatible, proximate, available blood donors:

1. **Biological Compatibility Matrix**:
   ABO and Rh factor biological compatibility is computed via enhanced Java 17 `switch` expressions:
   * **$O^-$ requests**: Only compatible with $O^-$ donors.
   * **$O^+$ requests**: Compatible with $O^-$ and $O^+$ donors.
   * **$A^-$ requests**: Compatible with $O^-$ and $A^-$ donors.
   * **$A^+$ requests**: Compatible with $O^-$, $O^+$, $A^-$, $A^+$ donors.
   * **$B^-$ requests**: Compatible with $O^-$ and $B^-$ donors.
   * **$B^+$ requests**: Compatible with $O^-$, $O^+$, $B^-$, $B^+$ donors.
   * **$AB^-$ requests**: Compatible with $O^-$, $A^-$, $B^-$, $AB^-$ donors.
   * **$AB^+$ requests**: Universal recipient — compatible with all 8 blood groups.
2. **Availability Check**: Ensures `donor.available == true`.
3. **Haversine Distance Filter**:
   Computes spherical distance across Earth's radius ($R = 6371\text{ km}$):
   $$\Delta\phi = \text{radians}(\text{lat}_2 - \text{lat}_1), \quad \Delta\lambda = \text{radians}(\text{lon}_2 - \text{lon}_1)$$
   $$a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\text{lat}_1)\cos(\text{lat}_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)$$
   $$c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1-a}\right)$$
   $$d = R \cdot c$$
   Filters for $d \le 20.0\text{ km}$.
4. **Duplicate Match Avoidance**: Checks `existsByEmergencyRequestAndDonor`.
5. **Distance Sorting**: Sorts matches nearest-first.

---

## 3. Concurrency Control & Data Integrity

### Optimistic Locking
In healthcare emergency workflows, multiple hospital staff members may interact with the same emergency request simultaneously (e.g., updating units or changing status).

To prevent lost updates:
* `EmergencyRequest` includes `@Version private Long version;`.
* On update operations, Hibernate validates that the version column in the database matches the entity version in memory.
* If a concurrent modification occurred, Hibernate throws `OptimisticLockException`, preventing race conditions.

---

## 4. Real-time Communication (WebSocket STOMP)

Standard HTTP polling wastes bandwidth and causes latency during emergencies. LifeLink uses STOMP over WebSockets:

1. **Connection Handshake**:
   * The client connects to `/ws` with a STOMP `CONNECT` frame.
   * `WebSocketAuthInterceptor` intercepts the frame, extracts `Bearer <token>`, validates the signature via `JwtService`, and attaches a `UsernamePasswordAuthenticationToken` to the WebSocket session attributes.
2. **Notification Delivery**:
   * When `DonorMatchingService` creates a match, `NotificationService` calls `messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", payload)`.
   * The donor receives an immediate in-app toast and badge increment.

---

## 5. Security & Privacy Architecture

* **Stateless Authentication**: JJWT generates compact HMAC-SHA tokens signed with a 256/384-bit key containing user ID and assigned role (`ROLE_DONOR`, `ROLE_HOSPITAL`, `ROLE_ADMIN`).
* **Principal Extraction**: User identity is derived strictly from `Authentication.getPrincipal()`.
* **PII Masking**: Prior to donor acceptance with consent, `contactNumber` and `email` remain masked (`null`).
* **IDOR Protection**: Request ownership verification ensures hospitals can only query matches and contacts associated with requests they created.
