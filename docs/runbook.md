# Operational Runbook

## Table of Contents
1. [System Overview](#system-overview)
2. [Access and Credentials](#access-and-credentials)
3. [Common Operations](#common-operations)
4. [Troubleshooting](#troubleshooting)
5. [Backup and Restore](#backup-and-restore)
6. [Deployment](#deployment)
7. [Monitoring](#monitoring)
8. [Scaling](#scaling)

## System Overview

### Architecture
- Single EC2 instance (t3.micro or t4g.micro) in ap-south-1
- Ubuntu 22.04 LTS
- Java 17 Spring Boot application
- PostgreSQL 14+ database
- Nginx reverse proxy
- CloudWatch monitoring

### Services
- `myapp.service` - Spring Boot application
- `postgresql.service` - PostgreSQL database
- `nginx.service` - Nginx web server
- `amazon-cloudwatch-agent.service` - CloudWatch agent

## Access and Credentials

### SSH Access
```bash
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP>
```

### Database Access
```bash
# As postgres user
sudo -u postgres psql

# As application user
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost
```

### Important Files
- Application JAR: `/home/appuser/app/myapp.jar`
- Application config: `/home/appuser/app/application.yml`
- Nginx config: `/etc/nginx/sites-available/mywebsite`
- Systemd service: `/etc/systemd/system/myapp.service`
- Application logs: `/var/log/myapp/application.log`
- Backup script: `/usr/local/bin/pg_backup_to_s3.sh`

## Common Operations

### Check Service Status

```bash
# Check all services
sudo systemctl status myapp
sudo systemctl status postgresql
sudo systemctl status nginx
sudo systemctl status amazon-cloudwatch-agent

# Quick health check
curl http://localhost/api/health
```

### Start/Stop/Restart Services

```bash
# Application
sudo systemctl start myapp
sudo systemctl stop myapp
sudo systemctl restart myapp

# PostgreSQL
sudo systemctl start postgresql
sudo systemctl stop postgresql
sudo systemctl restart postgresql

# Nginx
sudo systemctl start nginx
sudo systemctl stop nginx
sudo systemctl restart nginx
sudo nginx -t  # Test configuration before restart
```

### View Logs

```bash
# Application logs (systemd)
journalctl -u myapp -f
journalctl -u myapp -n 100
journalctl -u myapp --since "1 hour ago"

# Application logs (file)
tail -f /var/log/myapp/application.log
tail -f /var/log/myapp/error.log

# Nginx logs
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log

# PostgreSQL logs
sudo tail -f /var/log/postgresql/postgresql-*-main.log

# System logs
journalctl -xe
dmesg | tail
```

### Database Operations

```bash
# Connect to database
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost

# List tables
\dt

# Show table schema
\d entries

# Count entries
SELECT COUNT(*) FROM entries;

# Check recent entries
SELECT id, user_id, updated_at FROM entries ORDER BY updated_at DESC LIMIT 10;

# Check database size
SELECT pg_size_pretty(pg_database_size('myappdb'));

# Vacuum database (cleanup)
VACUUM ANALYZE;
```

## Troubleshooting

### Application Won't Start

**Symptoms:** `systemctl status myapp` shows failed state

**Diagnosis:**
```bash
# Check logs
journalctl -u myapp -n 50

# Common issues:
# 1. Database connection failure
# 2. Port 8080 already in use
# 3. Missing JAR file
# 4. Incorrect permissions
```

**Solutions:**
```bash
# Check if database is running
sudo systemctl status postgresql

# Check if port is in use
sudo netstat -tlnp | grep 8080

# Check JAR file exists
ls -la /home/appuser/app/myapp.jar

# Check permissions
sudo chown appuser:appuser /home/appuser/app/myapp.jar

# Check database connectivity
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -c "SELECT 1;"
```

### High CPU Usage

**Diagnosis:**
```bash
# Check CPU usage
top
htop

# Check which process
ps aux --sort=-%cpu | head -10

# Check application threads
jstack <PID>
```

**Solutions:**
```bash
# Restart application
sudo systemctl restart myapp

# Check for slow queries
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -c "
SELECT pid, now() - query_start as duration, query 
FROM pg_stat_activity 
WHERE state = 'active' 
ORDER BY duration DESC;
"

# If persistent, consider upgrading instance type
```

### High Memory Usage

**Diagnosis:**
```bash
# Check memory
free -h
vmstat 1

# Check Java heap usage
jstat -gc <PID>
```

**Solutions:**
```bash
# Adjust Java heap in systemd service
sudo nano /etc/systemd/system/myapp.service
# Change: -Xms256m -Xmx512m to appropriate values

sudo systemctl daemon-reload
sudo systemctl restart myapp
```

### Disk Space Full

**Diagnosis:**
```bash
# Check disk usage
df -h

# Find large files
du -sh /* | sort -h
du -sh /var/log/* | sort -h
```

**Solutions:**
```bash
# Clean old logs
sudo journalctl --vacuum-time=7d
sudo find /var/log -name "*.gz" -mtime +30 -delete

# Clean old backups (if stored locally)
sudo find /tmp -name "*.sql.gz" -mtime +7 -delete

# Vacuum database
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -c "VACUUM FULL;"
```

### Database Connection Errors

**Symptoms:** Application logs show "Connection refused" or "Connection timeout"

**Diagnosis:**
```bash
# Check if PostgreSQL is running
sudo systemctl status postgresql

# Check PostgreSQL is listening
sudo netstat -tlnp | grep 5432

# Check connection from localhost
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -c "SELECT 1;"
```

**Solutions:**
```bash
# Restart PostgreSQL
sudo systemctl restart postgresql

# Check PostgreSQL logs
sudo tail -f /var/log/postgresql/postgresql-*-main.log

# Check max connections
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -c "
SHOW max_connections;
SELECT count(*) FROM pg_stat_activity;
"
```

### Nginx 502 Bad Gateway

**Symptoms:** API returns 502 error

**Diagnosis:**
```bash
# Check if application is running
sudo systemctl status myapp
curl http://localhost:8080/api/health

# Check Nginx error logs
sudo tail -f /var/log/nginx/error.log
```

**Solutions:**
```bash
# Restart application
sudo systemctl restart myapp

# Wait for application to start
sleep 5

# Test again
curl http://localhost/api/health
```

### SSL Certificate Issues

**Symptoms:** HTTPS not working or certificate expired

**Diagnosis:**
```bash
# Check certificate expiry
sudo certbot certificates

# Test SSL
curl -I https://yourdomain.com
```

**Solutions:**
```bash
# Renew certificate
sudo certbot renew

# Force renewal (if close to expiry)
sudo certbot renew --force-renewal

# Restart Nginx
sudo systemctl restart nginx
```

## Backup and Restore

### Manual Backup

```bash
# Run backup script manually
sudo /usr/local/bin/pg_backup_to_s3.sh

# Check backup log
tail /var/log/pg_backup.log

# Verify backup in S3
aws s3 ls s3://<S3_BUCKET>/backups/ --region ap-south-1
```

### Restore from Backup

```bash
# 1. Download backup from S3
aws s3 cp s3://<S3_BUCKET>/backups/myappdb-2025-11-10T020000Z.sql.gz /tmp/ --region ap-south-1

# 2. Decompress
gunzip /tmp/myappdb-2025-11-10T020000Z.sql.gz

# 3. Stop application
sudo systemctl stop myapp

# 4. Drop and recreate database (CAUTION!)
sudo -u postgres psql -c "DROP DATABASE myappdb;"
sudo -u postgres psql -c "CREATE DATABASE myappdb OWNER myappuser;"

# 5. Restore
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -f /tmp/myappdb-2025-11-10T020000Z.sql

# 6. Start application
sudo systemctl start myapp

# 7. Verify
curl http://localhost/api/health

# 8. Cleanup
rm /tmp/myappdb-2025-11-10T020000Z.sql
```

### Backup Schedule

- Automated daily backups at 2:30 AM UTC
- Cron job: `30 2 * * * /usr/local/bin/pg_backup_to_s3.sh`
- Check cron: `crontab -l`

## Deployment

### Standard Deployment

```bash
# 1. Build locally
cd myapp
mvn clean package -DskipTests

# 2. Run deployment script
cd ..
./scripts/deploy.sh

# 3. Monitor deployment
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> "journalctl -u myapp -f"
```

### Manual Deployment

```bash
# 1. Backup current JAR
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> \
  "cp /home/appuser/app/myapp.jar /home/appuser/app/myapp.jar.backup"

# 2. Upload new JAR
scp -i ~/.ssh/myapp-key.pem target/myapp-0.0.1-SNAPSHOT.jar \
  ubuntu@<ELASTIC_IP>:/home/appuser/app/myapp.jar

# 3. Restart service
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> \
  "sudo systemctl restart myapp.service"

# 4. Check status
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> \
  "sudo systemctl status myapp.service"

# 5. Verify health
curl http://<ELASTIC_IP>/api/health
```

### Rollback

```bash
# SSH into server
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP>

# Stop service
sudo systemctl stop myapp

# Restore backup
cp /home/appuser/app/myapp.jar.backup /home/appuser/app/myapp.jar

# Start service
sudo systemctl start myapp

# Verify
curl http://localhost/api/health
```

### Database Migrations

```bash
# 1. Backup database before migration
sudo /usr/local/bin/pg_backup_to_s3.sh

# 2. Run migration script
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -f migration.sql

# 3. Verify migration
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -c "\dt"

# 4. Deploy new application version
./scripts/deploy.sh
```

## Monitoring

### CloudWatch Metrics

Access CloudWatch console:
- Region: ap-south-1
- Namespace: EC2/AppInstance
- Metrics: CPU, Memory, Disk usage

### Key Metrics to Monitor

1. **CPU Utilization** - Should be < 70%
2. **Memory Usage** - Should be < 80%
3. **Disk Usage** - Should be < 80%
4. **Application Response Time** - Should be < 500ms
5. **Database Connections** - Should be < 80% of max

### Set Up Alerts

```bash
# CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name high-cpu \
  --alarm-description "CPU usage > 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --region ap-south-1

# Disk alarm
aws cloudwatch put-metric-alarm \
  --alarm-name high-disk \
  --alarm-description "Disk usage > 80%" \
  --metric-name DISK_USED \
  --namespace EC2/AppInstance \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --region ap-south-1
```

### Health Checks

```bash
# Create health check script
cat > /usr/local/bin/health-check.sh <<'EOF'
#!/bin/bash
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/api/health)
if [ "$HEALTH" != "200" ]; then
  echo "Health check failed: $HEALTH"
  # Send alert (e.g., via SNS)
  exit 1
fi
echo "Health check passed"
EOF

chmod +x /usr/local/bin/health-check.sh

# Add to cron (every 5 minutes)
(crontab -l; echo "*/5 * * * * /usr/local/bin/health-check.sh >> /var/log/health-check.log 2>&1") | crontab -
```

## Scaling

### Vertical Scaling (Upgrade Instance)

```bash
# 1. Stop instance
aws ec2 stop-instances --instance-ids <INSTANCE_ID> --region ap-south-1

# 2. Wait for stopped state
aws ec2 wait instance-stopped --instance-ids <INSTANCE_ID> --region ap-south-1

# 3. Change instance type
aws ec2 modify-instance-attribute \
  --instance-id <INSTANCE_ID> \
  --instance-type t3.small \
  --region ap-south-1

# 4. Start instance
aws ec2 start-instances --instance-ids <INSTANCE_ID> --region ap-south-1

# 5. Verify services
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> "sudo systemctl status myapp"
```

### Horizontal Scaling (Future)

When single instance is insufficient:
1. Migrate database to RDS
2. Set up Application Load Balancer
3. Create Auto Scaling Group
4. Deploy application to multiple instances
5. Implement session management (Redis)

## Emergency Contacts

- AWS Support: [AWS Console](https://console.aws.amazon.com/support/)
- Database Admin: <CONTACT>
- DevOps Team: <CONTACT>
- On-call Engineer: <CONTACT>

## Useful Commands Reference

```bash
# System info
uname -a
lsb_release -a
df -h
free -h
uptime

# Network
netstat -tlnp
ss -tlnp
curl -I http://localhost/api/health

# Processes
ps aux | grep java
ps aux | grep postgres
ps aux | grep nginx

# Logs
journalctl -u myapp --since today
tail -f /var/log/syslog
dmesg | tail

# AWS CLI
aws ec2 describe-instances --region ap-south-1
aws s3 ls s3://<S3_BUCKET>/backups/
aws cloudwatch get-metric-statistics --region ap-south-1
```
