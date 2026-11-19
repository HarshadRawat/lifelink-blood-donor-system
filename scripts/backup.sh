#!/usr/bin/env bash
# ====================================================================
# LifeLink - Automated Production Database Backup Script
# Performs non-blocking mysqldump of blood_donor_system with gzip compression
# Retains rolling 7-day backups locally
# ====================================================================
set -euo pipefail

BACKUP_DIR="backups"
TIMESTAMP="$(date +%F_%H%M%S)"
TARGET_FILE="${BACKUP_DIR}/lifelink_${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Starting LifeLink MySQL database backup..."

# Execute mysqldump with --single-transaction for transactional InnoDB consistency without locking tables
docker compose exec -T mysql sh -c \
  'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --events blood_donor_system' \
  | gzip -9 > "${TARGET_FILE}"

# Verify backup was created and is non-empty
if [ -s "${TARGET_FILE}" ]; then
  FILE_SIZE="$(du -h "${TARGET_FILE}" | cut -f1)"
  echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Backup successfully created: ${TARGET_FILE} (${FILE_SIZE})"
else
  echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] ERROR: Backup file ${TARGET_FILE} is empty or failed!" >&2
  exit 1
fi

# Prune rolling backups older than 7 days
echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Pruning backups older than 7 days..."
find "${BACKUP_DIR}" -name 'lifelink_*.sql.gz' -mtime +7 -delete

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Backup operation complete."
