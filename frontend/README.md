# Goodwell Donation Management

The frontend is a Next.js client for the Spring Boot API in `../backend`. It includes sign-in, a dashboard, transaction lists and entry forms, and account settings.

## Run locally

1. Start the backend from `../backend` with `mvn spring-boot:run`. The backend expects MySQL at `localhost:3306` unless its `DB_*` settings are overridden.
2. Copy `.env.local.example` to `.env.local` if needed. By default, the browser sends API requests to the same-origin `/api` route and Next.js proxies them to `http://127.0.0.1:8080`. This keeps the authentication cookies same-origin and avoids browser CORS issues.
3. Start the frontend:

```bash
npm install
npm run dev
```

Open the URL printed by Next.js. To use a different backend host, change `API_PROXY_TARGET`; keep `NEXT_PUBLIC_API_URL=/api` so browser requests continue using the same-origin proxy.

For deployments, configure the backend's `CORS_ALLOWED_ORIGINS` with the public frontend origin(s), and point `API_PROXY_TARGET` at the backend service reachable from the Next.js server.

## Validate

```bash
npm run lint
npm run build
```

Each screen and action requires the corresponding permission assigned to the signed-in user by the backend.
