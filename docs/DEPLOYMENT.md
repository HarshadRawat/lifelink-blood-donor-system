# LifeLink — Production Deployment & Live Demo Guide

This guide details the deployment options, environment variables, security configurations, demo data seeding, and monitoring strategies for LifeLink.

---

## 1. Deployment Architecture Overview

LifeLink supports two production deployment models:

```mermaid
flowchart TD
    subgraph OptionA ["Model A: Single-Host Unified VM (Recommended)"]
        UserA["Browser Client (HTTPS / WSS)"] --> NginxA["Nginx 1.27 + Let's Encrypt SSL (Port 443)"]
        NginxA -->|Static SPA /| FrontA["React 19 Frontend"]
        NginxA -->|Proxy /api, /actuator| BackA["Spring Boot 4 Monolith (Port 8080)"]
        NginxA -->|Proxy /ws (HTTP Upgrade)| BackA
        BackA --> DB_A[("MySQL 8.0 (Port 3306)")]
    end

    subgraph OptionB ["Model B: Decoupled PaaS Architecture"]
        UserB["Browser Client"] --> Vercel["Vercel / Netlify (Frontend CDN)"]
        Vercel -->|REST HTTPS & WSS| Render["Render / Railway (Spring Boot Container)"]
        Render --> CloudDB[("Aiven / Railway Cloud MySQL 8.0")]
    end
```

---

## 2. Option A: Cloud VM Deployment (AWS EC2 / Oracle Cloud / DigitalOcean)

This is the simplest, most robust path for a student or enterprise deployment because the entire stack runs in an isolated Docker network behind Nginx, guaranteeing that WebSockets, REST APIs, and static assets work seamlessly on a single domain.

### Step 1: Provision Cloud VM & Install Docker
Provision an Ubuntu 22.04 / 24.04 LTS instance (e.g., AWS t3.small, Oracle Cloud Always-Free ARM/AMD, or DigitalOcean Droplet) and install Docker:

```bash
sudo apt update && sudo apt install -y docker.io docker-compose-plugin git certbot
sudo usermod -aG docker $USER
```

### Step 2: Clone Repository & Create `.env`
```bash
git clone https://github.com/HarshadRawat/lifelink-blood-donor-system.git
cd lifelink-blood-donor-system

# Create secure production environment file
cat << 'EOF' > .env
PORT=8080
FRONTEND_PORT=80
MYSQL_PORT=3307

# Spring Profiles: 'prod' activates strict validation; 'demo' seeds sample users
SPRING_PROFILES_ACTIVE=prod,demo

# MySQL Database Credentials
DB_ROOT_PASSWORD=replace_with_strong_root_password_32char
DB_USER=lifelink_prod_user
DB_PASSWORD=replace_with_strong_user_password_32char
DB_URL=jdbc:mysql://mysql:3306/blood_donor_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=root

# Hibernate & Flyway
JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
FLYWAY_BASELINE_ON_MIGRATE=false

# Cryptographic Keys (Generate with: openssl rand -base64 48)
JWT_SECRET=replace_with_a_64_character_random_hex_or_base64_secret_key!
JWT_EXPIRATION_MS=86400000

# Rate Limiting & Proxies
RATE_LIMIT_ENABLED=true
RATE_LIMIT_LOGIN_MAX=10
RATE_LIMIT_LOGIN_WINDOW=60
RATE_LIMIT_TRUSTED_PROXIES=127.0.0.1,::1,172.16.0.0/12

# Admin Initial Seed
ADMIN_EMAIL=admin@lifelink.com
ADMIN_PASSWORD=replace_with_strong_admin_password

# Demo Seeding
DEMO_SEED_ENABLED=true
EOF
```

### Step 3: Launch Services with Docker Compose
```bash
docker compose up -d --build
```

### Step 4: Configure HTTPS & SSL (Certbot / Nginx Host Proxy)
Point your DNS `A` record (e.g., `lifelink.example.com`) to your server IP. Install Nginx and obtain a Let's Encrypt certificate:

```bash
sudo apt install -y nginx python3-certbot-nginx
sudo certbot --nginx -d lifelink.example.com
```

Add the following reverse-proxy configuration inside `/etc/nginx/sites-available/lifelink`:

```nginx
server {
    server_name lifelink.example.com;

    location / {
        proxy_pass http://127.0.0.1:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }

    # Sustained WebSocket STOMP Connection
    location /ws {
        proxy_pass http://127.0.0.1:80;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_read_timeout 86400s;
        proxy_send_timeout 86400s;
    }

    listen 443 ssl;
    # Managed by Certbot SSL certificates...
}
```

Reload Nginx:
```bash
sudo nginx -s reload
```

---

## 3. Option B: Cloud PaaS Deployment (Render + Vercel)

### 1. Backend + Managed Database on Render
1. Fork or push this repository to GitHub.
2. In Render, select **Blueprints** -> **New Blueprint Instance**.
3. Point to this repo. Render automatically parses [`render.yaml`](../render.yaml), provisioning:
   * A managed MySQL database (`blood_donor_system`).
   * A containerized web service for `lifelink-backend`.
4. Ensure `SPRING_PROFILES_ACTIVE=prod,demo` and set `CORS_ALLOWED_ORIGINS` to include your Vercel frontend URL (e.g., `https://lifelink.vercel.app`).

### 2. Frontend on Vercel
1. Import the repository in [Vercel](https://vercel.com).
2. Set **Root Directory** to `blood-donor-system/frontend`.
3. Framework preset: **Vite**.
4. Set Environment Variable:
   * `VITE_API_BASE_URL`: `https://lifelink-backend.onrender.com` (your Render backend URL).
5. Deploy. Vercel automatically respects [`vercel.json`](../blood-donor-system/frontend/vercel.json) to handle SPA client-side route rewrites.

---

## 4. Pre-Configured Demo Credentials (Bengaluru Cluster)

When running with profile `demo` or `DEMO_SEED_ENABLED=true`, LifeLink automatically seeds a realistic geographic cluster in **Bengaluru (Koramangala)** to enable full matching, privacy, and notification testing in 30 seconds:

| Role | Email | Password | Details |
| :--- | :--- | :--- | :--- |
| 🏥 **Hospital** | `hospital@lifelink.com` | `Hospital123!` | **City Care Hospital** (Koramangala, 12.9352, 77.6245). Has an active CRITICAL request for A+ blood. |
| 🩸 **Donor 1 (Matched)** | `alice.donor@lifelink.com` | `Donor123!` | **Alice Sharma** (A+, 12.9380, 77.6290). Located **0.5 km** from hospital. Matched and pending consent. |
| 🩸 **Donor 2 (Accepted)** | `bob.donor@lifelink.com` | `Donor123!` | **Bob Rao** (O- universal donor, 12.9400, 77.6200). Located **0.8 km** away. Accepted with contact sharing consent. |
| 🩸 **Donor 3 (Excluded - ABO)** | `charlie.donor@lifelink.com` | `Donor123!` | **Charlie Kumar** (B+, 12.9250, 77.5850). ~4.5 km away, excluded due to ABO incompatibility. |
| 🩸 **Donor 4 (Excluded - ABO)** | `david.donor@lifelink.com` | `Donor123!` | **David Fernandes** (AB+, 12.9780, 77.6400). ~6.5 km away, AB+ universal recipient. |
| 🩸 **Donor 5 (Excluded - Distance)** | `emma.donor@lifelink.com` | `Donor123!` | **Emma Watson** (A+, 13.3400, 77.1000). Located in Tumkur (**~60 km** away, outside the 20 km radius). |
| 🛡️ **Admin** | `admin@lifelink.com` | `Admin123` | System oversight, metrics, and user management. |

> [!NOTE]
> Demo accounts can also be instantly auto-filled on the login screen using the **⚡ Quick Demo Logins** button panel.

---

## 5. Uptime Monitoring & Free Host Sleep Prevention

Free cloud tiers (Render, Railway, Fly.io) put containers to sleep after 15 minutes of inactivity. To keep the service hot and responsive for recruiters:

1. Create a free account at [UptimeRobot](https://uptimerobot.com) or [Cron-job.org](https://cron-job.org).
2. Create an **HTTP Monitor**:
   * **URL**: `https://<your-backend-host>/actuator/health`
   * **Monitoring Interval**: Every `5 minutes`
   * **Expected Response Code**: `200 OK`
3. This sends a lightweight request to the Spring Boot Actuator probe every 5 minutes, preventing container sleep without degrading database quotas.

---

## 6. End-to-End Smoke Test Checklist

Once deployed, execute this 2-minute mobile verification flow:
1. Open your smartphone browser on mobile data (independent cellular network).
2. Visit `https://<deployed-site>/login`.
3. Click **🏥 Hospital** under Quick Demo Logins and sign in.
4. Verify that the active emergency request for A+ blood appears with 2 matched donors.
5. In a second incognito window or separate device, log in as **🩸 Donor A+** (`alice.donor@lifelink.com`).
6. Click **Accept Match** and toggle **Consent to share contact details**.
7. In the Hospital window, immediately observe:
   - WebSocket instant toast alert notification.
   - Alice's contact phone number (`+91-9876543210`) unlocked and visible.
8. Click **Mark Fulfilled** as Hospital. Verify the request status updates to `FULFILLED` across both screens.
