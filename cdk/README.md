# The Bake House - AWS CDK Deployment

This CDK project deploys The Bake House application to AWS EC2 with a complete infrastructure setup.

## Architecture

- **EC2 Instance**: t3.small running Amazon Linux 2
- **Nginx**: Serves frontend and proxies API requests to backend
- **Spring Boot**: Backend API running on port 8080
- **SQLite**: Local database on EC2 instance
- **Security Group**: Allows HTTP (80), HTTPS (443), SSH (22), and backend (8080)
- **IAM Role**: Enables Systems Manager and CloudWatch

## Prerequisites

1. **AWS CLI** configured with credentials
2. **Node.js** (v18 or later)
3. **AWS CDK** installed globally:
   ```bash
   npm install -g aws-cdk
   ```
4. **AWS Account** with appropriate permissions
5. **EC2 Key Pair** named `bakehouse-key` (create in AWS Console)

## Setup

1. **Install dependencies**:
   ```bash
   cd cdk
   npm install
   ```

2. **Bootstrap CDK** (first time only):
   ```bash
   cdk bootstrap
   ```

3. **Create EC2 Key Pair**:
   - Go to AWS Console → EC2 → Key Pairs
   - Create a key pair named `bakehouse-key`
   - Download the `.pem` file
   - Set permissions: `chmod 400 bakehouse-key.pem`

## Deployment

1. **Review the stack**:
   ```bash
   cdk synth
   ```

2. **Deploy to AWS**:
   ```bash
   cdk deploy
   ```

3. **Note the outputs**:
   - Instance Public IP
   - Website URL
   - SSH Command

## Upload Application Files

After deployment, upload your application to the EC2 instance:

```bash
# Get the instance IP from CDK output
INSTANCE_IP=<your-instance-ip>

# Upload backend
scp -i bakehouse-key.pem -r ../myapp ec2-user@$INSTANCE_IP:/opt/bakehouse/

# Upload frontend
scp -i bakehouse-key.pem -r ../frontend/* ec2-user@$INSTANCE_IP:/opt/bakehouse/frontend/

# SSH into instance
ssh -i bakehouse-key.pem ec2-user@$INSTANCE_IP

# Run deployment script
/opt/bakehouse/deploy.sh
```

## Alternative: Use Systems Manager

If you don't want to use SSH:

```bash
# Get instance ID from AWS Console
aws ssm start-session --target <instance-id>

# Then upload files using S3 or other methods
```

## Access Your Application

Once deployed and files uploaded:
- **Website**: `http://<instance-public-dns>`
- **API**: `http://<instance-public-dns>/api/products`

## Monitoring

```bash
# SSH into instance
ssh -i bakehouse-key.pem ec2-user@<instance-ip>

# Check backend logs
sudo journalctl -u bakehouse-backend -f

# Check nginx logs
sudo tail -f /var/log/nginx/error.log

# Check service status
sudo systemctl status bakehouse-backend
sudo systemctl status nginx
```

## Updating the Application

```bash
# SSH into instance
ssh -i bakehouse-key.pem ec2-user@<instance-ip>

# Pull latest changes (if using git)
cd /opt/bakehouse/myapp
git pull

# Or upload new files via SCP
# Then run:
/opt/bakehouse/deploy.sh
```

## Cost Estimation (FREE TIER)

- **EC2 t2.micro**: FREE (750 hours/month for 12 months)
- **EBS Storage**: FREE (30 GB for 12 months)
- **Data Transfer**: FREE (15 GB outbound/month)
- **VPC**: FREE
- **Total**: **$0/month** (within free tier limits)

### Free Tier Limits:
- 750 hours/month of t2.micro (enough for 1 instance running 24/7)
- 30 GB of EBS General Purpose (SSD) storage
- 15 GB of bandwidth out aggregated across all AWS services
- Valid for 12 months from AWS account creation

## Cleanup

To destroy all resources:

```bash
cdk destroy
```

## Customization

### Change Instance Type

**Current**: t2.micro (FREE TIER)

To upgrade after free tier expires, edit `cdk/lib/bakehouse-stack.ts`:
```typescript
instanceType: ec2.InstanceType.of(
  ec2.InstanceClass.T3,  // or T3A for lower cost
  ec2.InstanceSize.SMALL  // SMALL, MEDIUM, LARGE, etc.
)
```

**Note**: Changing from t2.micro will incur charges (~$8-15/month for t3.small)

### Add Custom Domain

1. Register domain in Route 53
2. Add certificate in ACM
3. Update Nginx configuration
4. Add Route 53 record pointing to instance

### Enable HTTPS

1. Get SSL certificate (Let's Encrypt or ACM)
2. Update Nginx configuration
3. Redeploy

## Troubleshooting

### Backend not starting
```bash
sudo journalctl -u bakehouse-backend -n 100
```

### Nginx errors
```bash
sudo nginx -t
sudo tail -f /var/log/nginx/error.log
```

### Can't connect
- Check security group rules
- Verify instance is running
- Check if services are started

## Support

For issues or questions, check the application logs and AWS CloudWatch.
