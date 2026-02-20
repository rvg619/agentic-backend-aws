# Agentic Backend AWS

An intelligent agentic system built with Spring Boot that integrates with Amazon Bedrock for real LLM capabilities.

## Key Features

- 🧠 Agentic workflow engine (Planner → Executor → Critic)
- ⚙️ Spring Boot backend with clean layered architecture
- 🗄️ PostgreSQL for persistence (RDS in production)
- 📦 Artifact storage in S3
- 🐳 Dockerized and deployed on AWS ECS Fargate
- 🚀 CI/CD with GitHub Actions
- 📊 Run tracking, step logs, and failure handling
- 🔍 Production-style logging and health checks

## Tech Stack

- Java 21, Spring Boot
- PostgreSQL
- Docker, GitHub Actions
- AWS: ECS Fargate, ECR, RDS, S3, CloudWatch
- React dashboard

## LLM Integration

The system supports two LLM client implementations:

### 1. Fake LLM Client (Development)
- **Profile**: `fake` (default)
- **Purpose**: Development and testing
- **Features**: Simulated responses with realistic delays
- **Usage**: Activated by default or when `spring.profiles.active=fake`

### 2. AWS Bedrock Client (Production)
- **Profile**: `real`
- **Model**: Anthropic Claude 3 Haiku (`anthropic.claude-3-haiku-20240307-v1:0`)
- **Purpose**: Production use with real AI capabilities
- **Usage**: Activate with `spring.profiles.active=real`

## Running with Different Profiles

### Development Mode (Fake LLM)
```bash
# Default - uses fake LLM client
mvn spring-boot:run

# Or explicitly set fake profile
mvn spring-boot:run -Dspring-boot.run.profiles=fake
```

### Production Mode (AWS Bedrock)
```bash
# Activate real profile for Bedrock integration
mvn spring-boot:run -Dspring-boot.run.profiles=real
```

## AWS Configuration

### Prerequisites
1. AWS CLI configured with appropriate credentials
2. Access to Amazon Bedrock service
3. Claude 3 Haiku model enabled in your AWS region

### AWS Credentials Setup
The application uses AWS DefaultCredentialsProvider which looks for credentials in this order:
1. Environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`)
2. Java system properties
3. AWS credentials file (`~/.aws/credentials`)
4. EC2 instance profile credentials

### Configuration Properties
Configure these properties in `application-real.properties`:

```properties
# AWS Bedrock Configuration
aws.bedrock.region=us-east-1
aws.bedrock.model.id=anthropic.claude-3-haiku-20240307-v1:0
aws.bedrock.model.max-tokens=4000
aws.bedrock.model.temperature=0.7
```

### Available Regions
Ensure Claude 3 Haiku is available in your chosen region:
- `us-east-1` (Virginia) - Recommended
- `us-west-2` (Oregon)
- `eu-west-1` (Ireland)

## Architecture

The system uses a clean interface-based architecture:

```
LlmClient (Interface)
├── FakeLlmClient (@Profile("!real"))
└── BedrockLlmClient (@Profile("real"))
```

### Key Components

1. **LlmClient Interface**: Defines the contract for LLM interactions
2. **BedrockLlmClient**: Production implementation using AWS Bedrock
3. **FakeLlmClient**: Development implementation with simulated responses
4. **Spring Profiles**: Environment-based configuration switching

### Agent Integration

All agents (PlannerAgent, ExecutorAgent, CriticAgent) use dependency injection to receive the appropriate LlmClient implementation based on the active profile.

## API Endpoints

The system provides RESTful endpoints for:
- Task management (`/api/tasks`)
- Run orchestration (`/api/runs`)
- Step execution monitoring

## Development

### Building
```bash
mvn clean compile
```

### Testing
```bash
mvn test
```

### Running
```bash
# Development mode
mvn spring-boot:run

# Production mode
mvn spring-boot:run -Dspring-boot.run.profiles=real
```

## Monitoring

Health checks are available through Spring Actuator:
- Application health: `/actuator/health`
- LLM client health: Integrated into overall health status

## Error Handling

The BedrockLlmClient includes comprehensive error handling:
- Network failures
- Authentication issues
- Rate limiting
- Invalid responses
- Timeout handling

Errors are logged with appropriate detail levels and converted to meaningful user-facing messages.
