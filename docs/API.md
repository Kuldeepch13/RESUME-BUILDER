# REST API Contract

All application endpoints are versioned below `/api/v1`. Protected requests use `Authorization: Bearer <access-token>`. Interactive documentation is available at `/swagger-ui.html` when the backend runs; machine-readable OpenAPI is at `/api-docs`.

## Authentication And Account

| Method | Endpoint | Purpose | Validation / security |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | Email registration and verification delivery | email, display name <= 120, password 12-128 |
| `POST` | `/auth/verify-email` | Consume one-time verification token | hashed token, 24-hour expiry |
| `POST` | `/auth/login` | Issue JWT and refresh token | verified account required |
| `POST` | `/auth/refresh` | Rotate refresh token | prior opaque token consumed |
| `POST` | `/auth/forgot-password` | Request reset email | non-enumerating response |
| `POST` | `/auth/reset-password` | Consume reset token | 30-minute expiry |
| `GET` | `/oauth2/authorization/google` | Begin Google OAuth | redirects to one-time exchange |
| `POST` | `/auth/oauth/exchange` | Exchange OAuth code for tokens | 2-minute single use |
| `GET/PATCH` | `/account` | View/update profile preferences | authenticated owner |

## Resumes And Templates

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/templates` | Active marketplace catalog; publicly cacheable |
| `POST` | `/resumes` | Create resume from a template |
| `GET` | `/resumes?page=0&size=20&sort=updatedAt,desc` | Owner-scoped list |
| `GET` | `/resumes/{id}` | Load builder document |
| `PUT` | `/resumes/{id}/autosave` | Update sections/theme without history noise |
| `POST` | `/resumes/{id}/versions` | Save named immutable history revision |
| `GET` | `/resumes/{id}/versions` | Version timeline |
| `POST` | `/resumes/{id}/duplicate` | Copy document and initial history |

Sections are ordered JSON blocks. Each contains `id`, `type`, `title`, `order`, `visible` and `content`; unknown custom `type` values remain renderable. Template code must identify an active catalog entry.

## AI

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/resumes/{id}/analyses` | Queue ATS/job match analysis; returns `202` |
| `GET` | `/analyses/{id}` | Poll `QUEUED`, `PROCESSING`, `COMPLETED` or `FAILED` result |
| `POST` | `/ai/generate` | Generate safe draft sections from prompt |

Analysis results contain `atsScore`, `matchPercentage`, `missingKeywords`, `suggestions`, `strongSections` and `weakSections`. Prompt input is length limited. Free usage is throttled; token counts and configured model are persisted for cost reporting. Without an API key, local deterministic feedback is returned so local development remains functional.

## Delivery And Analytics

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/resumes/{id}/export?format=PDF` | Download `PDF`, `DOCX`, `TXT`, `HTML` or `JSON` |
| `POST` | `/resumes/{id}/shares?expiresAt=...` | Create public expiring URL |
| `DELETE` | `/shares/{id}` | Disable URL |
| `GET` | `/shares/{id}/qr` | QR PNG for a URL |
| `GET` | `/resumes/{id}/analytics` | Owner view and click totals |
| `GET` | `/public/resumes/{token}` | Public rendered resume, records view |
| `POST` | `/public/resumes/{token}/clicks` | Record recruiter CTA click |

Export jobs are logged and archived to S3-compatible storage when available. Raw visitor addresses are never stored in analytics; only SHA-256 hashes are persisted.

## Billing And Administration

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/billing/checkout?interval=month|year` | Create Stripe hosted subscription checkout |
| `GET` | `/billing/subscription` | Current plan and period |
| `POST` | `/webhooks/stripe` | Signed Stripe event ingestion |
| `GET` | `/admin/metrics` | Platform aggregate metrics (`ADMIN`) |
| `GET` | `/admin/users` | Paginated user administration (`ADMIN`) |
| `PATCH` | `/admin/users/{id}/role` | Role management (`ADMIN`) |
| `GET` | `/admin/audit-logs` | Audit history (`ADMIN`) |

Webhook handling verifies the `Stripe-Signature` timestamp and HMAC, rejects stale requests, and deduplicates Stripe event IDs before changing entitlements.

## Realtime Events

Connect with STOMP/SockJS at `/ws`; resume saves publish on `/topic/resumes/{resumeId}`. Production collaboration should authorize subscriptions and add optimistic revision checks or a CRDT before accepting multi-editor writes.

## Error Shape

```json
{
  "timestamp": "2026-05-27T08:00:00Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed.",
  "fieldErrors": { "password": "size must be between 12 and 128" }
}
```

A starter Postman collection is supplied at `docs/ATSForge.postman_collection.json`; use environment variables `baseUrl`, `accessToken` and `resumeId`.

