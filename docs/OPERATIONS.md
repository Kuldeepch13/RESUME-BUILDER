# Security And Operations

## Configuration

Copy `.env.example` into a local `.env` for Docker only. Production secrets belong in AWS Secrets Manager, GCP Secret Manager, Vault or an equivalent platform secret store, not in images or CI logs.

Mandatory production variables:

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Random secret of at least 64 characters; rotate using a key/version strategy before scale |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | managed PostgreSQL connectivity |
| `REDIS_HOST`, `REDIS_PASSWORD` | shared cache/rate-limit store |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | optional OAuth sign-in |
| `OPENAI_API_KEY`, `OPENAI_MODEL` | AI provider credentials and evaluated model selection |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | hosted billing and verified callbacks |
| `S3_*` | private artifact bucket configuration |
| `APP_PUBLIC_URL`, `CORS_ORIGINS`, `COOKIE_SECURE=true` | browser security boundary |

## Security Posture

- Passwords are BCrypt encoded at cost 12. Email verification is required for local login.
- Access JWTs expire after 15 minutes. Thirty-day refresh tokens are opaque, stored as SHA-256 digests and rotated on use.
- OAuth sends a short-lived single-use exchange code to the client; the OAuth session is invalidated after callback.
- Spring Data parameterization prevents SQL injection. The rendering layer HTML-escapes user data before PDF/HTML output.
- CORS and WebSocket origins are environment constrained. The web container supplies CSP, frame, MIME-sniffing and referrer headers.
- Stripe determines entitlement state only through timestamped HMAC-verified, replay-protected webhooks.
- Analytics stores hashed visitor addresses. Define retention and data subject deletion policies before public launch.
- File artifacts use private S3-compatible storage and server-side encryption requests. Production downloads should move to short-lived signed URLs plus malware inspection for imported files.

Before public release, add refresh-token logout/revocation-all endpoints, password breach screening, MFA for admins, OAuth account-link confirmation, content moderation policy, dependency/SAST/DAST scans and penetration testing.

## Deployment Shape

### AWS

Use Route 53 and CloudFront/WAF in front of an ALB, ECS/Fargate or EKS for `web` and `api`, Aurora PostgreSQL, ElastiCache Redis, private S3 with KMS encryption, SES email and Secrets Manager. Route logs/traces to CloudWatch and OpenTelemetry collection. Stripe and OAuth callbacks terminate at the public HTTPS origin.

### GCP

Equivalent services are Cloud Load Balancing/Armor, Cloud Run or GKE, Cloud SQL PostgreSQL, Memorystore Redis, Cloud Storage, Secret Manager and a transactional email provider.

## Reliability And Performance

| Concern | Action |
| --- | --- |
| Database | pooled connections, Flyway at deploy, backups and point-in-time recovery; replicas for administration |
| AI latency/cost | async status model, quotas, token logging, provider timeouts/retries, prompt/model evaluations |
| PDF spikes | move `export_jobs` processing to queue workers; cache identical immutable versions |
| API abuse | CDN/WAF limits first, Redis app limit second, endpoint-specific AI/export quotas |
| Object storage | lifecycle/archive old files, signed URLs, per-tenant key prefix |
| Observability | health probes, Prometheus metrics, structured logs with request IDs, tracing and alerting |

## Local Run

```bash
docker compose up --build
```

Mail verification links arrive in Mailpit at `http://localhost:8025`. MinIO files can be inspected at `http://localhost:9001`. Swagger is exposed by the API at `http://localhost:8080/swagger-ui.html`.

