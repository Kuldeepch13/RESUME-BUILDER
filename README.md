# ATSForge Resume Studio

Production-oriented AI resume builder SaaS foundation built with Java 21, Spring Boot, PostgreSQL, Redis, React and TypeScript.

## Quick Start

```bash
docker compose up --build
```

Services:

- Web application: `http://localhost:5173`
- REST API and Swagger: `http://localhost:8080/swagger-ui.html`
- Actuator health: `http://localhost:8080/actuator/health`
- Mailpit inbox: `http://localhost:8025`
- MinIO console: `http://localhost:9001`

Local backend development:

```bash
cd backend
mvn spring-boot:run
```

Local frontend development:

```bash
cd frontend
npm install
npm run dev
```

Use [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for system boundaries and data flows, [docs/API.md](docs/API.md) for the API surface, and [docs/ROADMAP.md](docs/ROADMAP.md) for production rollout guidance.

