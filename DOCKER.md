# LifeLink — Enterprise Docker & Containerization Guide

This guide details the **industry-grade Docker containerization** architecture for **LifeLink**. It enables running the complete full-stack system (MySQL 8.0 database, Spring Boot backend, and React Vite frontend) in isolated, production-hardened containers with a single command.

---

## 1. System Architecture

```mermaid
graph TD
    Client["Browser / Client"] -->|Port 80 / HTTP| Nginx["Frontend Container (Nginx 1.27 Alpine)"]
    
    subgraph "Docker Network: lifelink-network"
        Nginx -->|Serves Static SPA| ReactApp["React 19 + Vite Dist Bundle"]
        Nginx -->|Reverse Proxy /api/*| Backend["Backend Container (Spring Boot 4.x / Temurin 17 JRE)"]
        Nginx -->|WebSocket Proxy /ws| Backend
        
        Backend -->|JDBC Port 3306| MySQL["Database Container (MySQL 8.0)"]
    end
    
    subgraph "Host Direct Access (Dev / Admin)"
        Client -.->|Port 8080 (Optional direct)| Backend
        Client -.->|Port 3307 (MySQL Workbench)| MySQL
    end
    
    MySQL -->|Persistent Storage| Volume[("Named Volume: lifelink_mysql_data")]
```

### Key Architectural Highlights
* **Multi-Stage Builds**:
  * **Backend**: Compiled using `maven:3.9-eclipse-temurin-17` and runs in a lightweight, hardened `eclipse-temurin:17-jre-jammy` image.
  * **Frontend**: Bundled via `node:20-alpine` and served via ultra-fast `nginx:1.27-alpine`.
* **Security & Least Privilege**:
  * The backend runs under a dedicated, unprivileged non-root user (`spring:spring` UID 10001).
  * No build-time compilers or package manager caches exist in the final runtime images.
* **Smart Gateway & Reverse Proxy**:
  * Nginx handles Single Page Application (SPA) fallback routing (`try_files $uri $uri/ /index.html`), preventing 404s on browser reload.
  * Automatic proxying for `/api/` REST endpoints and `/ws` WebSocket push notifications eliminates Cross-Origin (CORS) complications.
  * Security headers (`X-Frame-Options`, `X-Content-Type-Options`, `X-XSS-Protection`, `Referrer-Policy`) and Gzip compression pre-configured.
* **Resilience & Dependency Healthchecks**:
  * The backend will not boot until MySQL passes its `mysqladmin ping` healthcheck.
  * The frontend will not serve until the backend passes its OpenAPI `/v3/api-docs` healthcheck.
* **Zero Host Pollution**:
  * No JDK, Maven, or local MySQL installation is needed on the host system to run the app.

---

## 2. Prerequisites

Ensure **Docker** and **Docker Compose** are installed:

| Operating System | Recommended Tool | Verification Command |
| :--- | :--- | :--- |
| **Windows 10/11** | [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/) (with WSL 2 backend) | `docker --version`<br>`docker compose version` |
| **macOS** | [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/) | `docker --version`<br>`docker compose version` |
| **Linux (Ubuntu/Debian)** | Docker Engine + Docker Compose Plugin | `docker compose version` |

> [!NOTE]
> If you are on Windows and haven't installed Docker Desktop yet, download and install it from the official Docker website. Once installed and running, open your PowerShell or command prompt to execute the commands below.

---

## 3. Quickstart: Run in One Command

### Step 3.1: Configure Environment (Optional)
Copy the template environment file:
```powershell
cp .env.docker.example .env
```
*(The default configuration works out of the box with safe, sensible fallbacks).*

### Step 3.2: Build and Start Containers
From the root directory (`d:\blood-donor-system`), execute:
```powershell
docker compose up -d --build
```

Docker will:
1. Initialize the `lifelink-network` bridge and `lifelink_mysql_data` volume.
2. Spin up MySQL 8.0 and wait for healthy status.
3. Build the Spring Boot backend inside a JDK container and launch the slim runtime.
4. Build the React frontend production bundle and start Nginx.

---

## 4. Verification & Access Endpoints

Once containers are active, access the services:

| Application Layer | URL | Credentials / Notes |
| :--- | :--- | :--- |
| **LifeLink Web Portal** | [http://localhost](http://localhost) | Main donor & hospital web interface (Port 80) |
| **Swagger UI Documentation** | [http://localhost/swagger-ui.html](http://localhost/swagger-ui.html) | Interactive REST API explorer (via Nginx proxy) |
| **Direct Backend API** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Direct Spring Boot port (for debugging) |
| **MySQL Database** | `localhost:3306` | User: `root`, Password: `${DB_ROOT_PASSWORD}` (default: `rootpassword`) |

---

## 5. Day-to-Day Operations & Management

### Check Container Status
```powershell
docker compose ps
```
You should see all three containers (`lifelink-mysql`, `lifelink-backend`, `lifelink-frontend`) with status `Up (healthy)`.

### View Live Container Logs
```powershell
# View all logs in real time
docker compose logs -f

# View only backend Spring Boot logs
docker compose logs -f backend

# View only frontend Nginx logs
docker compose logs -f frontend

# View only MySQL database logs
docker compose logs -f mysql
```

### Restart a Specific Service
```powershell
docker compose restart backend
```

### Rebuild After Code Changes
When you make changes to backend Java code or frontend React components:
```powershell
# Rebuild only the modified service
docker compose up -d --build backend
docker compose up -d --build frontend
```

### Stop Containers (Data Preserved)
```powershell
docker compose down
```
> [!TIP]
> Stopping containers preserves all database records, registered donors, and hospital profiles inside the `lifelink_mysql_data` volume.

### Complete Reset (Wipe Database)
If you want to completely clear the database and start fresh:
```powershell
docker compose down -v
```

---

## 6. Port Customization & Collision Avoidance

If ports `80`, `8080`, or `3306` are already in use by local services on your host machine (e.g. IIS, local MySQL, or Tomcat), update `.env`:

```env
# Map host ports to avoid conflicts:
FRONTEND_PORT=3000   # LifeLink UI now at http://localhost:3000
BACKEND_PORT=8081    # Direct backend at http://localhost:8081
MYSQL_PORT=3307      # MySQL reachable at localhost:3307
```

Then re-apply:
```powershell
docker compose up -d
```

---

## 7. Database Backup & Restore in Docker

### Create a Database Backup
```powershell
docker exec lifelink-mysql mysqldump -u root -prootpassword blood_donor_system > backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql
```

### Restore Database from Backup
```powershell
Get-Content backup.sql | docker exec -i lifelink-mysql mysql -u root -prootpassword blood_donor_system
```

---

## 8. Production Deployment Recommendations

For production deployments (AWS ECS, Google Cloud Run, Azure Container Apps, or Kubernetes):
1. **Secrets Management**: Inject `JWT_SECRET`, `DB_PASSWORD`, and `DB_ROOT_PASSWORD` via environment secret managers (e.g. AWS Secrets Manager, HashiCorp Vault).
2. **Database Auto-DDL**: Change `JPA_DDL_AUTO=validate` in production to prevent schema mutations at runtime.
3. **TLS/SSL Termination**: Terminate HTTPS at the cloud load balancer (e.g., AWS ALB, Cloudflare, Traefik).
4. **CI/CD Integration**: Incorporate `docker build -t your-org/lifelink-backend:latest ./blood-donor-system` and push to Docker Hub, GitHub Container Registry (GHCR), or AWS ECR.
