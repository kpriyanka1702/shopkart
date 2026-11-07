# ShopKart — Full-Stack E-Commerce Platform

A production-style e-commerce application built with **React**, **Spring Boot**, **MySQL**, and **Redis**, containerized with **Docker**, deployed via **Helm** on **Kubernetes**, with an automated **CI/CD** pipeline.

## Features

- Product catalog with search & category filtering
- **Discounts** — MRP (struck-through) + “% off” shown on cards and product page
- User registration / login with **JWT** authentication
- Shopping cart (add / update / remove)
- **Cancel order** — restores stock and evicts the product cache
- Two-step checkout with a **mock payment gateway** (Card / UPI / Net Banking, test mode)
  - HMAC-SHA256 signed + server-verified payments; declined payments are retryable
- Order placement, payment status & order history
- **Redis** caching for products + **Redis distributed lock** for checkout (no double orders / overselling)
- Admin-ready product management APIs
- Fully containerized (Docker) + Helm chart for K8s
- GitHub Actions CI/CD (build, test, image push)

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, React Router, Axios, Vite |
| Backend | Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA |
| Database | MySQL 8 |
| Cache | Redis 7 |
| Infra | Docker, Kubernetes, Helm |
| CI/CD | GitHub Actions |

## Architecture

```
React (Nginx)  ──►  Spring Boot API  ──►  MySQL
                          │  │
                          │  └──►  Mock Payment Gateway (HMAC sign/verify)
                          └──►  Redis (product cache + checkout distributed lock)
```

## Payment flow (mock / test mode)

1. Cart → **Proceed to Pay** → `POST /orders/checkout` creates a `PENDING_PAYMENT` order + payment intent.
2. Payment page → `POST /payments/pay` → mock gateway charges the instrument, returns a signed token,
   the server verifies the HMAC signature, then confirms the order (deduct stock, clear cart → `PLACED`).

Test instruments: card `4111 1111 1111 1111` (success) / `4000 0000 0000 0002` (decline);
UPI `success@upi` / `failure@upi`. No real money is charged. Signing key comes from `PAYMENT_GATEWAY_SECRET`.

## Run locally (Docker Compose)

```bash
# From the project root
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- MySQL: localhost:3306
- Redis: localhost:6379

## Run backend only (dev)

```bash
cd backend
export JWT_SECRET="change-me-in-prod-please-use-a-long-random-value"
./mvnw spring-boot:run
```

## Run frontend only (dev)

```bash
cd frontend
npm install
npm run dev
```

## Deploy to Kubernetes (Helm)

```bash
# Build & push images first (see CI/CD), then:
helm upgrade --install shopkart ./helm/shopkart \
  --namespace shopkart --create-namespace \
  --set backend.image.repository=<your-registry>/shopkart-backend \
  --set frontend.image.repository=<your-registry>/shopkart-frontend
```

## Configuration

Secrets are injected via environment variables — **never hardcoded**:

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | Secret key for signing JWT tokens (required) |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | MySQL connection |
| `REDIS_HOST`, `REDIS_PORT` | Redis connection |

## Project structure

```
shopkart/
├── backend/          # Spring Boot API
├── frontend/         # React app
├── helm/shopkart/    # Helm chart for K8s
├── .github/workflows # CI/CD
└── docker-compose.yml
```
