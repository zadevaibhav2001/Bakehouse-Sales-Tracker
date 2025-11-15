# Database Backup Guide

## Overview

The application provides automated and manual database backup functionality to AWS S3.

## Configuration

### Environment Variables

Set these environment variables on your EC2 instance:

```bash
export S3_BUCKET_NAME=your-backup-bucket-name
export DB_FILE_PATH=/opt/bakehouse/myapp/data/myapp.mv.db
```

### IAM Permissions

Ensure your EC2 instance has an IAM role with these S3 permissions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject"
            ],
            "Resource": "arn:aws:s3:::your-backup-bucket-name/backups/*"
        }
    ]
}
```

## Automated Backups

The application automatically creates daily backups at 5:30 PM UTC using the `ScheduledBackupService`.

## Manual Backups

### Via API

```bash
# Trigger backup via REST API
curl -X POST http://localhost:8080/api/backup/database
```

### Via Shell Script

```bash
# Run manual backup script
./scripts/backup-db-to-s3.sh
```

## Backup Format

- Backups are compressed using GZIP
- Filename format: `myapp-backup-YYYY-MM-DD_HH-MM-SS.db.gz`
- Stored in S3 under `backups/` prefix

## Monitoring

Check application logs for backup status:

```bash
# View backup logs
journalctl -u myapp | grep -i backup

# Or check log file
tail -f /var/log/myapp/application.log | grep -i backup
```

## Troubleshooting

### Common Issues

1. **S3 Access Denied**: Check IAM role permissions
2. **Database File Not Found**: Verify DB_FILE_PATH environment variable
3. **Disk Space**: Ensure sufficient space in /tmp for compression

### Manual Verification

```bash
# List backups in S3
aws s3 ls s3://your-backup-bucket-name/backups/

# Download and verify backup
aws s3 cp s3://your-backup-bucket-name/backups/myapp-backup-2024-01-01_02-30-00.db.gz /tmp/
gunzip /tmp/myapp-backup-2024-01-01_02-30-00.db.gz
```