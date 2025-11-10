# AWS Resources Setup Guide

This document provides templates and instructions for creating the required AWS resources.

## Prerequisites

- AWS Account with appropriate permissions
- AWS CLI installed and configured
- Your public IP address for SSH access

## 1. Security Group

### Via AWS Console:
1. Go to EC2 → Security Groups → Create Security Group
2. Name: `ec2-backend-sg`
3. Description: `Security group for EC2 backend instance`
4. VPC: Select default VPC in ap-south-1

**Inbound Rules:**
| Type  | Protocol | Port Range | Source          | Description           |
|-------|----------|------------|-----------------|-----------------------|
| SSH   | TCP      | 22         | <YOUR_IP>/32    | SSH access            |
| HTTP  | TCP      | 80         | 0.0.0.0/0       | Public HTTP access    |
| HTTPS | TCP      | 443        | 0.0.0.0/0       | Public HTTPS access   |

**Outbound Rules:**
| Type        | Protocol | Port Range | Destination | Description      |
|-------------|----------|------------|-------------|------------------|
| All traffic | All      | All        | 0.0.0.0/0   | Allow all egress |

### Via AWS CLI:
```bash
# Create security group
aws ec2 create-security-group \
  --group-name ec2-backend-sg \
  --description "Security group for EC2 backend instance" \
  --vpc-id <YOUR_VPC_ID> \
  --region ap-south-1

# Add inbound rules (replace <YOUR_IP> and <SG_ID>)
aws ec2 authorize-security-group-ingress \
  --group-id <SG_ID> \
  --ip-permissions \
    IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges='[{CidrIp=<YOUR_IP>/32,Description="SSH access"}]' \
    IpProtocol=tcp,FromPort=80,ToPort=80,IpRanges='[{CidrIp=0.0.0.0/0,Description="HTTP access"}]' \
    IpProtocol=tcp,FromPort=443,ToPort=443,IpRanges='[{CidrIp=0.0.0.0/0,Description="HTTPS access"}]' \
  --region ap-south-1
```

## 2. S3 Bucket for Backups

### Via AWS Console:
1. Go to S3 → Create Bucket
2. Bucket name: `<YOUR_UNIQUE_BUCKET_NAME>` (must be globally unique)
3. Region: ap-south-1 (Mumbai)
4. Block all public access: ✓ (enabled)
5. Bucket Versioning: Disabled (optional)
6. Default encryption: Enable (SSE-S3)
7. Create bucket

### Via AWS CLI:
```bash
# Create bucket
aws s3api create-bucket \
  --bucket <YOUR_BUCKET_NAME> \
  --region ap-south-1 \
  --create-bucket-configuration LocationConstraint=ap-south-1

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket <YOUR_BUCKET_NAME> \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'

# Block public access
aws s3api put-public-access-block \
  --bucket <YOUR_BUCKET_NAME> \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"
```

## 3. IAM Role for EC2

### IAM Policy Document

Create a file `ec2-instance-policy.json`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::<YOUR_BUCKET_NAME>",
        "arn:aws:s3:::<YOUR_BUCKET_NAME>/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "cloudwatch:PutMetricData",
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogStreams"
      ],
      "Resource": "*"
    }
  ]
}
```

### Trust Policy Document

Create a file `ec2-trust-policy.json`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

### Via AWS CLI:
```bash
# Create IAM role
aws iam create-role \
  --role-name EC2-Backend-Role \
  --assume-role-policy-document file://ec2-trust-policy.json

# Create IAM policy
aws iam create-policy \
  --policy-name EC2-Backend-Policy \
  --policy-document file://ec2-instance-policy.json

# Attach policy to role (replace <ACCOUNT_ID>)
aws iam attach-role-policy \
  --role-name EC2-Backend-Role \
  --policy-arn arn:aws:iam::<ACCOUNT_ID>:policy/EC2-Backend-Policy

# Create instance profile
aws iam create-instance-profile \
  --instance-profile-name EC2-Backend-InstanceProfile

# Add role to instance profile
aws iam add-role-to-instance-profile \
  --instance-profile-name EC2-Backend-InstanceProfile \
  --role-name EC2-Backend-Role
```

## 4. Elastic IP

### Via AWS Console:
1. Go to EC2 → Elastic IPs → Allocate Elastic IP address
2. Network Border Group: ap-south-1
3. Allocate
4. After EC2 instance is launched, associate this EIP with the instance

### Via AWS CLI:
```bash
# Allocate Elastic IP
aws ec2 allocate-address \
  --domain vpc \
  --region ap-south-1

# Note the AllocationId from the output
# After launching EC2, associate it:
aws ec2 associate-address \
  --instance-id <INSTANCE_ID> \
  --allocation-id <ALLOCATION_ID> \
  --region ap-south-1
```

## 5. EC2 Key Pair

### Via AWS Console:
1. Go to EC2 → Key Pairs → Create Key Pair
2. Name: `myapp-key`
3. Key pair type: RSA
4. Private key file format: .pem
5. Create and download the key
6. Save to `~/.ssh/myapp-key.pem`
7. Set permissions: `chmod 400 ~/.ssh/myapp-key.pem`

### Via AWS CLI:
```bash
# Create key pair
aws ec2 create-key-pair \
  --key-name myapp-key \
  --region ap-south-1 \
  --query 'KeyMaterial' \
  --output text > ~/.ssh/myapp-key.pem

# Set permissions
chmod 400 ~/.ssh/myapp-key.pem
```

## 6. Launch EC2 Instance

### Via AWS Console:
1. Go to EC2 → Launch Instance
2. Name: `backend-server`
3. AMI: Ubuntu Server 22.04 LTS (HVM), SSD Volume Type
4. Architecture: 64-bit (x86) for t3.micro OR 64-bit (Arm) for t4g.micro
5. Instance type: t3.micro or t4g.micro
6. Key pair: Select `myapp-key`
7. Network settings:
   - VPC: Default VPC
   - Subnet: Any public subnet
   - Auto-assign public IP: Enable
   - Security group: Select `ec2-backend-sg`
8. Configure storage: 20 GB gp3
9. Advanced details:
   - IAM instance profile: `EC2-Backend-InstanceProfile`
   - User data: Paste contents of `infrastructure/ec2-user-data.sh` (after editing placeholders)
10. Launch instance

### Via AWS CLI:
```bash
# Find Ubuntu 22.04 AMI ID for ap-south-1
aws ec2 describe-images \
  --owners 099720109477 \
  --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
  --query 'sort_by(Images, &CreationDate)[-1].ImageId' \
  --output text \
  --region ap-south-1

# Launch instance (replace placeholders)
aws ec2 run-instances \
  --image-id <AMI_ID> \
  --instance-type t3.micro \
  --key-name myapp-key \
  --security-group-ids <SG_ID> \
  --iam-instance-profile Name=EC2-Backend-InstanceProfile \
  --user-data file://infrastructure/ec2-user-data.sh \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":20,"VolumeType":"gp3"}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=backend-server}]' \
  --region ap-south-1
```

## 7. CloudWatch Log Groups (Optional)

```bash
# Create log groups
aws logs create-log-group \
  --log-group-name /aws/ec2/spring-boot \
  --region ap-south-1

aws logs create-log-group \
  --log-group-name /aws/ec2/nginx \
  --region ap-south-1
```

## 8. Route53 DNS (Optional)

If you have a domain in Route53:

```bash
# Create A record pointing to Elastic IP
aws route53 change-resource-record-sets \
  --hosted-zone-id <HOSTED_ZONE_ID> \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "api.yourdomain.com",
        "Type": "A",
        "TTL": 300,
        "ResourceRecords": [{"Value": "<ELASTIC_IP>"}]
      }
    }]
  }'
```

## Summary Checklist

- [ ] Security Group created with SSH, HTTP, HTTPS rules
- [ ] S3 bucket created with encryption enabled
- [ ] IAM role and instance profile created with S3 and CloudWatch permissions
- [ ] Elastic IP allocated
- [ ] EC2 key pair created and saved securely
- [ ] EC2 instance launched with user-data script
- [ ] Elastic IP associated with EC2 instance
- [ ] (Optional) Route53 DNS record created

## Next Steps

After infrastructure is set up:
1. SSH into the instance: `ssh -i ~/.ssh/myapp-key.pem ubuntu@<ELASTIC_IP>`
2. Check bootstrap logs: `tail -f /var/log/cloud-init-output.log`
3. Build and deploy the Spring Boot application
4. Configure SSL with Let's Encrypt if you have a domain
