# ShopKart — Build & Run Guide

All commands to build and start the **backend** and **frontend**.

- Backend: Spring Boot (Java 17), MySQL, Redis
- Frontend: React (Vite)
- Local DB used: MySQL `127.0.0.1:3306`, user `root`, password `root`

---

## Quickstart (2 terminals)

> Run Redis first (see §1), then use **two separate terminals** — one for the backend,
> one for the frontend. Keep both running.

**Terminal 1 — Backend** (API on `http://localhost:8085/api`)

```bash
cd ~/Desktop/shopkart/backend
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home mvn clean package -DskipTests

DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=shopkart DB_USER=root DB_PASSWORD=root \
REDIS_HOST=127.0.0.1 REDIS_PORT=6379 SERVER_PORT=8085 \
JWT_SECRET="local-dev-secret-please-change-me-32-bytes-minimum-value" \
PAYMENT_GATEWAY_SECRET="local-mock-gateway-signing-key" \
ADMIN_SEED_PASSWORD="Admin@12345" \
/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin/java -jar target/shopkart-backend-1.0.0.jar
```

**Terminal 2 — Frontend** (UI on `http://localhost:5173`)

```bash
cd ~/Desktop/shopkart/frontend
npm install
npm run dev
```

Then open **http://localhost:5173**. Detailed steps and options are below.

---

## 0. Prerequisites (one-time)

```bash
# Java 17 (used for build & run)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# Verify tools
$JAVA_HOME/bin/java -version      # should print 17.x
node -v                            # should be 18+ (20 recommended)
mysqladmin -h127.0.0.1 -uroot -proot ping   # should say "mysqld is alive"
```

---

## 1. Start Redis (needed for caching + checkout lock)

```bash
docker rm -f shopkart-redis 2>/dev/null
docker run -d --name shopkart-redis -p 6379:6379 redis:7-alpine

# verify
docker exec shopkart-redis redis-cli ping   # -> PONG
```

---

## 2. Backend — build

```bash
cd ~/Desktop/shopkart/backend
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home mvn clean package
```

> Skip tests: add `-DskipTests`

---

## 3. Backend — run (against local MySQL root/root)

```bash
cd ~/Desktop/shopkart/backend

DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=shopkart DB_USER=root DB_PASSWORD=root \
REDIS_HOST=127.0.0.1 REDIS_PORT=6379 SERVER_PORT=8085 \
JWT_SECRET="local-dev-secret-please-change-me-32-bytes-minimum-value" \
PAYMENT_GATEWAY_SECRET="local-mock-gateway-signing-key" \
ADMIN_SEED_PASSWORD="Admin@12345" \
/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin/java -jar target/shopkart-backend-1.0.0.jar
```

- API base URL: `http://localhost:8085/api`
- Health check: `curl http://localhost:8085/api/actuator/health`
- On first run it auto-creates the `shopkart` DB + tables and seeds 88 products
  (~16 per category across Electronics/Fashion/Home/Sports and 12 each for Books/Beauty,
  every product stocked at 100) + an admin user.

> Port note: use `8085` because another service uses `8080` on this machine.
> To use 8080, drop `SERVER_PORT` (and set the frontend proxy to 8080).

---

## 4. Frontend — build & run

### Dev mode (hot reload)

```bash
cd ~/Desktop/shopkart/frontend
npm install
npm run dev
```

- Opens on `http://localhost:5173`
- The dev server proxies `/api` → `http://localhost:8085` (matches the backend port above).
  If you run the backend on a different port, update `frontend/vite.config.js` (`server.proxy['/api'].target`)
  or point the app directly at the API:

```bash
VITE_API_BASE_URL=http://localhost:8085/api npm run dev
```

### Production build

```bash
cd ~/Desktop/shopkart/frontend
npm install
npm run build      # output in dist/
npm run preview    # serves the built app locally
```

---

## 5. Login credentials (seeded)

| Role  | Email                | Password      |
|-------|----------------------|---------------|
| Admin | admin@shopkart.com   | Admin@12345   |

Register any new user from the UI (`/register`).

---

## 6. Quick API smoke test (curl)

```bash
BASE=http://localhost:8085/api

# list products
curl -s "$BASE/products?size=5"

# register + capture token
TOKEN=$(curl -s -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d '{"name":"Test","email":"test@test.com","password":"Password123"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# add to cart
curl -s -X POST "$BASE/cart" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}'
curl -s "$BASE/cart" -H "Authorization: Bearer $TOKEN"

# checkout -> creates a PENDING_PAYMENT order + payment intent (orderId, gatewayOrderId, amount)
ORDER=$(curl -s -X POST "$BASE/orders/checkout" -H "Authorization: Bearer $TOKEN")
echo "$ORDER"
OID=$(echo "$ORDER" | python3 -c "import sys,json;print(json.load(sys.stdin)['orderId'])")

# pay with the TEST success card -> order becomes PLACED, stock deducted, cart cleared
curl -s -X POST "$BASE/payments/pay" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"orderId\":$OID,\"method\":\"CARD\",\"instrument\":\"4111111111111111\"}"

curl -s "$BASE/orders" -H "Authorization: Bearer $TOKEN"
```

---

## 6a. Payment gateway (mock / test mode)

Checkout is now a real 2-step flow like Amazon/Flipkart:

1. **Cart → "Proceed to Pay"** calls `POST /orders/checkout`, which creates a
   `PENDING_PAYMENT` order and a payment intent (`gatewayOrderId`, amount).
2. The **Payment page** (`/payment/:orderId`) lets you pay by **Card / UPI / Net Banking**.
3. `POST /payments/pay` runs a **mock gateway** that charges the instrument, returns an
   **HMAC-SHA256 signature**, the server **verifies the signature**, then confirms the order
   (deducts stock, clears cart, marks it **PLACED**).

No real money or external network calls are involved. The signing key is loaded from
`PAYMENT_GATEWAY_SECRET` (never hardcoded).

### Test instruments

| Method       | Succeeds with           | Fails with              |
|--------------|-------------------------|-------------------------|
| Card         | `4111 1111 1111 1111`   | `4000 0000 0000 0002`   |
| UPI          | `success@upi`           | `failure@upi`           |
| Net Banking  | any bank code (e.g. `HDFC`) | `FAIL`               |

- A **declined** payment keeps the order `PENDING_PAYMENT` so you can retry with a valid instrument.
- A **duplicate** pay on an already-paid order is rejected with `409`.
- Concurrent checkouts for the same user are guarded by a **Redis distributed lock** (one `200`, one `409`).

---

## 7. Inspect the database

```bash
mysql -h127.0.0.1 -uroot -proot shopkart -e "SHOW TABLES;"
mysql -h127.0.0.1 -uroot -proot shopkart -e "SELECT category,COUNT(*),SUM(stock) FROM products GROUP BY category;"
mysql -h127.0.0.1 -uroot -proot shopkart -e "SELECT * FROM orders;"
```

---

## 8. One-shot: everything with Docker Compose (alternative)

Runs MySQL + Redis + backend + frontend together:

```bash
cd ~/Desktop/shopkart
docker compose up --build
```

- Frontend: `http://localhost:3000`
- Backend:  `http://localhost:8080/api`

---

## 9. Stop everything

```bash
# stop backend: Ctrl+C in its terminal, or:
pkill -f shopkart-backend-1.0.0.jar

# stop redis
docker rm -f shopkart-redis

# stop docker compose (if used)
cd ~/Desktop/shopkart && docker compose down
```

---

## 10. Deploy to Kubernetes (Helm) — optional

```bash
helm upgrade --install shopkart ./helm/shopkart \
  --namespace shopkart --create-namespace \
  --set secrets.jwtSecret="<long-random-secret>" \
  --set secrets.paymentGatewaySecret="<long-random-secret>" \
  --set secrets.dbPassword="<db-pass>" \
  --set secrets.dbRootPassword="<root-pass>" \
  --set backend.image.repository=<registry>/shopkart-backend \
  --set frontend.image.repository=<registry>/shopkart-frontend
```
