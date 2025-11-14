#!/bin/bash

# Final fix: Rebuild JAR properly on EC2 with all dependencies
# Usage: ./final-fix-ec2.sh <instance-ip> <path-to-key>

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <instance-ip> <path-to-key.pem>"
    exit 1
fi

INSTANCE_IP=$1
KEY_PATH=$2

echo "🔨 Final fix: Rebuilding application properly on EC2..."
echo ""

ssh -i "$KEY_PATH" -o StrictHostKeyChecking=no ec2-user@$INSTANCE_IP << 'ENDSSH'
set -e

echo "🛑 Stopping service..."
sudo systemctl stop bakehouse-backend

echo "🧹 Cleaning everything..."
cd /opt/bakehouse/myapp
rm -rf target

echo "📦 Checking Maven..."
if [ ! -f "/opt/maven/bin/mvn" ]; then
    echo "Installing Maven..."
    cd /opt
    sudo wget -q https://dlcdn.apache.org/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz
    sudo tar xzf apache-maven-3.9.5-bin.tar.gz
    sudo ln -s apache-maven-3.9.5 maven
    sudo rm apache-maven-3.9.5-bin.tar.gz
fi

echo "🔨 Building application with all dependencies..."
cd /opt/bakehouse/myapp
/opt/maven/bin/mvn clean package -DskipTests -X 2>&1 | tail -50

echo ""
echo "✅ Build complete. Checking JAR..."
JAR_FILE=$(find target -name "*.jar" -type f | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ Build failed - no JAR file created!"
    exit 1
fi

echo "Found JAR: $JAR_FILE"
echo "JAR size: $(du -h $JAR_FILE | cut -f1)"

echo ""
echo "🔍 Verifying JAR contents..."
echo "Checking for SQLite driver..."
jar tf "$JAR_FILE" | grep "org/xerial/sqlite" | head -5 || echo "⚠️  SQLite driver not found"

echo "Checking for SLF4J..."
jar tf "$JAR_FILE" | grep "org/slf4j" | head -5 || echo "⚠️  SLF4J not found"

echo ""
echo "🔧 Creating simple systemd service (run JAR directly)..."

sudo tee /etc/systemd/system/bakehouse-backend.service > /dev/null << EOF
[Unit]
Description=The Bake House Backend Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/bakehouse/myapp
Environment="JAVA_HOME=/usr/lib/jvm/java-17-amazon-corretto"
ExecStart=/usr/lib/jvm/java-17-amazon-corretto/bin/java -jar /opt/bakehouse/myapp/$JAR_FILE
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=bakehouse-backend

[Install]
WantedBy=multi-user.target
EOF

echo "✅ Service file created"

echo ""
echo "🔄 Starting service..."
sudo systemctl daemon-reload
sudo systemctl start bakehouse-backend

echo ""
echo "⏳ Waiting for application to start (30 seconds)..."
for i in {1..30}; do
    sleep 1
    if sudo netstat -tlnp 2>/dev/null | grep -q ":8080"; then
        echo "✅ Port 8080 is now listening!"
        break
    fi
    echo -n "."
done
echo ""

echo ""
echo "📊 Service status:"
sudo systemctl status bakehouse-backend --no-pager -l | head -20

echo ""
echo "📋 Recent logs:"
sudo journalctl -u bakehouse-backend -n 40 --no-pager | tail -25

echo ""
echo "🔍 Port check:"
sudo netstat -tlnp | grep 8080 && echo "✅ SUCCESS!" || echo "❌ Still not listening"

echo ""
echo "🧪 Testing API:"
curl -s http://localhost:8080/api/products 2>&1 | head -10 || echo "API test failed"

ENDSSH

echo ""
echo "✅ Rebuild complete!"
echo ""
echo "🌐 Test from your machine:"
echo "   curl http://$INSTANCE_IP:8080/api/products"
echo ""
echo "📋 Monitor logs:"
echo "   ssh -i $KEY_PATH ec2-user@$INSTANCE_IP 'sudo journalctl -u bakehouse-backend -f'"
