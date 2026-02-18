# AWS Deployment Guide

This guide provides detailed instructions for deploying the Agentic Backend to AWS.

## Prerequisites

1. AWS Account with appropriate permissions
2. AWS CLI installed and configured
3. Docker installed locally
4. Maven installed locally

## Architecture Overview

The application is deployed using:
- **ECS Fargate**: For running containerized application
- **Application Load Balancer**: For distributing traffic
- **RDS PostgreSQL**: For database (optional, can use H2 initially)
- **ECR**: For storing Docker images
- **CloudWatch**: For logging and monitoring
- **Secrets Manager**: For storing sensitive configuration

## Step 1: Create ECR Repository

```bash
aws ecr create-repository \
  --repository-name agentic-backend-aws \
  --region us-east-1
```

## Step 2: Build and Push Docker Image

```bash
# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build the image
docker build -t agentic-backend-aws .

# Tag the image
docker tag agentic-backend-aws:latest \
  <account-id>.dkr.ecr.us-east-1.amazonaws.com/agentic-backend-aws:latest

# Push to ECR
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/agentic-backend-aws:latest
```

## Step 3: Create Database (Optional)

If using RDS PostgreSQL:

```bash
aws rds create-db-instance \
  --db-instance-identifier agentic-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username admin \
  --master-user-password <your-password> \
  --allocated-storage 20 \
  --vpc-security-group-ids sg-xxxxx \
  --db-subnet-group-name my-subnet-group
```

## Step 4: Store Secrets in AWS Secrets Manager

```bash
# Database credentials
aws secretsmanager create-secret \
  --name agentic-backend/db \
  --secret-string '{
    "username":"admin",
    "password":"<your-password>",
    "url":"jdbc:postgresql://<rds-endpoint>:5432/agenticdb"
  }'
```

## Step 5: Deploy using CloudFormation

```bash
aws cloudformation create-stack \
  --stack-name agentic-backend-stack \
  --template-body file://aws/cloudformation-template.yml \
  --parameters \
    ParameterKey=VpcId,ParameterValue=vpc-xxxxx \
    ParameterKey=SubnetIds,ParameterValue=subnet-xxxxx,subnet-yyyyy \
    ParameterKey=ImageUri,ParameterValue=<account-id>.dkr.ecr.us-east-1.amazonaws.com/agentic-backend-aws:latest \
  --capabilities CAPABILITY_IAM

# Monitor stack creation
aws cloudformation wait stack-create-complete \
  --stack-name agentic-backend-stack
```

## Step 6: Get Load Balancer URL

```bash
aws cloudformation describe-stacks \
  --stack-name agentic-backend-stack \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' \
  --output text
```

## Step 7: Test the Deployment

```bash
# Health check
curl http://<load-balancer-dns>/actuator/health

# Create a task
curl -X POST http://<load-balancer-dns>/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Task",
    "description": "Test deployment",
    "priority": "MEDIUM"
  }'
```

## Monitoring

### CloudWatch Logs

View application logs:

```bash
aws logs tail /ecs/agentic-backend --follow
```

### CloudWatch Metrics

View ECS metrics in the AWS Console:
1. Navigate to CloudWatch > Metrics
2. Select ECS namespace
3. View CPU, Memory, and other metrics

### ECS Service Status

```bash
aws ecs describe-services \
  --cluster agentic-backend-cluster \
  --services agentic-backend-service
```

## Scaling

### Manual Scaling

```bash
aws ecs update-service \
  --cluster agentic-backend-cluster \
  --service agentic-backend-service \
  --desired-count 5
```

### Auto Scaling (Already configured in CloudFormation)

The application is configured to:
- Minimum tasks: 2
- Maximum tasks: 10
- Scale out when CPU > 70%
- Scale in when CPU < 70% for 5 minutes

## Updating the Application

1. Build and push new image with a new tag:

```bash
docker build -t agentic-backend-aws .
docker tag agentic-backend-aws:latest \
  <account-id>.dkr.ecr.us-east-1.amazonaws.com/agentic-backend-aws:v1.1.0
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/agentic-backend-aws:v1.1.0
```

2. Update ECS service:

```bash
aws ecs update-service \
  --cluster agentic-backend-cluster \
  --service agentic-backend-service \
  --force-new-deployment
```

## Cost Optimization

1. **Use Fargate Spot**: Already configured in CloudFormation (50/50 split)
2. **Right-size tasks**: Adjust CPU/Memory based on actual usage
3. **Use RDS Reserved Instances**: For production databases
4. **Enable CloudWatch Logs retention**: Set appropriate retention period

## Troubleshooting

### Service Not Starting

1. Check ECS service events:
```bash
aws ecs describe-services \
  --cluster agentic-backend-cluster \
  --services agentic-backend-service
```

2. Check CloudWatch Logs for errors

3. Verify security group rules allow traffic

### Health Checks Failing

1. Verify the application is running: Check CloudWatch Logs
2. Ensure security groups allow traffic from ALB
3. Check target group health in AWS Console

### Database Connection Issues

1. Verify RDS security group allows traffic from ECS tasks
2. Check database credentials in Secrets Manager
3. Verify database endpoint and port

## Cleanup

To delete all resources:

```bash
# Delete CloudFormation stack
aws cloudformation delete-stack --stack-name agentic-backend-stack

# Delete ECR images
aws ecr batch-delete-image \
  --repository-name agentic-backend-aws \
  --image-ids imageTag=latest

# Delete ECR repository
aws ecr delete-repository \
  --repository-name agentic-backend-aws --force
```

## Security Best Practices

1. Use IAM roles instead of access keys
2. Store sensitive data in Secrets Manager
3. Enable encryption at rest for RDS and EBS
4. Use VPC endpoints for AWS services
5. Implement least privilege access
6. Enable AWS CloudTrail for audit logging
7. Use AWS WAF for API protection (optional)

## High Availability

The deployment includes:
- Multi-AZ deployment for ECS tasks
- Application Load Balancer across multiple AZs
- Auto-scaling based on metrics
- Health checks and automatic recovery
