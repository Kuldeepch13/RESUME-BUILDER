# Delivery Roadmap

## Implemented Foundation

- Java 21 / Spring Boot 3.5 API, React/TypeScript responsive product UI, PostgreSQL Flyway schema and Docker Compose environment.
- Verified email/password authentication, Google OAuth exchange, JWT/rotating refresh token flow, RBAC, Redis throttling and account settings.
- Resume creation, ordered/custom sections, themes/templates, autosave broadcast, duplication and immutable versions.
- ATS/job-match async analysis, structured OpenAI adapter, generation endpoint, usage tracking and local-development fallback.
- PDF, DOCX, TXT, HTML and JSON exports, S3-compatible archival, free export limits, share links, QR codes and view/click analytics.
- Stripe hosted checkout endpoint, secure idempotent webhook entitlement processing, admin metrics/user roles/audit views.
- Swagger/OpenAPI, health endpoints, nginx security headers, CI build pipeline and focused unit tests.

## Production Rollout

1. **Hardening and closed beta:** provision managed PostgreSQL/Redis/S3, configure TLS/secrets/mail/OAuth/Stripe, add integration tests with Testcontainers, add logout/revocation, webhook fixtures and backup/restore tests.
2. **Paid launch:** implement Stripe customer portal and invoice/history UI, S3 signed downloads, export worker queue, OpenTelemetry dashboards, plan-specific policy service, refund and chargeback playbooks.
3. **Collaboration:** persist comments, add invitation/permissions model, authenticate WebSocket subscriptions, implement optimistic locking or CRDT editing and moderation workflows.
4. **Career suite:** consented DOCX/PDF parsing/import, LinkedIn-compliant data import flow, cover-letter/portfolio/interview-prep modules, localization and accessibility audits.
5. **Scale:** extract AI/render workers, vector similarity store for job matching, analytics aggregation pipeline, template marketplace review/payout workflow and enterprise SSO/SCIM.

## Key Edge Cases To Cover

- Simultaneous refresh-token reuse, password reset followed by active-session revocation and OAuth email collision.
- Autosave racing a version snapshot, duplicate resume during template removal and malformed custom section JSON.
- Very long resumes, font substitution, page breaks and untrusted rich HTML in rendering.
- AI timeout, refusal, invalid structured output, prompt injection inside a job description and deletion of AI retained data.
- Replayed/out-of-order Stripe events, payment succeeded after user deletion and downgrade during active export.
- Expired share URL, crawler view inflation, analytics privacy consent and deleted resume artifacts.

## Common Mistakes

- Never let browser-submitted plan or role fields grant premium access; only verified billing events should do that.
- Never store raw refresh/reset/share-protection tokens, card details, unredacted AI prompts in logs or public bucket objects.
- Do not render user HTML directly into PDF or public pages without sanitization.
- Do not make provider calls synchronously in autosave or editor requests.
- Do not treat ATS scoring as hiring truth; present it as assistive guidance and evaluate prompts/models against curated examples.
- Do not add microservices before operational evidence demands them; job boundaries and clean modules preserve the option.

