#!/bin/bash
set -e

# ============================================
# EC2 Bootstrap Script for Ubuntu 22.04 LTS
# ============================================
# IMPORTANT: Replace all <PLACEHOLDER> values before using this script
# This script should be pasted into EC2 User Data during instance launch

# ============================================
# CONFIGURATION VARIABLES - EDIT THESE
# ============================================
APP_USER=appuser
APP_HOME=/home/$APP_USER/app
DB_PASSWORD='<DB_PASSWORD>'              # REPLACE: Strong password for PostgreSQL
DOMAIN='<YOUR_DOMAIN>'                   # REPLACE: Your domain name (or leave empty for no SSL)
S3_BUCKET='<S3_BUCKET_NAME>'            # REPLACE: S3 bucket name for backups
REGION='ap-south-1'                      # AWS region (Mumbai)

# ============================================
# SYSTEM UPDATES
# ============================================
echo "Starting system update..."
apt-get update -y
apt-get upgrade -y

# ============================================
# CREATE APPLICATION USER
# ============================================
echo "Creating application user..."
adduser --disabled-password --gecos "" $APP_USER
usermod -aG sudo $APP_USER

# ============================================
# INSTALL PACKAGES
# ============================================
echo "Installing required packages..."
DEBIAN_FRONTEND=noninteractive apt-get install -y \
  openjdk-17-jdk \
  nginx \
  postgresql postgresql-contrib \
  awscli \
  python3 \
  python3-pip \
  certbot python3-certbot-nginx \
  git \
  unzip \
  jq \
  cloud-image-utils

# ============================================
# INSTALL CLOUDWATCH AGENT (OPTIONAL)
# ============================================
echo "Installing CloudWatch agent..."
curl -s https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb -o /tmp/amazon-cloudwatch-agent.deb || true
if [ -f /tmp/amazon-cloudwatch-agent.deb ]; then
  dpkg -i /tmp/amazon-cloudwatch-agent.deb || true
fi

# ============================================
# CONFIGURE POSTGRESQL
# ============================================
echo "Configuring PostgreSQL..."
# Set postgres user password
sudo -u postgres psql -c "ALTER USER postgres PASSWORD '$DB_PASSWORD';"

# Configure PostgreSQL to listen only on localhost
sed -i "s/#listen_addresses = 'localhost'/listen_addresses = 'localhost'/" /etc/postgresql/*/main/postgresql.conf

# Configure authentication method
sed -i "s/local\s*all\s*postgres\s*peer/local all postgres md5/" /etc/postgresql/*/main/pg_hba.conf || true

# Restart PostgreSQL
systemctl restart postgresql

# ============================================
# CREATE APPLICATION DATABASE AND USER
# ============================================
echo "Creating application database..."
sudo -u postgres psql -c "CREATE USER myappuser WITH PASSWORD '$DB_PASSWORD';"
sudo -u postgres psql -c "CREATE DATABASE myappdb OWNER myappuser;"

# ============================================
# CREATE APPLICATION DIRECTORIES
# ============================================
echo "Creating application directories..."
mkdir -p $APP_HOME
mkdir -p /var/log/myapp
chown -R $APP_USER:$APP_USER $APP_HOME
chown -R $APP_USER:$APP_USER /var/log/myapp

# ============================================
# CREATE SYSTEMD SERVICE FOR SPRING BOOT
# ============================================
echo "Creating systemd service..."
cat > /etc/systemd/system/myapp.service <<'EOL'
[Unit]
Description=MyApp Spring Boot Service
After=network.target postgresql.service

[Service]
User=appuser
WorkingDirectory=/home/appuser/app
Environment="DB_PASSWORD=<DB_PASSWORD>"
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /home/appuser/app/myapp.jar --spring.config.location=file:/home/appuser/app/application.yml
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/myapp/application.log
StandardError=append:/var/log/myapp/error.log

[Install]
WantedBy=multi-user.target
EOL

# Replace DB_PASSWORD in service file
sed -i "s/<DB_PASSWORD>/$DB_PASSWORD/" /etc/systemd/system/myapp.service

systemctl daemon-reload
systemctl enable myapp.service

# ============================================
# CONFIGURE NGINX
# ============================================
echo "Configuring Nginx..."
cat > /etc/nginx/sites-available/mywebsite <<'NGINX'
server {
    listen 80;
    server_name _;

    root /var/www/mywebsite;
    index index.html;

    # API proxy to Spring Boot
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # Health check endpoint
    location /health {
        proxy_pass http://127.0.0.1:8080/api/health;
        access_log off;
    }

    # Static website
    location / {
        try_files $uri $uri/ =404;
    }

    # Custom error pages
    error_page 502 503 504 /50x.html;
    location = /50x.html {
        root /usr/share/nginx/html;
    }
}
NGINX

# Create website directory
mkdir -p /var/www/mywebsite
chown -R www-data:www-data /var/www/mywebsite

# Create default index.html
cat > /var/www/mywebsite/index.html <<'HTML'
<!DOCTYPE html>
<html>
<head>
    <title>Welcome</title>
    <style>
        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
        h1 { color: #333; }
    </style>
</head>
<body>
    <h1>Welcome to MyApp</h1>
    <p>API is available at <a href="/api/health">/api/health</a></p>
</body>
</html>
HTML

# Enable site and disable default
ln -sf /etc/nginx/sites-available/mywebsite /etc/nginx/sites-enabled/mywebsite
rm -f /etc/nginx/sites-enabled/default

# Test and restart Nginx
nginx -t && systemctl restart nginx

# ============================================
# CREATE BACKUP SCRIPT
# ============================================
echo "Creating backup script..."
cat > /usr/local/bin/pg_backup_to_s3.sh <<SCRIPT
#!/bin/bash
TIMESTAMP=\$(date -u +"%Y-%m-%dT%H%M%SZ")
BACKUP_FILE="/tmp/myappdb-\$TIMESTAMP.sql.gz"

# Create backup
PGPASSWORD='$DB_PASSWORD' pg_dump -U myappuser -h localhost myappdb | gzip > \$BACKUP_FILE

# Upload to S3
aws s3 cp \$BACKUP_FILE s3://$S3_BUCKET/backups/ --region $REGION

# Cleanup local file
rm -f \$BACKUP_FILE

# Log result
echo "\$(date): Backup completed - \$BACKUP_FILE" >> /var/log/pg_backup.log
SCRIPT

chmod +x /usr/local/bin/pg_backup_to_s3.sh

# ============================================
# SETUP CRON JOB FOR BACKUPS
# ============================================
echo "Setting up backup cron job..."
(crontab -l 2>/dev/null; echo "30 2 * * * /usr/local/bin/pg_backup_to_s3.sh >> /var/log/pg_backup.log 2>&1") | crontab -

# ============================================
# SETUP LOG ROTATION
# ============================================
echo "Setting up log rotation..."
cat > /etc/logrotate.d/myapp <<'LOGROT'
/var/log/myapp/*.log {
    daily
    rotate 7
    compress
    missingok
    notifempty
    create 0640 appuser appuser
    sharedscripts
    postrotate
        systemctl reload myapp.service > /dev/null 2>&1 || true
    endscript
}
LOGROT

# ============================================
# FINAL MESSAGE
# ============================================
echo "============================================"
echo "Bootstrapping complete!"
echo "============================================"
echo "Next steps:"
echo "1. Upload your Spring Boot JAR to $APP_HOME/myapp.jar"
echo "2. Upload application.yml to $APP_HOME/application.yml"
echo "3. Start the application: sudo systemctl start myapp.service"
echo "4. Check status: sudo systemctl status myapp.service"
echo "5. View logs: journalctl -u myapp -f"
echo ""
echo "If you have a domain, run: sudo certbot --nginx -d $DOMAIN"
echo "============================================"
