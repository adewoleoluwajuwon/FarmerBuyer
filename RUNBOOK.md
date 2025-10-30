# FarmerBuyer RUNBOOK

This runbook shows how to start the **Backend (BE)** and **Frontend (FE)** locally, what environment variables are required, how the DB initializes (Flyway), and how to use Paystack test keys.

> **Ports**
> - Backend default: **8085** (configurable via `SERVER_PORT`)
> - Frontend dev: **5173**
> - FE API base: `VITE_API_BASE` (see options below).

---

## 1) Prerequisites

- **Java 17+**
- **Maven 3.9+**
- **Node 18/20 + npm** (Vite)
- **MySQL 8.x** (or compatible)
- Paystack **test** credentials (see §5)

---

## 2) Environment setup

### 2.1 Backend (BE)

Copy the example env to your local shell (.env file optional if you export vars):

```
BE/.env.example   ->   BE/.env    (optional)
```

Required variables (see `BE/.env.example` for canonical names):

```
# DB
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/farmer_buyer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=changeme

# JWT
APP_JWT_SECRET=replace-me-with-64-bytes
APP_JWT_TTL_SECONDS=86400

# OTP
APP_OTP_TTL_SECONDS=300
APP_OTP_MAX_REQUESTS_PER_HOUR=10
APP_OTP_MAX_VERIFY_ATTEMPTS=5
APP_OTP_SECRET=replace-me-too
APP_OTP_ECHO_IN_RESPONSE=true   # set false in prod

# CORS
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173

# Payments (Paystack)
PAYSTACK_SECRET_KEY=sk_test_xxx
PAYSTACK_PUBLIC_KEY=pk_test_xxx
PAYSTACK_BASE_URL=https://api.paystack.co
PAYSTACK_CURRENCY=NGN

# Optional overrides
SERVER_PORT=8085
SPRING_SECURITY_LOG_LEVEL=DEBUG
```

> **Note**: Base `application.properties` is wired to read from these env vars (Phase 0).

### 2.2 Frontend (FE)

Copy the example env to your local dev env:

```
FE/.env.example   ->   FE/.env.local
```

`FE/.env.example` currently contains:

```
VITE_API_BASE=http://localhost:8080/api
```

- If your BE runs on **8085** directly, you can either:
  - Change it to `http://localhost:8085/api`, **OR**
  - Keep it as `/api` and configure a Vite dev proxy from `/api` → `http://localhost:8085`.

---

## 3) Database initialization (Flyway)

1. Create DB (first time only):
   ```sql
   CREATE DATABASE farmer_buyer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Start the backend (next section). Flyway will automatically run migrations on startup.
3. Verify tables (e.g., `app_user`, `crop`, `listing`, `order`, `payment`, `otp_token`, etc.).

---

## 4) Start the services

### 4.1 Backend

**Unix/macOS (bash/zsh):**
```bash
cd BE
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/farmer_buyer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="changeme"
export APP_JWT_SECRET="replace-me-with-64-bytes"
export APP_OTP_SECRET="replace-me-too"
export APP_OTP_ECHO_IN_RESPONSE="true"   # dev only
export APP_CORS_ALLOWED_ORIGINS="http://localhost:5173"
export PAYSTACK_SECRET_KEY="sk_test_xxx"
export PAYSTACK_PUBLIC_KEY="pk_test_xxx"
export SERVER_PORT="8085"

mvn -q spring-boot:run
```

**Windows (PowerShell):**
```powershell
cd BE
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/farmer_buyer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="changeme"
$env:APP_JWT_SECRET="replace-me-with-64-bytes"
$env:APP_OTP_SECRET="replace-me-too"
$env:APP_OTP_ECHO_IN_RESPONSE="true"   # dev only
$env:APP_CORS_ALLOWED_ORIGINS="http://localhost:5173"
$env:PAYSTACK_SECRET_KEY="sk_test_xxx"
$env:PAYSTACK_PUBLIC_KEY="pk_test_xxx"
$env:SERVER_PORT="8085"

mvn -q spring-boot:run
```

Your backend should be available at: `http://localhost:8085/api`

### 4.2 Frontend

```bash
cd FE
cp .env.example .env.local   # first time
npm install
npm run dev
```

Front-end dev server will be at `http://localhost:5173`.

---

## 5) Paystack (Test)

- Use **test** keys in local/dev:
  - `PAYSTACK_PUBLIC_KEY=pk_test_...`
  - `PAYSTACK_SECRET_KEY=sk_test_...`
- Typical flow:
  1. FE calls `POST /api/payments/init` → BE creates a Paystack transaction and returns `authorization_url`.
  2. User completes transaction on Paystack test page.
  3. Paystack **webhook** (to your ngrok/exposed endpoint): `POST /api/payments/paystack/webhook`.
  4. BE validates HMAC and updates `payment` + `order_status`.

> For local testing of webhooks, expose your BE with ngrok:
> ```bash
> ngrok http 8085
> ```
> Then configure the webhook URL on Paystack dashboard to:
> `https://<your-ngrok-subdomain>.ngrok.io/api/payments/paystack/webhook`

---

## 6) Sanity checks (dev)

1. **OTP login** (dev): if `APP_OTP_ECHO_IN_RESPONSE=true`, `/api/auth/otp/request` includes the code in the response.
2. **Crops & Listings**: `GET /api/crops`, `GET /api/listings` should return data (or empty).
3. **Orders**: `POST /api/orders` returns an order with totals; verify idempotency by reusing `Idempotency-Key` header.
4. **Payments**: `POST /api/payments/init` should return a URL; complete test payment; confirm webhook updates the DB.

---

## 7) Troubleshooting

- **CORS**: Ensure `APP_CORS_ALLOWED_ORIGINS` includes your FE origin (e.g., `http://localhost:5173`).
- **Ports**: If BE is not on 8085, update `SERVER_PORT` and `VITE_API_BASE` accordingly.
- **DB auth**: Verify MySQL user/password and that `allowPublicKeyRetrieval=true` is set on first connections.
- **JWT**: Ensure `APP_JWT_SECRET` is non-empty and long enough.
- **Webhooks**: Check signature validation failures in logs; confirm secret and URL.
