#!/bin/bash

################################################################################
# The Bake House - Frontend Only Deployment Script
# 
# Deploys only frontend (HTML/CSS/JS) to EC2
# Updates Nginx configuration to serve the new frontend files
# 
# Usage: ./deploy-frontend.sh <instance-ip> <path-to-key.pem>
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
echo -e "${BLUE}║   The Bake House - Frontend Only Deployment           ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Target: $INSTANCE_IP${NC}"
echo ""

# Create frontend package
echo -e "${YELLOW}[1/4] Creating frontend package...${NC}"
cd frontend
tar czf /tmp/bakehouse-frontend.tar.gz *
cd ..
echo -e "${GREEN}✓ Frontend package created${NC}"
echo ""

# Upload package
echo -e "${YELLOW}[2/4] Uploading to EC2...${NC}"
scp -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR \
    /tmp/bakehouse-frontend.tar.gz \
    ec2-user@$INSTANCE_IP:/tmp/

rm /tmp/bakehouse-frontend.tar.gz
echo -e "${GREEN}✓ Frontend uploaded${NC}"
echo ""

# Deploy frontend on EC2
echo -e "${YELLOW}[3/4] Deploying frontend...${NC}"

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR ec2-user@$INSTANCE_IP bash << 'ENDSSH'
set -e

echo "→ Backing up current frontend..."
if [ -d "/opt/bakehouse/frontend" ]; then
    sudo cp -r /opt/bakehouse/frontend /tmp/frontend-backup
    echo "  ✓ Frontend backed up to /tmp/frontend-backup"
fi

echo "→ Cleaning old frontend files..."
sudo rm -rf /opt/bakehouse/frontend/*
sudo mkdir -p /opt/bakehouse/frontend
sudo chown -R ec2-user:ec2-user /opt/bakehouse/frontend

echo "→ Extracting new frontend..."
cd /opt/bakehouse/frontend
tar xzf /tmp/bakehouse-frontend.tar.gz
rm /tmp/bakehouse-frontend.tar.gz

echo "✓ Frontend deployed"
ENDSSH

echo -e "${GREEN}✓ Frontend deployed${NC}"
echo ""

echo -e "${YELLOW}[4/4] Restarting Nginx...${NC}"

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR ec2-user@$INSTANCE_IP bash << 'ENDSSH'
set -e

# Test Nginx config
sudo nginx -t

# Restart Nginx to pick up new files
sudo systemctl restart nginx

echo "→ Waiting for Nginx..."
sleep 2

echo ""
echo "═══════════════════════════════════════════════════════"
echo "Service Status:"
echo "═══════════════════════════════════════════════════════"

# Nginx status
if sudo systemctl is-active --quiet nginx; then
    echo "✓ Nginx: RUNNING"
else
    echo "✗ Nginx: STOPPED"
fi

# Port check
if sudo netstat -tlnp 2>/dev/null | grep -q ":80"; then
    echo "✓ Port 80 (HTTP): LISTENING"
else
    echo "✗ Port 80 (HTTP): NOT LISTENING"
fi

echo ""
echo "═══════════════════════════════════════════════════════"
echo "Testing Frontend:"
echo "═══════════════════════════════════════════════════════"

# Test frontend
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/ 2>&1)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Frontend: Working (HTTP 200)"
else
    echo "○ Frontend: HTTP $HTTP_CODE"
fi

ENDSSH

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                 DEPLOYMENT COMPLETE!                  ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Frontend deployed successfully to: http://$INSTANCE_IP${NC}"
echo ""