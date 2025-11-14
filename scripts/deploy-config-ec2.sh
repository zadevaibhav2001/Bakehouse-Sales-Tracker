#!/bin/bash

# Deploy application configuration to EC2
# Usage: ./deploy-config-ec2.sh <instance-ip> <path-to-key>

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <instance-ip> <path-to-key.pem>"
    exit 1
fi

INSTANCE_IP=$1
KEY_PATH=$2

echo "📦 Deploying application configuration to EC2..."
echo ""

# Copy application.yml to EC2
echo "📤 Uploading application.yml..."
scp -i "$KEY_PATH" -o StrictHostKeyChecking=no \
    myapp/src/main/resources/application.yml \
    ec2-user@$INSTANCE_IP:/tmp/application.yml

# Move it to the correct location and restart service
ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no ec2-user@$INSTANCE_IP << 'ENDSSH'
set -e

echo "📝 Moving configuration file..."
sudo mv /tmp/application.yml /opt/bakehouse/myapp/application.yml
sudo chown ec2-user:ec2-user /opt/bakehouse/myapp/application.yml

echo "📁 Creating data directory..."
sudo mkdir -p /opt/bakehouse/myapp/data
sudo chown ec2-user:ec2-user /opt/bakehouse/myapp/data

echo "🔄 Updating systemd service to use correct working directory..."
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
ExecStart=/usr/lib/jvm/java-17-amazon-corretto/bin/java -jar $JAR_FILE --spring.config.location=file:/opt/bakehouse/myapp/application.yml
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=bakehouse-backend

[Install]
WantedBy=multi-user.target
EOF

echo "🔄 Reloading systemd and restarting service..."
sudo systemctl daemon-reload
sudo systemctl restart bakehouse-backend

echo ""
echo "⏳ Waiting for backend to start (15 seconds)..."
sleep 15

echo ""
echo "📊 Service status:"
sudo systemctl status bakehouse-backend --no-pager -l | head -20

echo ""
echo "📋 Recent logs:"
sudo journalctl -u bakehouse-backend -n 50 --no-pager

ENDSSH

echo ""
echo "✅ Configuration deployment complete!"
echo ""
echo "📋 To check logs:"
echo "   ssh -i $KEY_PATH ec2-user@$INSTANCE_IP 'sudo journalctl -u bakehouse-backend -f'"
echo ""
echo "🧪 Test the API:"
echo "   curl http://$INSTANCE_IP:8080/api/products"
