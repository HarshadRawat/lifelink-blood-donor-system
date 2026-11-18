# LifeLink — k6 High-Concurrency Load Testing Suite

This directory contains the production-grade **k6** load testing scripts for LifeLink, designed to stress-test high-traffic API flows under concurrent virtual user (VU) scenarios.

---

## 1. Prerequisites & Installation

### Install k6
* **Windows (winget)**:
  ```powershell
  winget install k6 --source winget
  ```
* **Windows (Chocolatey)**:
  ```powershell
  choco install k6
  ```
* **macOS (Homebrew)**:
  ```bash
  brew install k6
  ```
* **Linux (Debian/Ubuntu)**:
  ```bash
  sudo gpg -k
  sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
  echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
  sudo apt-get update
  sudo apt-get install k6
  ```
* **Docker**:
  ```bash
  docker run --rm -i grafana/k6 run - <load-tests/search-matching.js
  ```

---

## 2. Load Test Scenarios

| Script | Purpose | Virtual Users (VUs) | Duration | SLA Threshold |
| :--- | :--- | :--- | :--- | :--- |
| [`search-matching.js`](search-matching.js) | Geolocation bounding-box & candidate donor matching | 50 VUs | 60s | $p(95) < 300\text{ ms}$, failures $< 1\%$ |
| [`login.js`](login.js) | Authentication, BCrypt cost factor, JWT generation & refresh token storage | Ramps to 50 VUs | 60s | $p(95) < 400\text{ ms}$, failures $< 2\%$ |
| [`create-request.js`](create-request.js) | Emergency request broadcast and validation under contention | Ramps to 50 VUs | 60s | $p(95) < 300\text{ ms}$, failures $< 2\%$ |
| [`full-lifecycle.js`](full-lifecycle.js) | Composite flow: login $\rightarrow$ broadcast $\rightarrow$ match lookup | Ramps to 30 VUs | 50s | $p(95) < 350\text{ ms}$, failures $< 2\%$ |

---

## 3. Running the Tests

### 3.1 Geolocation Matching Benchmark (Task 10 Core)
```powershell
k6 run `
  -e BASE="http://localhost:8080" `
  -e TOKEN="<YOUR_JWT_TOKEN>" `
  load-tests/search-matching.js
```

### 3.2 Authentication & Login Throughput
```powershell
k6 run `
  -e BASE="http://localhost:8080" `
  -e LOGIN_EMAIL="admin@lifelink.com" `
  -e LOGIN_PASSWORD="Admin123" `
  load-tests/login.js
```

### 3.3 Emergency Blood Request Creation
```powershell
k6 run `
  -e BASE="http://localhost:8080" `
  -e TOKEN="<YOUR_HOSPITAL_JWT>" `
  load-tests/create-request.js
```

### 3.4 Full Lifecycle Workflow
```powershell
k6 run `
  -e BASE="http://localhost:8080" `
  load-tests/full-lifecycle.js
```

---

## 4. Live Metrics Scraped via Prometheus & Grafana

When running the stack with `docker-compose.monitoring.yml`, Prometheus scrapes Spring Boot Micrometer metrics (`/actuator/prometheus`) and k6 can optionally stream real-time test telemetry:

```powershell
# Start monitoring stack
docker compose -f docker-compose.monitoring.yml up -d

# Open Grafana
# URL: http://localhost:3001 (User: admin, Pass: admin)
```

The pre-configured dashboard **LifeLink System Metrics Dashboard** automatically displays:
* HTTP Throughput (req/s by response status)
* 95th & 99th percentile response latencies ($p95$, $p99$)
* JVM Heap & Non-Heap Memory Allocation
* CPU Usage (JVM process vs. host system)
