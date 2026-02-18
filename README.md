# Agentic Backend on AWS

This project is a production-style, backend-heavy agentic AI system built with **Spring Boot** and deployed on **AWS ECS Fargate**.  
It demonstrates how to design and operate an **asynchronous, multi-step AI workflow engine** with persistent execution steps, artifact storage, and a full **CI/CD pipeline**.

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
- (Optional) React dashboard
