# API Documentation

## Base URL

- Local Development: `http://localhost:8080`
- Production: `http://<load-balancer-dns>`

## Authentication

Currently, the API does not require authentication. In production, consider implementing Spring Security with JWT tokens.

## Task Endpoints

### Create Task

**POST** `/api/tasks`

Create a new task for processing.

**Request Body:**
```json
{
  "title": "Analyze customer feedback",
  "description": "Perform sentiment analysis on the latest customer reviews",
  "priority": "HIGH"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "title": "Analyze customer feedback",
  "description": "Perform sentiment analysis on the latest customer reviews",
  "status": "PENDING",
  "priority": "HIGH",
  "assignedAgent": null,
  "aiPrompt": null,
  "aiResponse": null,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "completedAt": null
}
```

### Get All Tasks

**GET** `/api/tasks`

Retrieve all tasks.

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "Task 1",
    "status": "PENDING",
    ...
  },
  {
    "id": 2,
    "title": "Task 2",
    "status": "COMPLETED",
    ...
  }
]
```

### Get Task by ID

**GET** `/api/tasks/{id}`

Retrieve a specific task by ID.

**Response:** `200 OK` or `404 Not Found`

### Get Tasks by Status

**GET** `/api/tasks/status/{status}`

Retrieve tasks by status.

**Status Values:** `PENDING`, `IN_PROGRESS`, `PROCESSING_AI`, `COMPLETED`, `FAILED`

**Response:** `200 OK`

### Get Pending Tasks

**GET** `/api/tasks/pending`

Retrieve all pending tasks ordered by priority.

**Response:** `200 OK`

### Update Task

**PUT** `/api/tasks/{id}`

Update an existing task.

**Request Body:**
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "priority": "CRITICAL"
}
```

**Response:** `200 OK` or `404 Not Found`

### Delete Task

**DELETE** `/api/tasks/{id}`

Delete a task.

**Response:** `204 No Content`

### Process Task with AI

**POST** `/api/tasks/{id}/process`

Submit a task for AI processing (asynchronous).

**Response:** `202 Accepted`
```json
"Task processing started"
```

### Assign Task to Agent

**POST** `/api/tasks/{id}/assign/{agentName}`

Assign a task to a specific agent.

**Response:** `200 OK` or `404 Not Found`

## Agent Endpoints

### Create Agent

**POST** `/api/agents`

Create a new agent.

**Request Body:**
```json
{
  "name": "DataAnalyzer",
  "description": "AI agent specialized in data analysis",
  "type": "AI_PROCESSOR",
  "capabilities": "sentiment analysis, data classification, trend detection"
}
```

**Agent Types:** `AI_PROCESSOR`, `DATA_ANALYZER`, `TASK_EXECUTOR`, `ORCHESTRATOR`

**Response:** `201 Created`

### Get All Agents

**GET** `/api/agents`

Retrieve all agents.

**Response:** `200 OK`

### Get Agent by ID

**GET** `/api/agents/{id}`

Retrieve a specific agent by ID.

**Response:** `200 OK` or `404 Not Found`

### Get Agent by Name

**GET** `/api/agents/name/{name}`

Retrieve an agent by name.

**Response:** `200 OK` or `404 Not Found`

### Get Idle Agents

**GET** `/api/agents/idle`

Retrieve all idle agents.

**Response:** `200 OK`

### Update Agent Status

**PATCH** `/api/agents/{id}/status/{status}`

Update an agent's status.

**Status Values:** `IDLE`, `BUSY`, `OFFLINE`, `ERROR`

**Response:** `200 OK` or `404 Not Found`

### Delete Agent

**DELETE** `/api/agents/{id}`

Delete an agent.

**Response:** `204 No Content`

## Orchestration Endpoints

### Create Orchestration

**POST** `/api/orchestrations`

Create a new orchestration workflow.

**Request Body:**
```json
{
  "name": "Daily Data Processing",
  "description": "Process all pending tasks in priority order"
}
```

**Response:** `201 Created`

### Get All Orchestrations

**GET** `/api/orchestrations`

Retrieve all orchestrations.

**Response:** `200 OK`

### Get Orchestration by ID

**GET** `/api/orchestrations/{id}`

Retrieve a specific orchestration by ID.

**Response:** `200 OK` or `404 Not Found`

### Execute Orchestration

**POST** `/api/orchestrations/{id}/execute`

Execute an orchestration (asynchronous).

**Response:** `202 Accepted`
```json
"Orchestration execution started"
```

### Pause Orchestration

**POST** `/api/orchestrations/{id}/pause`

Pause a running orchestration.

**Response:** `200 OK` or `404 Not Found`

### Resume Orchestration

**POST** `/api/orchestrations/{id}/resume`

Resume a paused orchestration.

**Response:** `200 OK` or `404 Not Found`

## Health & Monitoring Endpoints

### Health Check

**GET** `/actuator/health`

Check application health.

**Response:** `200 OK`
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Application Metrics

**GET** `/actuator/metrics`

Get available metrics.

**Response:** `200 OK`

### Specific Metric

**GET** `/actuator/metrics/{metricName}`

Get a specific metric value.

Example: `/actuator/metrics/jvm.memory.used`

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters"
}
```

### 404 Not Found
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

## Rate Limiting

Currently, no rate limiting is implemented. Consider adding rate limiting for production use.

## Pagination

For future enhancement, consider implementing pagination for list endpoints:
- Query parameters: `page`, `size`, `sort`
- Response headers: `X-Total-Count`, `X-Page-Number`, `X-Page-Size`

## Best Practices

1. **Idempotency**: Use PUT for updates to ensure idempotent operations
2. **Status Codes**: Follow HTTP status code conventions
3. **Error Handling**: Always include descriptive error messages
4. **Versioning**: Consider adding API versioning (e.g., `/api/v1/tasks`)
5. **Documentation**: Keep this document updated with API changes
