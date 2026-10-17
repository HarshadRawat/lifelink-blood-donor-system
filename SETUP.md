# LifeLink — Developer Setup & Run Guide (Windows & Cross-Platform)

This guide walks you through setting up and running **LifeLink** from scratch on a clean machine.

> [!TIP]
> **Prefer running with Docker?** You can skip manual JDK, Node.js, and MySQL installations entirely! Refer to [DOCKER.md](DOCKER.md) to run LifeLink using `docker compose up -d --build`.

---

## 1. Prerequisites & Version Requirements

Before starting, ensure the following software is installed on your system:

| Tool | Required Version | Verification Command | Notes |
| :--- | :--- | :--- | :--- |
| **Java JDK** | **JDK 17 or higher** | `java -version` | Eclipse Temurin 17+, Amazon Corretto 17+, or OpenJDK |
| **Node.js** | **v18.0.0 or higher** | `node -v` | LTS v20+ recommended |
| **npm** | **v9.0.0 or higher** | `npm -v` | Installed with Node.js |
| **MySQL Server** | **v8.0 or higher** | `mysql --version` | Community Server 8.0+ |
| **Git** | **v2.30+** | `git --version` | For version control |

### Setting `JAVA_HOME` (Windows PowerShell)
Ensure your `JAVA_HOME` environment variable points to a valid JDK 17+ installation:
```powershell
# Verify current setting
echo $env:JAVA_HOME

# If not set, configure it for your session:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"  # Adjust path to your JDK install
```

---

## 2. Database Setup (MySQL)

LifeLink uses MySQL 8.0+ with the InnoDB storage engine.

### Step 2.1: Create the Database
Log into your MySQL command-line client or MySQL Workbench:
```powershell
mysql -u root -p
```
Run the following SQL statement to create the application database:
```sql
CREATE DATABASE blood_donor_system 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

### Step 2.2: Schema Initialization
* **Automatic JPA DDL**: You do **not** need to manually run table creation scripts. LifeLink uses Hibernate's `ddl-auto=update` mechanism. Upon starting the backend for the first time, Spring Boot automatically creates all required tables (`users`, `donor_profiles`, `hospital_profiles`, `emergency_requests`, `request_donor_matches`, `notifications`).

---

## 3. Backend Setup & Startup (Spring Boot)

### Step 3.1: Configure Database Credentials
You can provide database credentials via environment variables or by modifying `application.properties`.

**Option A: Using PowerShell Environment Variables (Recommended)**
```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/blood_donor_system"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password_here"
```

**Option B: Direct Configuration in `application.properties`**
Open `blood-donor-system/src/main/resources/application.properties` and verify:
```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/blood_donor_system}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:your_mysql_password_here}
```

### Step 3.2: Compile and Run Tests
Open a PowerShell terminal in the project directory:
```powershell
cd d:\blood-donor-system\blood-donor-system

# Run the full test suite (132 tests)
.\mvnw.cmd test
```
All 132 tests should pass with `BUILD SUCCESS`.

### Step 3.3: Start the Backend Server
```powershell
.\mvnw.cmd spring-boot:run
```
When startup completes, you will see output similar to:
```
Started BloodDonorSystemApplication in 3.4 seconds (process running for 3.9)
```
The backend is now live at **`http://localhost:8080`**.

### Step 3.4: Verify Backend Health
Open your browser and navigate to:
* **Swagger UI Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* You should see interactive documentation for all Auth, Donor, Hospital, Emergency Request, and Notification endpoints.

---

## 4. Frontend Setup & Startup (React + Vite)

### Step 4.1: Install Dependencies
Open a **second** terminal window:
```powershell
cd d:\blood-donor-system\blood-donor-system\frontend

# Install dependencies using committed lockfile
npm install
```

### Step 4.2: Configure Environment
Copy the example environment file:
```powershell
cp .env.example .env
```
*(Leave `VITE_API_BASE_URL=` empty for local development so Vite's built-in proxy handles `/api` and `/ws` requests without CORS hurdles).*

### Step 4.3: Start Vite Development Server
```powershell
npm run dev
```
You will see:
```
  VITE v8.3.4  ready in 180 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

### Step 4.4: Verify Production Build
To verify that the frontend bundles cleanly without errors:
```powershell
npm run build
```
This builds optimized client artifacts to `dist/` in under 500 ms.

---

## 5. End-to-End Verification Flow

Once both the backend (`:8080`) and frontend (`:5173`) are running:

1. Open **`http://localhost:5173`** in your browser.
2. **Register a Hospital**:
   * Click **Register** $\rightarrow$ select role **Hospital**.
   * Fill out the hospital registration form and log in.
   * On the Hospital Dashboard, click **Set Up Hospital Profile** (use the one-click **Set Demo Location** button to populate Bangalore coordinates).
3. **Register a Donor** (in an incognito/private browser tab):
   * Click **Register** $\rightarrow$ select role **Donor**.
   * Fill out the donor profile with blood group `O+` and click **Set Demo Location**.
   * Verify that **Available to Donate** is toggled **ON**.
4. **Issue an Emergency Request**:
   * Switch back to the hospital tab.
   * Click **New Emergency Request** $\rightarrow$ select `O+`, units `2`, urgency `CRITICAL` $\rightarrow$ click **Broadcast Request**.
   * Click **Find Matches**.
5. **Verify Real-time Donor Alert & Acceptance**:
   * In the donor tab, notice the instant notification badge.
   * Under **Emergency Donation Matches**, the request appears.
   * Click **Accept**: an informed consent dialog appears explaining that phone and email will be shared with the demanding hospital.
   * Check the consent checkbox and confirm.
6. **Verify Contact Sharing on Hospital Portal**:
   * In the hospital tab, open **View Matches**.
   * The accepted donor now displays a **Verified Donor Consent** badge with actionable **Call (`tel:`)** and **Email (`mailto:`)** buttons.

---

## 6. Common Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| `CommunicationsException: Communications link failure` | MySQL is not running or incorrect port | Check MySQL service in Windows Services (`services.msc`) or verify port 3306. |
| `Access denied for user 'root'@'localhost'` | Wrong database password | Ensure `$env:DB_PASSWORD` matches your local MySQL root password. |
| `Port 8080 was already in use` | Another process is holding port 8080 | Run `Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess \| Stop-Process -Force` or pass `-Dserver.port=8081`. |
| `Port 5173 was already in use` | Vite dev server already running | Vite will offer port 5174 automatically, or run `npm run dev -- --port 5174`. |
| `Failed to fetch` on register/login | Backend is not started or URL mismatch | Ensure Spring Boot is running on port 8080. If accessing via non-localhost, update `VITE_API_BASE_URL` in `.env`. |
| `403 Forbidden` on WebSocket `/ws` | STOMP connection missing JWT token | Verify that you are logged in; the frontend attaches `Bearer <token>` to STOMP headers automatically. |
