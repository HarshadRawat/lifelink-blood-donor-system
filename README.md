# LifeLink — Blood Donor Emergency Response System

[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19.2-61dafb.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8.3-646cff.svg)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg?logo=docker&logoColor=white)](DOCKER.md)
[![Tests](https://img.shields.io/badge/Tests-132%20Passed-success.svg)](#testing)

LifeLink is an emergency blood coordination platform built with Java Spring Boot and React. It bridges the critical latency gap between hospital blood shortages and nearby eligible donors using geolocation radius search, biological blood compatibility matrix matching, real-time push notifications over WebSockets, and privacy-first contact sharing.

---

## Table of Contents

1. [Problem Statement & Objectives](#problem-statement--objectives)
2. [Key Features](#key-features)
3. [User Roles & Permissions](#user-roles--permissions)
4. [Technology Stack](#technology-stack)
5. [System Architecture](#system-architecture)
6. [Repository Structure](#repository-structure)
7. [Prerequisites](#prerequisites)
8. [Quick Start & Setup](#quick-start--setup)
9. [Environment Configuration](#environment-configuration)
10. [API & Swagger Documentation](#api--swagger-documentation)
11. [Testing & Quality Assurance](#testing--quality-assurance)
12. [Security & Privacy Design](#security--privacy-design)
13. [Troubleshooting](#troubleshooting)
14. [Contributing](#contributing)
15. [License](#license)

---

## Problem Statement & Objectives

During medical emergencies, trauma care, and surgeries, hospitals often face acute shortages of specific blood groups. Traditional manual donor registries rely on static telephone directories and broadcast calls, leading to:
* **High latency**: Hours spent locating eligible donors.
* **Geographical inefficiency**: Contacting donors located too far away to arrive in time.
* **Compatibility mismatches**: Incompatible donor types being called.
* **Privacy concerns**: Exposing donor personal contact details publicly without consent.

**LifeLink solves this through:**
* Automated geolocation filtering using the **Haversine great-circle distance algorithm** ($\le 20.0\text{ km}$ radius).
* Medical **ABO/Rh compatibility matrix matching** ensuring accurate physiological compatibility.
* **Sub-second push notifications** delivered via STOMP WebSockets directly to matching donors.
* **Secure, consent-driven contact sharing**: Donor phone and email addresses remain masked until the donor explicitly accepts the request and grants consent to coordinate.

---

## Key Features

* **Hospital Emergency Request Portal**:
  * Broadcast emergency blood requests with blood type, units needed, and urgency level (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
  * Automatic geolocation capture or manual coordinate positioning.
  * Real-time tracking of matched donors and fulfillment status.
* **Algorithmic Matching Engine**:
  * Distance calculation using the Haversine equation ($R = 6371\text{ km}$).
  * Full ABO & Rh biological compatibility matrix ($O^-$ universal donor; $AB^+$ universal recipient).
  * Ranked donor matching (nearest eligible donors prioritized).
  * Duplicate match suppression.
* **Donor Control Center**:
  * Availability toggle (`AVAILABLE` / `UNAVAILABLE`).
  * Instant emergency request match view with demanding hospital name, city, and urgency.
  * Informed acceptance & decline controls with explicit contact-sharing consent disclosure.
* **Real-time Push Notifications**:
  * STOMP protocol over WebSocket with secure JWT handshake authentication.
  * Unread count badges, in-app notification drawer, and auto-dismiss alerts.
* **Concurrency Control & Safety**:
  * Optimistic concurrency locking (`@Version`) preventing race conditions during concurrent request updates.
  * Spring Data JPA Specifications enabling composable multi-criteria dynamic filtering and database pagination (`LIMIT/OFFSET`).

---

## User Roles & Permissions

| Role | Access Permissions |
| :--- | :--- |
| `ROLE_HOSPITAL` | Create/manage emergency blood requests, trigger donor matching, access contact details of accepted donors, mark donation fulfilled. |
| `ROLE_DONOR` | Manage donor profile, toggle donation availability, receive real-time match alerts, accept/decline match with contact sharing consent. |
| `ROLE_ADMIN` | System administrative oversight and platform monitoring. |

---

## Technology Stack

### Backend
* **Language**: Java 17+
* **Framework**: Spring Boot 4.1.1
* **Security**: Spring Security 6, JJWT 0.12.6 (stateless HMAC-SHA JWT)
* **Data Access**: Spring Data JPA, Hibernate, MySQL Connector/J
* **Database**: MySQL 8.0+
* **Real-time**: Spring WebSocket, STOMP messaging broker (`SimpMessagingTemplate`)
* **API Documentation**: SpringDoc OpenAPI 3.0.3 / Swagger UI
* **Build Tool**: Maven 3.9+ (includes Maven Wrapper `mvnw`)

### Frontend
* **Core**: React 19.2.8
* **Build & Dev Tool**: Vite 8.3.4
* **Routing**: React Router DOM 7.18.4
* **Icons**: Lucide React 1.54.0
* **WebSocket Client**: `@stomp/stompjs` 7.3.0
* **Styling**: Vanilla CSS design system with responsive design tokens

---

## System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        LifeLink Architecture                           │
├────────────────────────────────────────────────────────────────────────┤
│                           React 19 Frontend                            │
│           (Donor Dashboard / Hospital Portal / Real-time Alerts)       │
└───────────────────────────┬────────────────────────────────────────────┘
                            │  HTTP REST / WebSocket STOMP
                            ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 Spring Boot 4 Application Boundary                     │
├────────────────────────────────────────────────────────────────────────┤
│  Security Layer:                                                       │
│    • JwtFilter (Stateless Bearer token validation)                     │
│    • WebSocketAuthInterceptor (CONNECT frame token verification)       │
│    • Method Security (@PreAuthorize checks)                            │
├────────────────────────────────────────────────────────────────────────┤
│  Core Services:                                                        │
│    • DonorMatchingService (Haversine equation & ABO/Rh matrix)         │
│    • EmergencyRequestService (JPA Specifications & Version locking)    │
│    • NotificationService (STOMP WebSocket push messaging)             │
├────────────────────────────────────────────────────────────────────────┤
│  Data Layer:                                                           │
│    • Spring Data JPA Repositories                                      │
│    • Optimistic Locking (@Version on EmergencyRequest)                 │
│    • MySQL Database (InnoDB storage engine)                            │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Repository Structure

```
blood-donor-system/
├── blood-donor-system/              # Spring Boot Backend & React Frontend
│   ├── frontend/                    # React 19 + Vite Application
│   │   ├── src/
│   │   │   ├── api/                 # API client & fetch interceptors
│   │   │   ├── components/          # Reusable UI components & badges
│   │   │   ├── context/             # AuthContext & NotificationContext (STOMP)
│   │   │   ├── pages/               # Hospital, Donor, Auth, & Info pages
│   │   │   └── utils/               # Coordinate & request utilities
│   │   ├── package.json             # Frontend dependencies
│   │   ├── vite.config.js           # Vite dev proxy configuration
│   │   └── .env.example             # Frontend environment example
│   ├── src/
│   │   ├── main/java/com/example/blooddonor/
│   │   │   ├── config/              # Security, OpenAPI, & WebSocket configuration
│   │   │   ├── controller/          # REST Controllers
│   │   │   ├── dto/                 # Request & Response Data Transfer Objects
│   │   │   ├── entity/              # JPA Entities (Optimistic locking & Enums)
│   │   │   ├── exception/           # Global exception handling & RFC errors
│   │   │   ├── repository/          # Spring Data JPA Repositories & Specifications
│   │   │   ├── security/            # JwtFilter implementation
│   │   │   └── service/             # Domain logic (Haversine, Matching, Notif)
│   │   └── test/java/com/example/blooddonor/ # 132 automated test cases
│   ├── pom.xml                      # Maven project configuration
│   ├── mvnw / mvnw.cmd              # Maven wrappers
│   └── .gitignore
├── docs/                            # Deep Architecture & API Guides
│   ├── ARCHITECTURE.md              # Detailed architecture breakdown
│   ├── API.md                       # REST API endpoint reference
│   └── SECURITY.md                  # Security model & privacy standards
├── .env.example                     # Root environment configuration template
├── .gitignore                       # Root Git ignore rules
├── CONTRIBUTING.md                  # Development guidelines
├── README.md                        # Project documentation
└── SETUP.md                         # Clean machine setup guide
```

---

## Prerequisites

* **Java Development Kit (JDK)**: JDK 17 or higher
* **Node.js**: v18.0.0 or higher (v20+ recommended)
* **npm**: v9.0.0 or higher
* **MySQL Server**: 8.0 or higher
* **Ports**: `8080` (Backend API) and `5173` (Frontend Dev Server)

---

## Quick Start & Setup

### Option 1: Run with Docker Compose (Recommended — Zero Host Setup)

Run the entire full-stack application (MySQL 8.0, Spring Boot backend, and React Nginx frontend) in isolated containers with a single command:

```powershell
# 1. (Optional) Copy example Docker environment configuration
cp .env.docker.example .env

# 2. Build and run all containers in background
docker compose up -d --build
```

* **Frontend Web Portal**: [http://localhost](http://localhost) (Port 80)
* **Swagger UI API Docs**: [http://localhost/swagger-ui.html](http://localhost/swagger-ui.html)
* **Direct Backend API**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

For in-depth Docker commands, live logs, volume backups, and custom port configuration, see the [Enterprise Docker Guide (DOCKER.md)](DOCKER.md).

---

### Option 2: Local Manual Setup (Non-Docker)

#### 1. Database Setup
Log into your MySQL terminal or GUI and create the database:
```sql
CREATE DATABASE blood_donor_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2. Configure Environment
Copy `.env.example` to `.env` or set your environment variables:
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
```
*(By default, `application.properties` falls back to standard local credentials if variables are not set).*

#### 3. Start the Backend
Navigate to the project root and run Spring Boot:
```powershell
cd blood-donor-system
.\mvnw.cmd spring-boot:run
```
The backend starts at `http://localhost:8080`.

#### 4. Start the Frontend
In a separate terminal:
```powershell
cd blood-donor-system/frontend
npm install
npm run dev
```
Open **`http://localhost:5173`** in your browser.

---

## Environment Configuration

| Variable | Description | Default | Scope |
| :--- | :--- | :--- | :--- |
| `PORT` | Backend server HTTP port | `8080` | Backend |
| `DB_URL` | JDBC MySQL connection URL | `jdbc:mysql://localhost:3306/blood_donor_system` | Backend |
| `DB_USERNAME` | MySQL database username | `root` | Backend |
| `DB_PASSWORD` | MySQL database password | `your_mysql_password` (set via env) | Backend |
| `JPA_DDL_AUTO` | Hibernate schema mode (`update`, `validate`, `none`) | `update` | Backend |
| `JWT_SECRET` | 256/384-bit cryptographic signing key | Configured key | Backend |
| `JWT_EXPIRATION_MS` | JWT validity in milliseconds | `86400000` (24h) | Backend |
| `VITE_API_BASE_URL` | Frontend REST API base URL | Empty (uses Vite dev proxy) | Frontend |

---

## API & Swagger Documentation

When the backend is running, interactive OpenAPI 3 documentation is available at:
* **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

For a complete tabular list of endpoints, request bodies, and error codes, see [docs/API.md](docs/API.md).

---

## Testing & Quality Assurance

LifeLink maintains a suite of **132 automated tests** covering controllers, services, repositories, security authorization, and WebSocket interceptors.

Run the full test suite:
```powershell
cd blood-donor-system
.\mvnw.cmd test
```

Build the frontend bundle:
```powershell
cd blood-donor-system/frontend
npm run build
```

---

## Security & Privacy Design

1. **Context-Derived Principals**: Authenticated user IDs are extracted directly from `SecurityContextHolder.getContext().getAuthentication().getPrincipal()`. The server never trusts client-supplied IDs.
2. **Defensive PII Exposure**: Contact information (telephone numbers and emails) is masked at the service level and only revealed when the donor explicitly marks their match as `ACCEPTED` with consent.
3. **IDOR Prevention**: Emergency request ownership checks verify that the requesting hospital owns the target request before permitting match or contact inspection.
4. **Optimistic Locking**: JPA `@Version` on emergency requests prevents race conditions when concurrent users update blood request status or units.

For complete security specifications, see [docs/SECURITY.md](docs/SECURITY.md).

---

## Troubleshooting

* **MySQL Connection Refused**: Verify that the MySQL service is running on port 3306 and that the `blood_donor_system` database has been created.
* **Port 8080 or 5173 Already in Use**: Free the ports or pass `-Dserver.port=8081` / `--port 5174`.
* **CORS Errors**: The Vite development server includes a built-in proxy in `vite.config.js` forwarding `/api` and `/ws` to `localhost:8080`, eliminating browser CORS issues in development.

---

## Contributing

Please review [CONTRIBUTING.md](CONTRIBUTING.md) for details on code style, branch naming conventions, and the pull request checklist.

---

## License

This project is currently distributed for educational and assessment purposes. The repository owner should assign an appropriate open-source license (such as MIT or Apache 2.0) prior to public distribution.
