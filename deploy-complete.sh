#!/bin/bash

################################################################################
# The Bake House - Complete Full-Stack Deployment Script
# 
# Deploys both backend (Spring Boot + H2) and frontend (HTML/CSS/JS) to EC2
# Configures Nginx to serve frontend and proxy API requests to backend
# 
# Usage: ./deploy-complete.sh <instance-ip> <path-to-key.pem>
################################################################################

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

if [ "$#" -ne 2 ]; then
    echo -e "${RED}Usage: $0 <instance-ip> <path-to-key.pem>${NC}"
    exit 1
fi

INSTANCE_IP=$1
KEY_PATH=$2

if [ ! -f "$KEY_PATH" ]; then
    echo -e "${RED}Error: Key file not found: $KEY_PATH${NC}"
    exit 1
fi

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   The Bake House - Full Stack Deployment              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Target: $INSTANCE_IP${NC}"
echo ""

# Create deployment packages
echo -e "${YELLOW}[1/8] Creating deployment packages...${NC}"
cd myapp
tar czf /tmp/bakehouse-backend.tar.gz pom.xml src/
cd ../frontend
tar czf /tmp/bakehouse-frontend.tar.gz *
cd ..
echo -e "${GREEN}✓ Packages created${NC}"
echo ""

# Upload packages
echo -e "${YELLOW}[2/8] Uploading to EC2...${NC}"
scp -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR \
    /tmp/bakehouse-backend.tar.gz \
    /tmp/bakehouse-frontend.tar.gz \
    ec2-user@$INSTANCE_IP:/tmp/

rm /tmp/bakehouse-backend.tar.gz /tmp/bakehouse-frontend.tar.gz
echo -e "${GREEN}✓ Files uploaded${NC}"
echo ""

# Deploy on EC2
echo -e "${YELLOW}[3/8] Deploying on EC2...${NC}"

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR ec2-user@$INSTANCE_IP bash << 'ENDSSH'
set -e

echo "→ Stopping services..."
sudo systemctl stop bakehouse-backend 2>/dev/null || true
sudo systemctl stop nginx 2>/dev/null || true

echo "→ Backing up database..."
if [ -d "/opt/bakehouse/myapp/data" ]; then
    sudo cp -r /opt/bakehouse/myapp/data /tmp/bakehouse-db-backup
    echo "  ✓ Database backed up to /tmp/bakehouse-db-backup"
else
    echo "  ℹ No existing database found"
fi

echo "→ Cleaning old files (preserving database)..."
sudo rm -rf /opt/bakehouse/myapp/src /opt/bakehouse/myapp/target /opt/bakehouse/myapp/pom.xml
sudo rm -rf /opt/bakehouse/frontend/*
sudo mkdir -p /opt/bakehouse/{myapp,frontend}
sudo chown -R ec2-user:ec2-user /opt/bakehouse

echo "→ Extracting backend..."
cd /opt/bakehouse/myapp
tar xzf /tmp/bakehouse-backend.tar.gz

echo "→ Extracting frontend..."
cd /opt/bakehouse/frontend
tar xzf /tmp/bakehouse-frontend.tar.gz

echo "→ Restoring database..."
if [ -d "/tmp/bakehouse-db-backup" ]; then
    sudo mkdir -p /opt/bakehouse/myapp/data
    sudo cp -r /tmp/bakehouse-db-backup/* /opt/bakehouse/myapp/data/
    sudo chown -R ec2-user:ec2-user /opt/bakehouse/myapp/data
    sudo rm -rf /tmp/bakehouse-db-backup
    echo "  ✓ Database restored"
else
    sudo mkdir -p /opt/bakehouse/myapp/data
    sudo chown -R ec2-user:ec2-user /opt/bakehouse/myapp/data
    echo "  ℹ No database to restore (fresh install)"
fi

rm /tmp/bakehouse-*.tar.gz

echo "✓ Files extracted and database preserved"
ENDSSH

echo -e "${GREEN}✓ Deployment prepared${NC}"
echo ""

echo -e "${YELLOW}[4/8] Installing dependencies...${NC}"

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR ec2-user@$INSTANCE_IP bash << 'ENDSSH'
set -e

# Install Nginx if not present
if ! command -v nginx &> /dev/null; then
    echo "→ Installing Nginx..."
    sudo amazon-linux-extras install -y nginx1 2>/dev/null || sudo yum install -y nginx
fi

# Install Maven if not present
if [ ! -f "/opt/maven/bin/mvn" ]; then
    echo "→ Installing Maven..."
    cd /opt
    sudo rm -rf maven apache-maven-* 2>/dev/null || true
    sudo wget -q https://dlcdn.apache.org/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz
    sudo tar xzf apache-maven-3.9.5-bin.tar.gz
    sudo ln -s apache-maven-3.9.5 maven
    sudo rm apache-maven-3.9.5-bin.tar.gz
fi

echo "✓ Dependencies ready"
ENDSSH

echo -e "${GREEN}✓ Dependencies installed${NC}"
echo ""

echo -e "${YELLOW}[5/8] Building backend...${NC}"

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR ec2-user@$INSTANCE_IP bash << 'ENDSSH'
set -e

cd /opt/bakehouse/myapp
/opt/maven/bin/mvn clean package -DskipTests -q

JAR_FILE=$(find target -name "*.jar" -type f | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: Build failed!"
    exit 1
fi

echo "✓ Built: $JAR_FILE ($(du -h $JAR_FILE | cut -f1))"
ENDSSH

echo -e "${GREEN}✓ Backend built${NC}"
echo ""

echo -e "${YELLOW}[6/8] Configuring Nginx...${NC}"

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR ec2-user@$INSTANCE_IP bash << 'ENDSSH'
set -e

# Configure Nginx
sudo tee /etc/nginx/conf.d/bakehouse.conf > /dev/null << 'EOF'
server {
    listen 80;
    server_name _;

    # Frontend
    root /opt/bakehouse/frontend;
    index index.html;

    # Serve frontend files
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to Spring Boot backend
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # Health check
    location /health {
        proxy_pass http://localhost:8080/actuator/health;
        access_log off;
    }
}
EOF

# Remove default config
sudo rm -f /etc/nginx/sites-enabled/default
sudo rm -f /etc/nginx/conf.d/default.conf

# Test Nginx config
sudo nginx -t

echo "✓ Nginx configured"
ENDSSH

echo -e "${GREEN}✓ Nginx configured${NC}"
echo ""

echo -e "${YELLOW}[7/8] Creating systemd service...${NC}"

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR ec2-user@$INSTANCE_IP bash << 'ENDSSH'
set -e

JAR_FILE=$(find /opt/bakehouse/myapp/target -name "*.jar" -type f | head -1)

sudo tee /etc/systemd/system/bakehouse-backend.service > /dev/null << EOF
[Unit]
Description=The Bake House Backend Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/bakehouse/myapp
Environment="JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto"
ExecStart=/usr/lib/jvm/java-17-amazon-corretto/bin/java -jar $JAR_FILE
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=bakehouse-backend

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
echo "✓ Service configured"
ENDSSH

echo -e "${GREEN}✓ Service configured${NC}"
echo ""

echo -e "${YELLOW}[8/8] Starting services...${NC}"

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR ec2-user@$INSTANCE_IP bash << 'ENDSSH'
set -e

# Start backend
sudo systemctl enable bakehouse-backend 2>/dev/null
sudo systemctl start bakehouse-backend

# Start Nginx
sudo systemctl enable nginx 2>/dev/null
sudo systemctl start nginx

echo "→ Waiting for backend..."
for i in {1..40}; do
    sleep 1
    if sudo netstat -tlnp 2>/dev/null | grep -q ":8080"; then
        break
    fi
    [ $((i % 5)) -eq 0 ] && echo -n " ${i}s" || echo -n "."
done
echo ""

echo ""
echo "═══════════════════════════════════════════════════════"
echo "Service Status:"
echo "═══════════════════════════════════════════════════════"

# Backend status
if sudo systemctl is-active --quiet bakehouse-backend; then
    echo "✓ Backend: RUNNING"
else
    echo "✗ Backend: STOPPED"
fi

# Nginx status
if sudo systemctl is-active --quiet nginx; then
    echo "✓ Nginx: RUNNING"
else
    echo "✗ Nginx: STOPPED"
fi

# Port checks
if sudo netstat -tlnp 2>/dev/null | grep -q ":80"; then
    echo "✓ Port 80 (HTTP): LISTENING"
else
    echo "✗ Port 80 (HTTP): NOT LISTENING"
fi

if sudo netstat -tlnp 2>/dev/null | grep -q ":8080"; then
    echo "✓ Port 8080 (Backend): LISTENING"
else
    echo "✗ Port 8080 (Backend): NOT LISTENING"
fi

echo ""
echo "═══════════════════════════════════════════════════════"
echo "Testing Application:"
echo "═══════════════════════════════════════════════════════"

# Test frontend
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/ 2>&1)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Frontend: Working (HTTP 200)"
else
    echo "○ Frontend: HTTP $HTTP_CODE"
fi

# Test API
sleep 2
API_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/api/products 2>&1)
if [ "$API_CODE" = "200" ]; then
    echo "✓ API: Working (HTTP 200)"
else
    echo "○ API: HTTP $API_CODE"
fi

echo ""
echo "Recent Backend Logs:"
sudo journalctl -u bakehouse-backend -n 15 --no-pager | tail -10

ENDSSH

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║          🎉 Deployment Complete! 🎉                    ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}Your Application:${NC}"
echo "  🌐 Frontend: http://$INSTANCE_IP"
echo "  🔌 API: http://$INSTANCE_IP/api/products"
echo "  ❤️  Health: http://$INSTANCE_IP/health"
echo ""
echo -e "${BLUE}Useful Commands:${NC}"
echo "  📋 Backend logs:"
echo "     ssh -i $KEY_PATH ec2-user@$INSTANCE_IP 'sudo journalctl -u bakehouse-backend -f'"
echo ""
echo "  📋 Nginx logs:"
echo "     ssh -i $KEY_PATH ec2-user@$INSTANCE_IP 'sudo tail -f /var/log/nginx/error.log'"
echo ""
echo "  🔄 Restart services:"
echo "     ssh -i $KEY_PATH ec2-user@$INSTANCE_IP 'sudo systemctl restart bakehouse-backend nginx'"
echo ""
echo -e "${GREEN}Open http://$INSTANCE_IP in your browser to use the app!${NC}"
echo ""
