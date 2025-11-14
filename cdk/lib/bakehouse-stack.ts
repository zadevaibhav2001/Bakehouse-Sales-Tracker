import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';
import * as fs from 'fs';
import * as path from 'path';

export class BakehouseStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Create VPC
    const vpc = new ec2.Vpc(this, 'BakehouseVPC', {
      maxAzs: 2,
      natGateways: 0, // Use 0 for cost savings, instances will use public subnets
      subnetConfiguration: [
        {
          cidrMask: 24,
          name: 'Public',
          subnetType: ec2.SubnetType.PUBLIC,
        },
      ],
    });

    // Security Group for EC2
    const securityGroup = new ec2.SecurityGroup(this, 'BakehouseSecurityGroup', {
      vpc,
      description: 'Security group for The Bake House application',
      allowAllOutbound: true,
    });

    // Allow HTTP traffic
    securityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(80),
      'Allow HTTP traffic'
    );

    // Allow HTTPS traffic
    securityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(443),
      'Allow HTTPS traffic'
    );

    // Allow SSH for management
    securityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(22),
      'Allow SSH access'
    );

    // Allow Spring Boot backend port
    securityGroup.addIngressRule(
      ec2.Peer.anyIpv4(),
      ec2.Port.tcp(8080),
      'Allow Spring Boot backend'
    );

    // IAM Role for EC2
    const role = new iam.Role(this, 'BakehouseEC2Role', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchAgentServerPolicy'),
      ],
    });

    // User Data Script
    const userDataScript = ec2.UserData.forLinux();
    userDataScript.addCommands(
      '#!/bin/bash',
      'set -e',
      '',
      '# Update system',
      'yum update -y',
      '',
      '# Install Java 17',
      'yum install -y java-17-amazon-corretto-devel',
      '',
      '# Install Maven',
      'cd /opt',
      'wget https://dlcdn.apache.org/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz',
      'tar xzf apache-maven-3.9.5-bin.tar.gz',
      'ln -s apache-maven-3.9.5 maven',
      'echo "export M2_HOME=/opt/maven" >> /etc/profile.d/maven.sh',
      'echo "export PATH=\\${M2_HOME}/bin:\\${PATH}" >> /etc/profile.d/maven.sh',
      'source /etc/profile.d/maven.sh',
      '',
      '# Install Nginx',
      'amazon-linux-extras install -y nginx1',
      '',
      '# Install Git',
      'yum install -y git',
      '',
      '# Create application directory',
      'mkdir -p /opt/bakehouse',
      'cd /opt/bakehouse',
      '',
      '# Clone or copy application (you\'ll need to update this with your repo)',
      '# For now, we\'ll create placeholder directories',
      'mkdir -p myapp frontend',
      '',
      '# Configure Nginx',
      'cat > /etc/nginx/conf.d/bakehouse.conf << \'EOF\'',
      'server {',
      '    listen 80;',
      '    server_name _;',
      '',
      '    # Frontend',
      '    location / {',
      '        root /opt/bakehouse/frontend;',
      '        index index.html;',
      '        try_files $uri $uri/ /index.html;',
      '    }',
      '',
      '    # Backend API',
      '    location /api/ {',
      '        proxy_pass http://localhost:8080/api/;',
      '        proxy_set_header Host $host;',
      '        proxy_set_header X-Real-IP $remote_addr;',
      '        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;',
      '        proxy_set_header X-Forwarded-Proto $scheme;',
      '    }',
      '}',
      'EOF',
      '',
      '# Create systemd service for Spring Boot',
      'cat > /etc/systemd/system/bakehouse-backend.service << \'EOF\'',
      '[Unit]',
      'Description=The Bake House Backend Service',
      'After=network.target',
      '',
      '[Service]',
      'Type=simple',
      'User=ec2-user',
      'WorkingDirectory=/opt/bakehouse/myapp',
      'ExecStart=/opt/maven/bin/mvn spring-boot:run',
      'Restart=always',
      'RestartSec=10',
      'StandardOutput=journal',
      'StandardError=journal',
      'SyslogIdentifier=bakehouse-backend',
      '',
      '[Install]',
      'WantedBy=multi-user.target',
      'EOF',
      '',
      '# Set permissions',
      'chown -R ec2-user:ec2-user /opt/bakehouse',
      '',
      '# Enable and start services',
      'systemctl enable nginx',
      'systemctl start nginx',
      'systemctl enable bakehouse-backend',
      '',
      '# Create deployment script',
      'cat > /opt/bakehouse/deploy.sh << \'EOF\'',
      '#!/bin/bash',
      'cd /opt/bakehouse',
      '',
      '# Build backend',
      'cd myapp',
      '/opt/maven/bin/mvn clean package -DskipTests',
      '',
      '# Restart backend service',
      'sudo systemctl restart bakehouse-backend',
      '',
      '# Restart nginx',
      'sudo systemctl restart nginx',
      '',
      'echo "Deployment complete!"',
      'EOF',
      '',
      'chmod +x /opt/bakehouse/deploy.sh',
      'chown ec2-user:ec2-user /opt/bakehouse/deploy.sh',
      '',
      '# Create README',
      'cat > /opt/bakehouse/README.md << \'EOF\'',
      '# The Bake House Deployment',
      '',
      '## Application Structure',
      '- `/opt/bakehouse/myapp` - Spring Boot backend',
      '- `/opt/bakehouse/frontend` - Frontend files',
      '',
      '## Services',
      '- Backend: systemctl status bakehouse-backend',
      '- Nginx: systemctl status nginx',
      '',
      '## Logs',
      '- Backend: journalctl -u bakehouse-backend -f',
      '- Nginx: tail -f /var/log/nginx/error.log',
      '',
      '## Deploy',
      'Run: /opt/bakehouse/deploy.sh',
      '',
      '## Upload Files',
      'Use SCP or AWS Systems Manager Session Manager to upload your application files.',
      'EOF',
      '',
      'echo "Setup complete! Upload your application files to /opt/bakehouse"'
    );

    // EC2 Instance - FREE TIER ELIGIBLE (t2.micro)
    const instance = new ec2.Instance(this, 'BakehouseInstance', {
      vpc,
      instanceType: ec2.InstanceType.of(
        ec2.InstanceClass.T2,
        ec2.InstanceSize.MICRO  // Free tier: 750 hours/month
      ),
      machineImage: ec2.MachineImage.latestAmazonLinux2(),
      securityGroup,
      role,
      userData: userDataScript,
      vpcSubnets: {
        subnetType: ec2.SubnetType.PUBLIC,
      },
      blockDevices: [
        {
          deviceName: '/dev/xvda',
          volume: ec2.BlockDeviceVolume.ebs(30, {  // Free tier: 30 GB
            volumeType: ec2.EbsDeviceVolumeType.GP2,  // Free tier eligible
            deleteOnTermination: true,
          }),
        },
      ],
      keyName: 'bakehouse-key', // You'll need to create this key pair in AWS Console
    });

    // Outputs
    new cdk.CfnOutput(this, 'InstancePublicIP', {
      value: instance.instancePublicIp,
      description: 'Public IP address of the EC2 instance',
    });

    new cdk.CfnOutput(this, 'WebsiteURL', {
      value: `http://${instance.instancePublicDnsName}`,
      description: 'URL to access The Bake House application',
    });

    new cdk.CfnOutput(this, 'SSHCommand', {
      value: `ssh -i bakehouse-key.pem ec2-user@${instance.instancePublicDnsName}`,
      description: 'SSH command to connect to the instance',
    });

    new cdk.CfnOutput(this, 'DeploymentInstructions', {
      value: 'Upload your application files to /opt/bakehouse and run /opt/bakehouse/deploy.sh',
      description: 'How to deploy your application',
    });
  }
}
