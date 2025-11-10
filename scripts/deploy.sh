#!/bin/bash
set -e

# ============================================
# Deployment Script for MyApp
# ============================================
# This script deploys the Spring Boot application to EC2

# Configuration - EDIT THESE VALUES
SERVER_USER=ubuntu
HOST="<EC2_PUBLIC_IP_OR_DOMAIN>"  # REPLACE with your EC2 Elastic IP or domain
KEY="$HOME/.ssh/myapp-key.pem"    # Path to your SSH key
LOCAL_JAR="target/myapp-0.0.1-SNAPSHOT.jar"
REMOTE_APP_DIR="/home/appuser/app"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}MyApp Deployment Script${NC}"
echo -e "${GREEN}============================================${NC}"

# Check if JAR exists
if [ ! -f "$LOCAL_JAR" ]; then
    echo -e "${RED}Error: JAR file not found at $LOCAL_JAR${NC}"
    echo "Please run: mvn clean package"
    exit 1
fi

# Check if SSH key exists
if [ ! -f "$KEY" ]; then
    echo -e "${RED}Error: SSH key not found at $KEY${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 1: Backing up current JAR on server...${NC}"
ssh -i "$KEY" "$SERVER_USER@$HOST" "
    if [ -f $REMOTE_APP_DIR/myapp.jar ]; then
        cp $REMOTE_APP_DIR/myapp.jar $REMOTE_APP_DIR/myapp.jar.backup
        echo 'Backup created'
    else
        echo 'No existing JAR to backup'
    fi
" || echo "Backup step skipped"

echo -e "${YELLOW}Step 2: Uploading new JAR to server...${NC}"
scp -i "$KEY" "$LOCAL_JAR" "$SERVER_USER@$HOST:$REMOTE_APP_DIR/myapp.jar"

echo -e "${YELLOW}Step 3: Restarting application service...${NC}"
ssh -i "$KEY" "$SERVER_USER@$HOST" "
    sudo systemctl restart myapp.service
    sleep 3
    sudo systemctl status myapp.service --no-pager
"

echo -e "${YELLOW}Step 4: Waiting for application to start...${NC}"
sleep 5

echo -e "${YELLOW}Step 5: Checking application health...${NC}"
HEALTH_CHECK=$(curl -s -o /dev/null -w "%{http_code}" "http://$HOST/api/health" || echo "000")

if [ "$HEALTH_CHECK" = "200" ]; then
    echo -e "${GREEN}✓ Deployment successful! Application is healthy.${NC}"
    echo -e "${GREEN}Health check: http://$HOST/api/health${NC}"
else
    echo -e "${RED}✗ Warning: Health check failed (HTTP $HEALTH_CHECK)${NC}"
    echo -e "${YELLOW}Check logs with: ssh -i $KEY $SERVER_USER@$HOST 'journalctl -u myapp -n 50'${NC}"
    exit 1
fi

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo "Useful commands:"
echo "  View logs: ssh -i $KEY $SERVER_USER@$HOST 'journalctl -u myapp -f'"
echo "  Check status: ssh -i $KEY $SERVER_USER@$HOST 'sudo systemctl status myapp'"
echo "  Rollback: ssh -i $KEY $SERVER_USER@$HOST 'cp $REMOTE_APP_DIR/myapp.jar.backup $REMOTE_APP_DIR/myapp.jar && sudo systemctl restart myapp'"
