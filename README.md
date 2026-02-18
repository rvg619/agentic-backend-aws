# Agentic Backend AWS

An AI-driven task orchestration backend using Spring Boot, Docker, and AWS, with CI/CD and scalable cloud deployment.

## 🚀 Features

- **AI-Powered Task Processing**: Integration with AWS Bedrock for intelligent task analysis and processing
- **Task Orchestration**: Sophisticated workflow management with multiple agents
- **RESTful API**: Complete REST API for task, agent, and orchestration management
- **Scalable Architecture**: Built with Spring Boot and designed for AWS ECS deployment
- **Docker Support**: Full containerization with Docker and Docker Compose
- **CI/CD Pipeline**: GitHub Actions workflow for automated build, test, and deployment
- **Health Monitoring**: Spring Boot Actuator for health checks and metrics
- **Database Support**: H2 for development, PostgreSQL for production

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Docker and Docker Compose
- AWS Account (for deployment)
- AWS CLI configured (for deployment)

## 🏗️ Architecture

### Components

1. **Task Management**: Create, update, and track tasks with AI processing capabilities
2. **Agent System**: Manage multiple AI agents with different capabilities
3. **Orchestration Engine**: Coordinate complex workflows across multiple tasks
4. **AI Service**: Interface with AWS Bedrock for AI processing

### Tech Stack

- **Backend**: Spring Boot 3.2.0
- **Database**: H2 (dev), PostgreSQL (prod)
- **AI Platform**: AWS Bedrock (Claude)
- **Cloud**: AWS ECS, ECR, RDS
- **CI/CD**: GitHub Actions
- **Containerization**: Docker

## 🛠️ Local Development

### 1. Clone the Repository

```bash
git clone https://github.com/rvg619/agentic-backend-aws.git
cd agentic-backend-aws
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run Locally

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access H2 Console (Development)

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:agenticdb`
- Username: `sa`
- Password: (leave empty)

## 🐳 Docker Deployment

### Using Docker Compose (Recommended)

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database
- Agentic Backend application

### Using Docker Only

```bash
# Build the image
docker build -t agentic-backend-aws .

# Run the container
docker run -p 8080:8080 agentic-backend-aws
```

## ☁️ AWS Deployment

### Prerequisites

1. AWS CLI configured with appropriate credentials
2. ECR repository created
3. ECS cluster set up
4. VPC and subnets configured

### Option 1: Using CloudFormation

```bash
aws cloudformation create-stack \
  --stack-name agentic-backend-stack \
  --template-body file://aws/cloudformation-template.yml \
  --parameters ParameterKey=VpcId,ParameterValue=vpc-xxxxx \
               ParameterKey=SubnetIds,ParameterValue=subnet-xxxxx,subnet-yyyyy \
  --capabilities CAPABILITY_IAM
```

### Option 2: Manual Deployment

1. **Build and push Docker image to ECR:**

```bash
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

docker build -t agentic-backend-aws .
docker tag agentic-backend-aws:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/agentic-backend-aws:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/agentic-backend-aws:latest
```

2. **Register ECS Task Definition:**

```bash
aws ecs register-task-definition --cli-input-json file://aws/ecs-task-definition.json
```

3. **Create or Update ECS Service:**

```bash
aws ecs create-service \
  --cluster agentic-backend-cluster \
  --service-name agentic-backend-service \
  --task-definition agentic-backend-task \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxx],securityGroups=[sg-xxxxx],assignPublicIp=ENABLED}"
```

## 📡 API Endpoints

### Task Management

- `POST /api/tasks` - Create a new task
- `GET /api/tasks` - Get all tasks
- `GET /api/tasks/{id}` - Get task by ID
- `GET /api/tasks/status/{status}` - Get tasks by status
- `GET /api/tasks/pending` - Get pending tasks by priority
- `PUT /api/tasks/{id}` - Update task
- `DELETE /api/tasks/{id}` - Delete task
- `POST /api/tasks/{id}/process` - Process task with AI
- `POST /api/tasks/{id}/assign/{agentName}` - Assign task to agent

### Agent Management

- `POST /api/agents` - Create a new agent
- `GET /api/agents` - Get all agents
- `GET /api/agents/{id}` - Get agent by ID
- `GET /api/agents/name/{name}` - Get agent by name
- `GET /api/agents/idle` - Get idle agents
- `PATCH /api/agents/{id}/status/{status}` - Update agent status
- `DELETE /api/agents/{id}` - Delete agent

### Orchestration Management

- `POST /api/orchestrations` - Create orchestration
- `GET /api/orchestrations` - Get all orchestrations
- `GET /api/orchestrations/{id}` - Get orchestration by ID
- `POST /api/orchestrations/{id}/execute` - Execute orchestration
- `POST /api/orchestrations/{id}/pause` - Pause orchestration
- `POST /api/orchestrations/{id}/resume` - Resume orchestration

### Health & Monitoring

- `GET /actuator/health` - Health check endpoint
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/info` - Application information

## 📝 Example API Usage

### Create a Task

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Analyze customer data",
    "description": "Perform sentiment analysis on customer reviews",
    "priority": "HIGH"
  }'
```

### Process Task with AI

```bash
curl -X POST http://localhost:8080/api/tasks/1/process
```

### Create an Agent

```bash
curl -X POST http://localhost:8080/api/agents \
  -H "Content-Type: application/json" \
  -d '{
    "name": "DataAnalyzer",
    "description": "AI agent for data analysis",
    "type": "AI_PROCESSOR",
    "capabilities": "sentiment analysis, data processing"
  }'
```

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=TaskControllerTest
```

## 🔧 Configuration

### Environment Variables

- `SPRING_PROFILES_ACTIVE`: Active profile (dev/prod)
- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `AWS_REGION`: AWS region
- `AWS_BEDROCK_MODEL_ID`: Bedrock model ID

### Application Properties

Key configurations can be found in:
- `src/main/resources/application.properties` (default/dev)
- `src/main/resources/application-prod.properties` (production)

## 🔐 Security Considerations

- AWS credentials should be managed via IAM roles (for ECS) or AWS Secrets Manager
- Database credentials should be stored in AWS Secrets Manager
- API endpoints can be secured with Spring Security (to be implemented)
- Network security is managed via Security Groups in AWS

## 📊 Monitoring and Observability

The application includes:
- Spring Boot Actuator endpoints for health checks
- CloudWatch Logs integration (in AWS deployment)
- Auto-scaling based on CPU utilization
- Load balancer health checks

## 🚦 CI/CD Pipeline

The GitHub Actions workflow automatically:
1. Builds the application on every push/PR
2. Runs all tests
3. Builds Docker image
4. Pushes to Amazon ECR (on main branch)
5. Deploys to ECS (on main branch)

### Required GitHub Secrets

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👥 Authors

- Initial work - [rvg619](https://github.com/rvg619)

## 🙏 Acknowledgments

- Spring Boot community
- AWS Bedrock team
- Open source contributors
