#!/bin/bash
# End-to-end demo of ShopKart against local MySQL (root/root) + Redis.
set +e
J17=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin/java
BASE=http://localhost:8085/api
LOG=/tmp/shopkart-backend.log

echo "==================================================================="
echo " STARTING BACKEND (MySQL 127.0.0.1 root/root, Redis 127.0.0.1:6379)"
echo "==================================================================="
cd /Users/kothuri.l/Desktop/shopkart/backend
DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=shopkart DB_USER=root DB_PASSWORD=root \
REDIS_HOST=127.0.0.1 REDIS_PORT=6379 SERVER_PORT=8085 \
JWT_SECRET="local-dev-secret-please-change-me-32-bytes-minimum-value" \
ADMIN_SEED_PASSWORD="Admin@12345" \
"$J17" -jar target/shopkart-backend-1.0.0.jar > "$LOG" 2>&1 &
BACKEND_PID=$!
echo "backend pid=$BACKEND_PID"

# Wait for health
for i in $(seq 1 60); do
  code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/health" 2>/dev/null)
  if [ "$code" = "200" ]; then echo "Backend UP after ${i}s"; break; fi
  sleep 1
done

echo
echo "=== 1) LIST PRODUCTS (public, served via Redis-cached reads) ==="
curl -s "$BASE/products?size=3" | python3 -m json.tool 2>/dev/null | head -40

echo
echo "=== 2) REGISTER a new user ==="
REG=$(curl -s -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d '{"name":"Priyanka","email":"priyanka@test.com","password":"Password123"}')
echo "$REG" | python3 -m json.tool 2>/dev/null || echo "$REG"
TOKEN=$(echo "$REG" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
if [ -z "$TOKEN" ]; then
  echo "(user likely exists — logging in instead)"
  LOGIN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d '{"email":"priyanka@test.com","password":"Password123"}')
  TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
fi
echo "JWT (first 40 chars): ${TOKEN:0:40}..."
AUTH="Authorization: Bearer $TOKEN"

echo
echo "=== 3) ADD items to CART (product 1 x2, product 3 x1) ==="
curl -s -X POST "$BASE/cart" -H "$AUTH" -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}' > /dev/null
curl -s -X POST "$BASE/cart" -H "$AUTH" -H 'Content-Type: application/json' -d '{"productId":3,"quantity":1}' | python3 -m json.tool 2>/dev/null

echo
echo "=== 4) VIEW CART ==="
curl -s "$BASE/cart" -H "$AUTH" | python3 -m json.tool 2>/dev/null

echo
echo "=== 5) CONCURRENT CHECKOUT x3 (Redis distributed lock in action) ==="
echo "    Only ONE should succeed with an order; others get 409 'checkout in progress'."
for n in 1 2 3; do
  ( curl -s -o /tmp/co_$n.json -w "req$n -> HTTP %{http_code}\n" -X POST "$BASE/orders/checkout" -H "$AUTH" ) &
done
wait
echo "--- responses ---"
for n in 1 2 3; do echo "req$n body: $(cat /tmp/co_$n.json)"; done

echo
echo "=== 6) MY ORDERS ==="
curl -s "$BASE/orders" -H "$AUTH" | python3 -m json.tool 2>/dev/null

echo
echo "=== 7) DATABASE ROWS (proof it persisted to your local MySQL) ==="
mysql -h127.0.0.1 -uroot -proot 2>/dev/null -e "
  SELECT 'users' tbl, COUNT(*) n FROM shopkart.users
  UNION ALL SELECT 'products', COUNT(*) FROM shopkart.products
  UNION ALL SELECT 'orders', COUNT(*) FROM shopkart.orders
  UNION ALL SELECT 'order_items', COUNT(*) FROM shopkart.order_items;
  SELECT id,user_id,total,status,created_at FROM shopkart.orders ORDER BY id DESC LIMIT 3;
  SELECT id,name,stock FROM shopkart.products WHERE id IN (1,3);"

echo
echo "=== 8) REDIS KEYS (cache + any lock) ==="
docker exec shopkart-redis redis-cli KEYS '*' 2>/dev/null

echo
echo "=== STOPPING BACKEND ==="
kill $BACKEND_PID 2>/dev/null
echo "done."
