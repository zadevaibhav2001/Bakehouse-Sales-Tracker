#!/bin/bash

# Add port 8080 to security group
# Usage: ./add-port-8080.sh <instance-ip>

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <instance-ip>"
    exit 1
fi

INSTANCE_IP=$1

echo "🔍 Finding EC2 instance and security group..."

# Get instance ID from IP
INSTANCE_ID=$(aws ec2 describe-instances \
    --filters "Name=ip-address,Values=$INSTANCE_IP" \
    --query 'Reservations[0].Instances[0].InstanceId' \
    --output text)

if [ "$INSTANCE_ID" == "None" ] || [ -z "$INSTANCE_ID" ]; then
    echo "❌ Could not find instance with IP $INSTANCE_IP"
    exit 1
fi

echo "✅ Found instance: $INSTANCE_ID"

# Get security group ID
SG_ID=$(aws ec2 describe-instances \
    --instance-ids $INSTANCE_ID \
    --query 'Reservations[0].Instances[0].SecurityGroups[0].GroupId' \
    --output text)

echo "✅ Security Group: $SG_ID"
echo ""

# Check if port 8080 is already open
EXISTING=$(aws ec2 describe-security-groups \
    --group-ids $SG_ID \
    --query "SecurityGroups[0].IpPermissions[?FromPort==\`8080\`]" \
    --output json)

if [ "$EXISTING" != "[]" ]; then
    echo "✅ Port 8080 is already open in the security group"
    echo ""
    echo "Current rule:"
    echo "$EXISTING" | jq '.'
    exit 0
fi

echo "📝 Adding port 8080 to security group..."

# Add the rule
aws ec2 authorize-security-group-ingress \
    --group-id $SG_ID \
    --protocol tcp \
    --port 8080 \
    --cidr 0.0.0.0/0 \
    --region ap-south-1

if [ $? -eq 0 ]; then
    echo "✅ Successfully added port 8080 to security group!"
    echo ""
    echo "🧪 Testing connection..."
    sleep 2
    curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://$INSTANCE_IP:8080/api/products || echo "Backend not responding yet"
else
    echo "❌ Failed to add security group rule"
    exit 1
fi
