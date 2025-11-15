#!/bin/bash

# Database backup script for EC2
# Usage: ./backup-db-to-s3.sh

set -e

# Configuration
DB_FILE_PATH="${DB_FILE_PATH:-/opt/bakehouse/myapp/data/myapp.mv.db}"
S3_BUCKET="${S3_BUCKET_NAME:-myapp-backups}"
BACKUP_DIR="/tmp"

# Check if database file exists
if [ ! -f "$DB_FILE_PATH" ]; then
    echo "Error: Database file not found at $DB_FILE_PATH"
    exit 1
fi

# Generate backup filename with timestamp
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
BACKUP_FILENAME="myapp-backup-${TIMESTAMP}.db.gz"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILENAME}"

echo "Starting database backup..."
echo "Source: $DB_FILE_PATH"
echo "Backup: $BACKUP_PATH"

# Create compressed backup
gzip -c "$DB_FILE_PATH" > "$BACKUP_PATH"

# Upload to S3
echo "Uploading to S3..."
aws s3 cp "$BACKUP_PATH" "s3://${S3_BUCKET}/backups/${BACKUP_FILENAME}"

# Clean up local backup file
rm "$BACKUP_PATH"

echo "Backup completed successfully!"
echo "Location: s3://${S3_BUCKET}/backups/${BACKUP_FILENAME}"