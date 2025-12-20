#!/bin/bash

################################################################################
# SSL Certificate Setup for thebakehousebyishita.com
# 
# Sets up Let's Encrypt SSL certificates and updates Nginx configuration
# 
# Usage: ./setup-ssl.sh <instance-ip> <path-to-key.pem>
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
DOMAIN="thebakehousebyishita.com"
WILDCARD_DOMAIN="*.thebakehousebyishita.com"

if [ ! -f "$KEY_PATH" ]; then
    echo -e "${RED}Error: Key file not found: $KEY_PATH${NC}"
    exit 1
fi

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   SSL Certificate Setup for $DOMAIN   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Target: $INSTANCE_IP${NC}"
echo -e "${GREEN}Domain: $DOMAIN${NC}"
echo ""

# Setup SSL on EC2
ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no -o LogLevel=ERROR ec2-user@$INSTANCE_IP bash << ENDSSH
set -e

echo "→ Installing Certbot..."
sudo yum update -y
sudo yum install -y certbot python3-certbot-nginx

echo "→ Updating Nginx configuration for wildcard domain..."
sudo tee /etc/nginx/conf.d/bakehouse.conf > /dev/null << 'EOF'
server {
    listen 80;
    server_name thebakehousebyishita.com *.thebakehousebyishita.com;

    # Frontend
    root /opt/bakehouse/frontend;
    index index.html;

    # Serve frontend files
    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # Proxy API requests to Spring Boot backend
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
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

echo "→ Testing Nginx configuration..."
sudo nginx -t

echo "→ Reloading Nginx..."
sudo systemctl reload nginx

echo "→ Obtaining wildcard SSL certificate..."
echo "NOTE: You need to SSH into the server and run certbot manually."
echo "Run this command on the EC2 server:"
echo "sudo certbot certonly --manual --preferred-challenges dns -d thebakehousebyishita.com -d '*.thebakehousebyishita.com' --agree-tos --email zadevaibhav2001@gmail.com"
echo ""
echo "After getting the certificate, the script will continue..."
read -p "Press Enter when you have completed the SSL certificate setup..."

echo "→ Configuring Nginx SSL..."
sudo tee /etc/nginx/conf.d/bakehouse.conf > /dev/null << 'EOF'
server {
    listen 80;
    server_name thebakehousebyishita.com *.thebakehousebyishita.com;
    return 301 https://\$server_name\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name thebakehousebyishita.com *.thebakehousebyishita.com;

    ssl_certificate /etc/letsencrypt/live/thebakehousebyishita.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/thebakehousebyishita.com/privkey.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # Frontend
    root /opt/bakehouse/frontend;
    index index.html;

    # Serve frontend files
    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # Proxy API requests to Spring Boot backend
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
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

echo "→ Setting up auto-renewal..."
sudo crontab -l 2>/dev/null | { cat; echo "0 12 * * * /usr/bin/certbot renew --quiet"; } | sudo crontab -

echo ""
echo "═══════════════════════════════════════════════════════"
echo "SSL Certificate Status:"
echo "═══════════════════════════════════════════════════════"

# Check certificate
if sudo certbot certificates | grep -q "thebakehousebyishita.com"; then
    echo "✓ SSL Certificate: INSTALLED"
    sudo certbot certificates | grep -A 5 "thebakehousebyishita.com"
else
    echo "✗ SSL Certificate: NOT FOUND"
fi

echo ""
echo "Testing HTTPS..."
sleep 3

# Test HTTPS
HTTPS_CODE=\$(curl -s -o /dev/null -w "%{http_code}" https://thebakehousebyishita.com/ 2>&1 || echo "000")
if [ "\$HTTPS_CODE" = "200" ]; then
    echo "✓ HTTPS: Working (HTTP 200)"
else
    echo "○ HTTPS: HTTP \$HTTPS_CODE"
fi

ENDSSH

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║                 SSL SETUP COMPLETE!                   ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo ""
echo -e "${GREEN}Your site is now available at:${NC}"
echo -e "${GREEN}  https://thebakehousebyishita.com${NC}"
echo -e "${GREEN}  https://*.thebakehousebyishita.com (any subdomain)${NC}"
echo ""
echo -e "${YELLOW}Note: Wildcard certificate covers all subdomains${NC}"
echo -e "${YELLOW}Make sure your domain DNS points to $INSTANCE_IP${NC}"
echo ""