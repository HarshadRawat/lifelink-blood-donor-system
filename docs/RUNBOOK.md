# LifeLink — Operational Runbook & Disaster Recovery

This runbook documents operational procedures for maintaining, backing up, restoring, rotating secrets, and redeploying the LifeLink production environment.

---

## 1. Automated Database Backups

### Backup Script (`scripts/backup.sh`)
LifeLink uses a non-blocking MySQL backup mechanism:
```bash
chmod +x scripts/backup.sh
./scripts/backup.sh
```

The script executes:
- `mysqldump` with `--single-transaction`, `--routines`, and `--triggers` against InnoDB tables to guarantee transactional snapshot consistency without locking write operations.
- Direct gzip stream compression into `backups/lifelink_YYYY-MM-DD_HHMMSS.sql.gz`.
- Integrity verification checking that the backup file is non-empty (`-s`).
- Automated retention cleanup removing backup files older than 7 days.

### Daily Cron Job Setup
On the production VM, install a root cron job to run the backup every night at 02:00 UTC:
```bash
sudo crontab -e
```
Add the following entry:
```cron
0 2 * * * cd /home/ubuntu/lifelink-blood-donor-system && ./scripts/backup.sh >> /var/log/lifelink-backup.log 2>&1
```

For off-site disaster recovery, sync the `backups/` directory to encrypted object storage (e.g., AWS S3, Cloudflare R2):
```bash
aws s3 sync /home/ubuntu/lifelink-blood-donor-system/backups s3://your-company-lifelink-backups/ --sse AES256
```

---

## 2. Database Restore Procedure

> [!CAUTION]
> Restoring a database snapshot will overwrite all current table states. Always take a pre-restore backup before proceeding.

### Step-by-Step Restoration
1. Identify the backup archive to restore:
   ```bash
   ls -lh backups/
   ```

2. Confirm MySQL container is running:
   ```bash
   docker compose ps mysql
   ```

3. Pipe the decompressed SQL snapshot directly into the MySQL container:
   ```bash
   # Gunzip and stream restore directly:
   gunzip < backups/lifelink_2026-11-19_020000.sql.gz | docker compose exec -T mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" blood_donor_system'
   ```

4. Verify restored data and restart backend to reinitialize caches and connections:
   ```bash
   # Query table row counts
   docker compose exec -T mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" -e "SELECT count(*) FROM blood_donor_system.users;"'

   # Restart backend service
   docker compose restart backend
   ```

---

## 3. Secret Rotation Procedures

### A. Rotating JWT Secret
1. Generate a new 48-byte cryptographically secure key:
   ```bash
   openssl rand -base64 48
   ```
2. Update `JWT_SECRET` in `.env`.
3. Perform a zero-downtime rolling restart of the backend:
   ```bash
   docker compose up -d --no-deps backend
   ```
4. *Consequence*: Active access tokens signed with the previous key will expire or be rejected on their next call. Active clients will automatically invoke the refresh token endpoint (`/api/auth/refresh`), which uses database-backed UUID hashes (unaffected by JWT secret rotation), and receive a new access token seamlessly.

### B. Rotating Database Passwords
1. Generate new strong passwords for `DB_PASSWORD` and `DB_ROOT_PASSWORD`.
2. Connect to MySQL container as root and update the user credentials:
   ```bash
   docker compose exec -it mysql mysql -u root -p
   ```
   ```sql
   ALTER USER 'lifelink_user'@'%' IDENTIFIED BY 'NEW_STRONG_PASSWORD';
   FLUSH PRIVILEGES;
   ```
3. Update `DB_PASSWORD` in `.env`.
4. Restart the backend container:
   ```bash
   docker compose up -d --no-deps backend
   ```

### C. Rotating Redis and RabbitMQ Passwords
1. Update `REDIS_PASSWORD` and/or `RABBITMQ_PASSWORD` in `.env`.
2. Recreate Redis / RabbitMQ and Backend containers:
   ```bash
   docker compose up -d redis rabbitmq backend
   ```

---

## 4. Zero-Downtime Deployment & Application Redeployment

When deploying code updates from GitHub:

```bash
# 1. Pull latest verified commits from main
git pull origin main

# 2. Build updated images without taking the stack offline
docker compose build backend frontend

# 3. Apply rolling container recreation
docker compose up -d --no-deps backend
docker compose up -d --no-deps frontend

# 4. Verify healthy liveness and readiness status
docker compose ps
curl -i http://localhost/health
curl -i http://localhost:8080/actuator/health/liveness
curl -i http://localhost:8080/actuator/health/readiness
```

---

## 5. Troubleshooting & Health Diagnostics

| Symptom | Diagnostic Command | Remediation Action |
| :--- | :--- | :--- |
| **Backend restarting / unhealthy** | `docker compose logs -f backend` | Check database connectivity; verify DB credentials in `.env`; check `/actuator/health/liveness`. |
| **Rate limiter blocking legitimate users** | `docker compose exec redis redis-cli -a "$REDIS_PASSWORD" keys "rl:*"` | Verify Nginx `real_ip` configuration (`set_real_ip_from`) and `ClientIpResolver` CIDR configuration in backend. |
| **WebSocket connection dropping** | `docker compose logs --tail=100 frontend` | Verify Caddy / Nginx `proxy_set_header Upgrade $http_upgrade` and `Connection "upgrade"`. |
| **MySQL disk space warning** | `docker compose exec mysql df -h /var/lib/mysql` | Run `scripts/backup.sh`, copy archive off-host, and prune old binlogs or test data. |
