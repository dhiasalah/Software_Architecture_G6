# Spring Boot Authentication API

REST API with JWT authentication, permission-based authorization, email verification, and full Docker deployment with Nginx load balancing.

## Table of Contents

- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Option 1: Docker Compose (Recommended)](#option-1-docker-compose-recommended)
  - [Option 2: Run Locally](#option-2-run-locally)
- [API Endpoints](#api-endpoints)
- [Using Swagger UI](#using-swagger-ui)
- [Architecture](#architecture)

---

## Tech Stack

| Technology       | Version | Purpose                          |
| ---------------- | ------- | -------------------------------- |
| Java             | 17      | Language                         |
| Spring Boot      | 4.0.2   | Backend framework                |
| Spring Security  | -       | Authentication & authorization   |
| PostgreSQL       | 16      | Database                         |
| JWT (jjwt)       | 0.11.5  | Token-based authentication       |
| RabbitMQ         | 3       | Async messaging (email events)   |
| MailHog          | -       | Dev email capture                |
| Nginx            | -       | API gateway & load balancer      |
| SpringDoc OpenAPI| 2.3.0   | Swagger documentation            |
| Docker Compose   | -       | Container orchestration          |

---

## Getting Started

### Prerequisites

- **Docker & Docker Compose** (for Option 1)
- **Java 17+** and **Maven** (for Option 2)

---

### Option 1: Docker Compose (Recommended)

This starts the full stack: PostgreSQL, RabbitMQ, MailHog, 3 backend instances, and Nginx.

```bash
# Start everything
docker compose up -d --build

# Check that all services are running
docker compose ps

# View logs
docker compose logs -f backend

# Stop everything
docker compose down
```

Once started, the services are available at:

| Service         | URL                          |
| --------------- | ---------------------------- |
| **API (Nginx)** | http://localhost              |
| **Swagger UI**  | http://localhost/swagger-ui/index.html |
| **MailHog UI**  | http://localhost:8025         |
| **RabbitMQ UI** | http://localhost:15672 (guest/guest) |

> All API requests go through Nginx on port **80**. Nginx load-balances across 3 backend instances and validates JWT tokens on protected routes.

---

### Option 2: Run Locally

You need PostgreSQL running locally (or via Docker).

```bash
# 1. Start PostgreSQL
docker run --name postgres \
  -e POSTGRES_PASSWORD=dhia \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_DB=project_spring \
  -p 5432:5432 \
  -d postgres:latest

# 2. (Optional) Start RabbitMQ for email verification
docker run --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -d rabbitmq:3-management

# 3. (Optional) Start MailHog for email capture
docker run --name mailhog \
  -p 1025:1025 -p 8025:8025 \
  -d mailhog/mailhog

# 4. Run the application
./mvnw spring-boot:run
```

The API will be available at http://localhost:8080 and Swagger UI at http://localhost:8080/swagger-ui/index.html.

---

## API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint              | Auth Required | Description                        |
| ------ | --------------------- | ------------- | ---------------------------------- |
| POST   | `/api/auth/register`  | No            | Register a new user                |
| POST   | `/api/auth/login`     | No            | Login and get a JWT token          |
| GET    | `/api/auth/verify`    | No            | Verify email via token link        |
| POST   | `/api/auth/logout`    | Yes           | Invalidate JWT token (blacklist)   |
| GET    | `/api/auth/validate`  | Yes           | Validate JWT (used by Nginx)       |

### User Management (`/api/users`)

| Method | Endpoint           | Permission Required | Description         |
| ------ | ------------------ | ------------------- | ------------------- |
| GET    | `/api/users`       | USER_READ           | List all users      |
| GET    | `/api/users/{id}`  | USER_READ           | Get user by ID      |
| POST   | `/api/users`       | USER_CREATE         | Create a new user   |
| PUT    | `/api/users/{id}`  | USER_UPDATE         | Update a user       |
| DELETE | `/api/users/{id}`  | USER_DELETE          | Delete a user       |

### Roles & Permissions

| Role    | Permissions                                          |
| ------- | ---------------------------------------------------- |
| `ADMIN` | USER_READ, USER_CREATE, USER_UPDATE, USER_DELETE     |
| `USER`  | USER_READ                                            |

### Quick Test with curl

```bash
# 1. Register
curl -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"secret123"}'

# 2. Login (get token)
curl -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"secret123"}'

# 3. Use token to access protected endpoints
curl http://localhost/api/users \
  -H "Authorization: Bearer <paste-token-here>"

# 4. Logout (invalidate token)
curl -X POST http://localhost/api/auth/logout \
  -H "Authorization: Bearer <paste-token-here>"
```

---

## Using Swagger UI

Swagger UI provides an interactive interface to explore and test all API endpoints.

### Step 1: Open Swagger UI

- **Docker Compose**: http://localhost/swagger-ui/index.html
- **Local**: http://localhost:8080/swagger-ui/index.html

### Step 2: Register and Login

1. Expand **POST /api/auth/register** and click **Try it out**
2. Enter the request body:
   ```json
   {
     "username": "john",
     "email": "john@example.com",
     "password": "secret123"
   }
   ```
3. Click **Execute** — you should get a `201` response
4. Expand **POST /api/auth/login** and click **Try it out**
5. Enter:
   ```json
   {
     "username": "john",
     "password": "secret123"
   }
   ```
6. Click **Execute** — copy the `token` value from the response

### Step 3: Authorize

1. Click the **Authorize** button (lock icon) at the top of the page
2. In the **Value** field, type: `Bearer <paste-token-here>`
3. Click **Authorize**, then **Close**

All subsequent requests from Swagger UI will include the JWT token automatically.

### Step 4: Test Protected Endpoints

You can now expand any endpoint under **User Management** and click **Try it out** to test it. The JWT token will be sent automatically in the `Authorization` header.

**Example: Create a user (requires ADMIN role)**

Expand **POST /api/users**, click **Try it out**, and enter:
```json
{
  "username": "newuser",
  "role": { "name": "USER" },
  "credentials": {
    "email": "newuser@example.com",
    "password": "password123"
  }
}
```

> If you get a `403 Forbidden`, your user does not have the required permission. Register with `"roleType": "ADMIN"` to get full access.

### Step 5: View the OpenAPI Spec

The raw JSON spec is available at:
- **Docker Compose**: http://localhost/v3/api-docs
- **Local**: http://localhost:8080/v3/api-docs

---

## Architecture

```
                         ┌──────────────┐
                         │    Client    │
                         └──────┬───────┘
                                │
                                v
                         ┌──────────────┐
                         │    Nginx     │  Port 80
                         │  (Gateway)   │  JWT validation + load balancing
                         └──────┬───────┘
                                │
                 ┌──────────────┼──────────────┐
                 v              v              v
          ┌────────────┐ ┌────────────┐ ┌────────────┐
          │  Backend 1 │ │  Backend 2 │ │  Backend 3 │  Port 8080
          │ Spring Boot│ │ Spring Boot│ │ Spring Boot│
          └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
                │              │              │
                └──────────────┼──────────────┘
                        ┌──────┴──────┐
                        v             v
                 ┌────────────┐ ┌──────────┐
                 │ PostgreSQL │ │ RabbitMQ │
                 │   (DB)     │ │ (Async)  │
                 └────────────┘ └────┬─────┘
                                     │
                                     v
                               ┌──────────┐
                               │ MailHog  │
                               │ (Email)  │
                               └──────────┘
```

### Authentication Flow

1. User registers via `POST /api/auth/register`
2. A verification email is sent asynchronously via RabbitMQ + MailHog
3. User clicks the verification link to activate the account
4. User logs in via `POST /api/auth/login` and receives a JWT token (15 min expiry)
5. Protected endpoints require `Authorization: Bearer <token>` header
6. Nginx validates the token via a subrequest to `/api/auth/validate` before proxying
7. Logout adds the token to a database-backed blacklist shared across all instances

---

**Course**: Introduction to Software Architecture
