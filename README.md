# EC2 Backend Infrastructure

A cost-effective, single EC2 instance solution for hosting a Java Spring Boot REST API backend with PostgreSQL database and Nginx reverse proxy. Designed for mobile apps with offline-first capabilities and optimized for AWS Free Tier.

## 🏗️ Architecture

```
Internet
  └─> Route53/DNS → Elastic IP → Nginx (EC2)
        ├─> API (Spring Boot :8080) → PostgreSQL (local)
        └─> Static Website (/var/www/mywebsite)

Mobile Apps ←→ REST API (sync)
Backups: pg_dump → S3
Monitoring: CloudWatch Agent → CloudWatch
```

## 📋 Features

- **Single EC2 Instance**: All components on one instance for minimal cost
- **Spring Boot REST API**: Mobile sync endpoints with conflict resolution
- **PostgreSQL Database**: Local database with automated backups
- **Nginx Reverse Proxy**: Routes API requests and serves static content
- **Automated Backups**: Daily PostgreSQL backups to S3
- **CloudWatch Monitoring**: System metrics and application logs
- **SSL/TLS Support**: Let's Encrypt integration
- **Free Tier Optimized**: Designed to stay within AWS Free Tier limits

## 🚀 Quick Start

### Prerequisites

- AWS Account
- AWS CLI configured
- Maven 3.6+
- Java 17+
- SSH key pair

### 1. Set Up AWS Infrastructure

Follow the guide in `infrastructure/aws-resources-template.md` to create:
- Security Group
- S3 Bucket for backups
- IAM Role for EC2
- Elastic IP
- EC2 Key Pair

### 2. Launch EC2 Instance

1. Edit `infrastructure/ec2-user-data.sh` and replace placeholders:
   - `<DB_PASSWORD>` - Strong database password
   - `<YOUR_DOMAIN>` - Your domain name (optional)
   - `<S3_BUCKET_NAME>` - Your S3 bucket name

2. Launch EC2 instance (t3.micro or t4g.micro) in ap-south-1:
   - AMI: Ubuntu 22.04 LTS
   - Instance type: t3.micro or t4g.micro
   - User data: Paste contents of `ec2-user-data.sh`
   - Security group: Use created security group
   - IAM role: Attach created instance profile

3. Associate Elastic IP with the instance

4. Wait for bootstrap to complete (~5-10 minutes)

### 3. Build and Deploy Application

```bash
# Build the application
cd myapp
mvn clean package

# Edit deployment script
cd ..
nano scripts/deploy.sh
# Replace <EC2_PUBLIC_IP_OR_DOMAIN> with your Elastic IP

# Deploy
./scripts/deploy.sh
```

### 4. Initialize Database Schema

```bash
# SSH into EC2
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP>

# Run database setup
cd /tmp
# Upload schema.sql or run directly
PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -f schema.sql
```

### 5. Verify Deployment

```bash
# Check health endpoint
curl http://<ELASTIC_IP>/api/health

# Test push sync
curl -X POST http://<ELASTIC_IP>/api/sync/push \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "entries": [{
      "id": "test-id-1",
      "payload": "{\"test\":\"data\"}",
      "updatedAt": "2025-11-10T12:00:00Z",
      "deleted": false
    }]
  }'

# Test pull sync
curl "http://<ELASTIC_IP>/api/sync/pull?userId=test-user"
```

### 6. Set Up SSL (Optional)

```bash
# SSH into EC2
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP>

# Run certbot
sudo certbot --nginx -d yourdomain.com

# Verify HTTPS
curl https://yourdomain.com/api/health
```

## 📁 Project Structure

```
.
├── infrastructure/
│   ├── ec2-user-data.sh           # EC2 bootstrap script
│   └── aws-resources-template.md  # AWS setup guide
├── myapp/
│   ├── pom.xml                    # Maven configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/example/myapp/
│       │   │   ├── Application.java
│       │   │   ├── controller/
│       │   │   │   ├── SyncController.java
│       │   │   │   └── GlobalExceptionHandler.java
│       │   │   ├── service/
│       │   │   │   └── SyncService.java
│       │   │   ├── model/
│       │   │   │   └── Entry.java
│       │   │   ├── repository/
│       │   │   │   └── EntryRepository.java
│       │   │   └── dto/
│       │   │       ├── EntryDto.java
│       │   │       ├── PushRequest.java
│       │   │       ├── PushResponse.java
│       │   │       ├── PullResponse.java
│       │   │       └── ConflictRecord.java
│       │   └── resources/
│       │       ├── application.yml
│       │       └── schema.sql
│       └── test/
│           └── java/com/example/myapp/
│               ├── service/
│               │   └── SyncServiceTest.java
│               └── controller/
│                   └── SyncControllerTest.java
├── scripts/
│   ├── deploy.sh                  # Deployment script
│   └── setup-database.sh          # Database initialization
├── docs/
│   ├── mobile-sync-api.md         # API documentation
│   └── runbook.md                 # Operations guide
└── README.md
```

## 🔌 API Endpoints

### Push Sync
```
POST /api/sync/push
```
Push local changes from mobile client to server.

### Pull Sync
```
GET /api/sync/pull?userId={userId}&since={timestamp}
```
Fetch changes from server since a specific timestamp.

### Health Check
```
GET /api/health
```
Check API availability.

See `docs/mobile-sync-api.md` for detailed API documentation.

## 🔄 Sync Strategy

The API implements **Last-Write-Wins (LWW)** conflict resolution:

1. Client sends entries with `updatedAt` timestamps
2. Server compares with existing entries
3. If client timestamp > server timestamp: Accept update
4. If client timestamp < server timestamp: Return conflict
5. Client handles conflicts (accept server version or prompt user)

## 🛠️ Development

### Local Development

```bash
# Start PostgreSQL locally
docker run -d \
  --name postgres \
  -e POSTGRES_DB=myappdb \
  -e POSTGRES_USER=myappuser \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:14

# Run application
cd myapp
export DB_PASSWORD=password
mvn spring-boot:run

# Test endpoints
curl http://localhost:8080/api/health
```

### Run Tests

```bash
cd myapp
mvn test
```

### Build JAR

```bash
cd myapp
mvn clean package
# Output: target/myapp-0.0.1-SNAPSHOT.jar
```

## 📊 Monitoring

### CloudWatch Metrics
- CPU utilization
- Memory usage
- Disk usage
- Application logs
- Nginx logs

### Check Logs

```bash
# Application logs
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> "journalctl -u myapp -f"

# Nginx logs
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> "tail -f /var/log/nginx/access.log"

# Database logs
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> "sudo tail -f /var/log/postgresql/*.log"
```

## 💾 Backup and Restore

### Automated Backups
- Daily backups at 2:30 AM UTC
- Stored in S3 bucket
- Compressed format

### Manual Backup
```bash
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP>
sudo /usr/local/bin/pg_backup_to_s3.sh
```

### Restore from Backup
See `docs/runbook.md` for detailed restore procedures.

## 💰 Cost Estimate

### Free Tier (First 12 Months)
- EC2 t3.micro: 750 hours/month (FREE)
- EBS 20 GB: 30 GB free
- S3: 5 GB free
- Data transfer: 15 GB outbound free

### Post-Free Tier (ap-south-1 Mumbai)
- EC2 t4g.micro: ~$6-7/month
- EBS 20 GB gp3: ~$2/month
- S3 storage: ~$0.50/month
- CloudWatch: ~$1-2/month
- **Total: ~$8-10/month**

## 🔒 Security

- SSH access restricted to specific IP
- PostgreSQL only accessible from localhost
- Nginx reverse proxy for API
- SSL/TLS with Let's Encrypt
- Environment variables for secrets
- IAM roles with minimal permissions

## 📚 Documentation

- [AWS Resources Setup](infrastructure/aws-resources-template.md)
- [Mobile Sync API](docs/mobile-sync-api.md)
- [Operational Runbook](docs/runbook.md)
- [Requirements](. kiro/specs/ec2-backend-infrastructure/requirements.md)
- [Design](. kiro/specs/ec2-backend-infrastructure/design.md)
- [Tasks](.kiro/specs/ec2-backend-infrastructure/tasks.md)

## 🐛 Troubleshooting

### Application won't start
```bash
# Check logs
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> "journalctl -u myapp -n 50"

# Check database connection
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> \
  "PGPASSWORD='<DB_PASSWORD>' psql -U myappuser -d myappdb -h localhost -c 'SELECT 1;'"
```

### 502 Bad Gateway
```bash
# Restart application
ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP> "sudo systemctl restart myapp"
```

See `docs/runbook.md` for comprehensive troubleshooting guide.

## 🚀 Scaling

### Current Capacity
- ~100 concurrent users
- ~1000 sync operations/hour
- ~10 GB database size

### Future Scaling Path
1. Migrate to RDS PostgreSQL
2. Add Application Load Balancer
3. Deploy to multiple EC2 instances
4. Use Auto Scaling Group
5. Implement Redis for caching
6. Serve static content from S3 + CloudFront

## 📝 License

This project is provided as-is for educational and commercial use.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📧 Support

For issues and questions:
- Check `docs/runbook.md` for operational procedures
- Review `docs/mobile-sync-api.md` for API details
- Check CloudWatch logs for errors

## 🎯 Roadmap

- [ ] Add authentication (JWT)
- [ ] Implement rate limiting
- [ ] Add API versioning
- [ ] Support for file uploads
- [ ] WebSocket support for real-time sync
- [ ] Multi-region deployment
- [ ] Automated testing pipeline
- [ ] Performance optimization
- [ ] Enhanced monitoring dashboards
