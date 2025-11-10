# Quick Start Guide

Get your EC2 backend infrastructure up and running in under 30 minutes.

## Prerequisites Checklist

- [ ] AWS Account with billing enabled
- [ ] AWS CLI installed: `aws --version`
- [ ] Java 17 installed: `java -version`
- [ ] Maven installed: `mvn -version`
- [ ] SSH client available
- [ ] Your public IP address: `curl ifconfig.me`

## Step 1: Prepare Configuration (5 minutes)

### 1.1 Choose Your Values

Create a file `config.txt` with your values:

```bash
# AWS Configuration
AWS_REGION=ap-south-1
INSTANCE_TYPE=t3.micro  # or t4g.micro for ARM

# Security
DB_PASSWORD=YourStrongPassword123!  # CHANGE THIS
YOUR_IP=$(curl -s ifconfig.me)

# S3 Bucket (must be globally unique)
S3_BUCKET=myapp-backups-$(date +%s)

# Domain (optional, leave empty if none)
DOMAIN=api.yourdomain.com  # or leave empty
```

### 1.2 Edit Bootstrap Script

```bash
# Edit the user-data script
nano infrastructure/ec2-user-data.sh

# Replace these placeholders:
# <DB_PASSWORD> → Your strong password
# <S3_BUCKET_NAME> → Your S3 bucket name
# <YOUR_DOMAIN> → Your domain (or leave empty)
```

## Step 2: Create AWS Resources (10 minutes)

### 2.1 Create S3 Bucket

```bash
aws s3api create-bucket \
  --bucket myapp-backups-YOUR-UNIQUE-ID \
  --region ap-south-1 \
  --create-bucket-configuration LocationConstraint=ap-south-1

aws s3api put-bucket-encryption \
  --bucket myapp-backups-YOUR-UNIQUE-ID \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'
```

### 2.2 Create Security Group

```bash
# Get your default VPC ID
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=isDefault,Values=true" \
  --query "Vpcs[0].VpcId" \
  --output text \
  --region ap-south-1)

# Create security group
SG_ID=$(aws ec2 create-security-group \
  --group-name ec2-backend-sg \
  --description "Security group for EC2 backend" \
  --vpc-id $VPC_ID \
  --region ap-south-1 \
  --query 'GroupId' \
  --output text)

# Get your IP
YOUR_IP=$(curl -s ifconfig.me)

# Add rules
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --ip-permissions \
    IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges="[{CidrIp=$YOUR_IP/32}]" \
    IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges="[{CidrIp=0.0.0.0/0}]" \
    IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges="[{CidrIp=0.0.0.0/0}]" \
  --region ap-south-1

echo "Security Group ID: $SG_ID"
```

### 2.3 Create IAM Role

```bash
# Create trust policy
cat > /tmp/trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"Service": "ec2.amazonaws.com"},
    "Action": "sts:AssumeRole"
  }]
}
EOF

# Create role
aws iam create-role \
  --role-name EC2-Backend-Role \
  --assume-role-policy-document file:///tmp/trust-policy.json

# Create policy
cat > /tmp/instance-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::myapp-backups-YOUR-UNIQUE-ID",
        "arn:aws:s3:::myapp-backups-YOUR-UNIQUE-ID/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData",
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "*"
    }
  ]
}
EOF

# Get account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Create and attach policy
aws iam create-policy \
  --policy-name EC2-Backend-Policy \
  --policy-document file:///tmp/instance-policy.json

aws iam attach-role-policy \
  --role-name EC2-Backend-Role \
  --policy-arn arn:aws:iam::$ACCOUNT_ID:policy/EC2-Backend-Policy

# Create instance profile
aws iam create-instance-profile \
  --instance-profile-name EC2-Backend-InstanceProfile

aws iam add-role-to-instance-profile \
  --instance-profile-name EC2-Backend-InstanceProfile \
  --role-name EC2-Backend-Role

# Wait for propagation
echo "Waiting 10 seconds for IAM propagation..."
sleep 10
```

### 2.4 Create Key Pair

```bash
aws ec2 create-key-pair \
  --key-name myapp-key \
  --region ap-south-1 \
  --query 'KeyMaterial' \
  --output text > ~/.ssh/myapp-key.pem

chmod 400 ~/.ssh/myapp-key.pem

echo "Key saved to ~/.ssh/myapp-key.pem"
```

### 2.5 Allocate Elastic IP

```bash
ALLOCATION_ID=$(aws ec2 allocate-address \
  --domain vpc \
  --region ap-south-1 \
  --query 'AllocationId' \
  --output text)

echo "Elastic IP Allocation ID: $ALLOCATION_ID"
```

## Step 3: Launch EC2 Instance (5 minutes)

```bash
# Find Ubuntu 22.04 AMI
AMI_ID=$(aws ec2 describe-images \
  --owners 099720109477 \
  --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
  --query 'sort_by(Images, &CreationDate)[-1].ImageId' \
  --output text \
  --region ap-south-1)

echo "Using AMI: $AMI_ID"

# Launch instance
INSTANCE_ID=$(aws ec2 run-instances \
  --image-id $AMI_ID \
  --instance-type t3.micro \
  --key-name myapp-key \
  --security-group-ids $SG_ID \
  --iam-instance-profile Name=EC2-Backend-InstanceProfile \
  --user-data file://infrastructure/ec2-user-data.sh \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":20,"VolumeType":"gp3"}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=backend-server}]' \
  --region ap-south-1 \
  --query 'Instances[0].InstanceId' \
  --output text)

echo "Instance ID: $INSTANCE_ID"

# Wait for instance to be running
echo "Waiting for instance to start..."
aws ec2 wait instance-running \
  --instance-ids $INSTANCE_ID \
  --region ap-south-1

# Associate Elastic IP
aws ec2 associate-address \
  --instance-id $INSTANCE_ID \
  --allocation-id $ALLOCATION_ID \
  --region ap-south-1

# Get public IP
PUBLIC_IP=$(aws ec2 describe-addresses \
  --allocation-ids $ALLOCATION_ID \
  --query 'Addresses[0].PublicIp' \
  --output text \
  --region ap-south-1)

echo "========================================="
echo "Instance launched successfully!"
echo "Instance ID: $INSTANCE_ID"
echo "Public IP: $PUBLIC_IP"
echo "========================================="
echo "Waiting for bootstrap to complete (5-10 minutes)..."
echo "You can monitor progress with:"
echo "ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP 'tail -f /var/log/cloud-init-output.log'"
```

## Step 4: Build and Deploy Application (5 minutes)

### 4.1 Wait for Bootstrap

```bash
# Wait 5-10 minutes for bootstrap to complete
# Check bootstrap status
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP 'tail -20 /var/log/cloud-init-output.log'

# Look for "Bootstrapping complete!" message
```

### 4.2 Build Application

```bash
cd myapp
mvn clean package -DskipTests
cd ..
```

### 4.3 Configure Deployment Script

```bash
# Edit deploy script
nano scripts/deploy.sh

# Replace:
# <EC2_PUBLIC_IP_OR_DOMAIN> with your $PUBLIC_IP
```

### 4.4 Deploy

```bash
./scripts/deploy.sh
```

## Step 5: Initialize Database (2 minutes)

```bash
# SSH into instance
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP

# Create schema
PGPASSWORD='YourStrongPassword123!' psql -U myappuser -d myappdb -h localhost <<EOF
CREATE TABLE IF NOT EXISTS entries (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    payload JSONB,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted BOOLEAN DEFAULT FALSE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_entries_user_updated 
ON entries (user_id, updated_at);
EOF

# Verify
PGPASSWORD='YourStrongPassword123!' psql -U myappuser -d myappdb -h localhost -c '\dt'

# Exit SSH
exit
```

## Step 6: Verify Everything Works (3 minutes)

### 6.1 Test Health Endpoint

```bash
curl http://$PUBLIC_IP/api/health
# Expected: OK
```

### 6.2 Test Push Sync

```bash
curl -X POST http://$PUBLIC_IP/api/sync/push \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "entries": [{
      "id": "test-id-1",
      "payload": "{\"title\":\"Test Note\",\"content\":\"Hello World\"}",
      "updatedAt": "2025-11-10T12:00:00Z",
      "deleted": false
    }]
  }'

# Expected: {"accepted":1,"conflicts":[]}
```

### 6.3 Test Pull Sync

```bash
curl "http://$PUBLIC_IP/api/sync/pull?userId=test-user"

# Expected: {"entries":[...],"serverTime":"..."}
```

### 6.4 Test Website

```bash
# Open in browser
open http://$PUBLIC_IP

# Or with curl
curl http://$PUBLIC_IP
```

## Step 7: Set Up SSL (Optional, 5 minutes)

If you have a domain:

```bash
# SSH into instance
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP

# Run certbot
sudo certbot --nginx -d yourdomain.com

# Follow prompts
# Test HTTPS
curl https://yourdomain.com/api/health

exit
```

## Troubleshooting

### Instance not accessible

```bash
# Check instance status
aws ec2 describe-instance-status \
  --instance-ids $INSTANCE_ID \
  --region ap-south-1

# Check security group allows your IP
aws ec2 describe-security-groups \
  --group-ids $SG_ID \
  --region ap-south-1
```

### Application not starting

```bash
# Check logs
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP \
  "journalctl -u myapp -n 50"

# Check if database is running
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP \
  "sudo systemctl status postgresql"
```

### Health check fails

```bash
# Check if application is running
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP \
  "sudo systemctl status myapp"

# Check if Nginx is running
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP \
  "sudo systemctl status nginx"

# Test from inside instance
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP \
  "curl http://localhost:8080/api/health"
```

## Next Steps

1. **Set up monitoring**: Configure CloudWatch alarms
2. **Configure backups**: Verify S3 backups are working
3. **Add authentication**: Implement JWT or API keys
4. **Mobile integration**: Use the API in your mobile apps
5. **Custom domain**: Point your domain to the Elastic IP

## Useful Commands

```bash
# Save these for later use
export PUBLIC_IP=<your-elastic-ip>
export INSTANCE_ID=<your-instance-id>
export SG_ID=<your-security-group-id>

# SSH into instance
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP

# View application logs
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP "journalctl -u myapp -f"

# Restart application
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP "sudo systemctl restart myapp"

# Check backup logs
ssh -i ~/.ssh/myapp-key.pem ubuntu@$PUBLIC_IP "tail -f /var/log/pg_backup.log"

# List backups in S3
aws s3 ls s3://myapp-backups-YOUR-UNIQUE-ID/backups/ --region ap-south-1
```

## Cost Tracking

```bash
# Set up billing alert
aws budgets create-budget \
  --account-id $ACCOUNT_ID \
  --budget file://budget.json \
  --notifications-with-subscribers file://notifications.json
```

## Summary

You now have:
- ✅ EC2 instance running Ubuntu 22.04
- ✅ Spring Boot REST API on port 8080
- ✅ PostgreSQL database
- ✅ Nginx reverse proxy
- ✅ Automated daily backups to S3
- ✅ CloudWatch monitoring
- ✅ Static website
- ✅ SSL/TLS (if domain configured)

**Total setup time: ~30 minutes**
**Monthly cost: ~$8-10 (after Free Tier)**

For detailed documentation, see:
- [README.md](README.md) - Full documentation
- [docs/mobile-sync-api.md](docs/mobile-sync-api.md) - API reference
- [docs/runbook.md](docs/runbook.md) - Operations guide
