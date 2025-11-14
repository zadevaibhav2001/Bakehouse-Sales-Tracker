#!/bin/bash

################################################################################
# Setup Daily S3 Backup for The Bake House Database
# 
# This script sets up automatic daily backups to S3
# Usage: ./setup-s3-backup.sh <instance-ip> <path-to-key> <s3-bucket-name>
################################################################################

set -e

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <instance-ip> <path-to-key.pem> <s3-bucket-name>"
    echo "Example: $0 3.110.159.51 ~/bakehouse-key.pem my-bakehouse-backups"
    exit 1
fi

INSTANCE_IP=$1
KEY_PATH=$2
S3_BUCKET=$3

echo "🔧 Setting up daily S3 backups..."
echo "Target: $INSTANCE_IP"
echo "S3 Bucket: $S3_BUCKET"
echo ""

# Create the backup script on EC2
ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no ec2-user@$INSTANCE_IP bash << EOF
set -e

echo "📝 Creating backup script..."

# Create backup script
sudo tee /usr/local/bin/bakehouse-backup.sh > /dev/null << 'BACKUP_SCRIPT'
#!/bin/bash

# Configuration
DB_PATH="/opt/bakehouse/myapp/data"
BACKUP_DIR="/tmp/bakehouse-backups"
S3_BUCKET="${S3_BUCKET}"
TIMESTAMP=\$(date +%Y%m%d_%H%M%S)
DATE_FOLDER=\$(date +%Y/%m)
BACKUP_FILE="bakehouse-db-\${TIMESTAMP}.tar.gz"

# Create backup directory
mkdir -p \${BACKUP_DIR}

# Create compressed backup
echo "\$(date): Starting backup..."
tar czf \${BACKUP_DIR}/\${BACKUP_FILE} -C \${DB_PATH} .

# Upload to S3
echo "\$(date): Uploading to S3..."
aws s3 cp \${BACKUP_DIR}/\${BACKUP_FILE} s3://\${S3_BUCKET}/backups/\${DATE_FOLDER}/\${BACKUP_FILE}

# Verify upload
if [ \$? -eq 0 ]; then
    echo "\$(date): Backup successful - \${BACKUP_FILE}"
    # Clean up local backup
    rm -f \${BACKUP_DIR}/\${BACKUP_FILE}
else
    echo "\$(date): Backup failed!"
    exit 1
fi

# Optional: Delete backups older than 30 days from S3
# aws s3 ls s3://\${S3_BUCKET}/backups/ --recursive | \\
#     awk '\$1 < "'"\$(date -d '30 days ago' +%Y-%m-%d)"'" {print \$4}' | \\
#     xargs -I {} aws s3 rm s3://\${S3_BUCKET}/{}

echo "\$(date): Backup completed successfully"
BACKUP_SCRIPT

# Make script executable
sudo chmod +x /usr/local/bin/bakehouse-backup.sh

echo "✓ Backup script created"

echo ""
echo "📅 Setting up cron job..."

# Add cron job (runs at 2 AM every day)
(sudo crontab -l 2>/dev/null | grep -v bakehouse-backup; echo "0 2 * * * /usr/local/bin/bakehouse-backup.sh >> /var/log/bakehouse-backup.log 2>&1") | sudo crontab -

echo "✓ Cron job configured (runs daily at 2 AM)"

echo ""
echo "📁 Creating log file..."
sudo touch /var/log/bakehouse-backup.log
sudo chown ec2-user:ec2-user /var/log/bakehouse-backup.log

echo "✓ Log file created"

echo ""
echo "🧪 Testing backup script..."
sudo -u ec2-user /usr/local/bin/bakehouse-backup.sh

if [ \$? -eq 0 ]; then
    echo "✓ Test backup successful!"
else
    echo "⚠️  Test backup failed - check AWS credentials and S3 bucket permissions"
fi

echo ""
echo "═══════════════════════════════════════════════════════"
echo "Backup Configuration Complete!"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "Backup Schedule: Daily at 2:00 AM UTC"
echo "S3 Bucket: s3://${S3_BUCKET}/backups/"
echo "Log File: /var/log/bakehouse-backup.log"
echo ""
echo "To view backup logs:"
echo "  tail -f /var/log/bakehouse-backup.log"
echo ""
echo "To manually run backup:"
echo "  sudo /usr/local/bin/bakehouse-backup.sh"
echo ""
echo "To list backups in S3:"
echo "  aws s3 ls s3://${S3_BUCKET}/backups/ --recursive"
echo ""

EOF

echo ""
echo "✅ S3 Backup Setup Complete!"
echo ""
echo "📋 Next Steps:"
echo "1. Ensure your EC2 instance has IAM role with S3 permissions"
echo "2. Create S3 bucket if it doesn't exist:"
echo "   aws s3 mb s3://$S3_BUCKET"
echo ""
echo "3. Verify backups are working:"
echo "   ssh -i $KEY_PATH ec2-user@$INSTANCE_IP 'tail -f /var/log/bakehouse-backup.log'"
echo ""
echo "4. To restore a backup:"
echo "   aws s3 cp s3://$S3_BUCKET/backups/YYYY/MM/backup-file.tar.gz ."
echo "   tar xzf backup-file.tar.gz -C /opt/bakehouse/myapp/data/"
echo ""
