# itip-web-backend

Spring Boot backend service for the ITIP Web Frontend (BFF layer). Exposes REST APIs consumed by
`itip-web-frontend`, delegates to SIE Definition Manager for GSM-governed data.

## Stack

- Java 25 / Spring Boot 3.x
- Spring Web, Spring Security (custom JWT filter), Spring Actuator
- Kubernetes / Helm deployment

## Local Docker stack

The module now ships with a `docker-compose.yml` at the repository root of `itip-web-backend`.

```bash
docker compose up --build
```

This starts:

- PostgreSQL 17
- the Spring Boot API container

The API reads its database connection from `SPRING_DATASOURCE_*`, runs Liquibase at startup, and is configured to start without an external OIDC server for local work.

## Liquibase workflow

Database migrations are managed with Liquibase and a Hibernate-backed diff configuration.

```bash
mvn liquibase:diff
mvn liquibase:update
```

The diff configuration is defined in `src/main/resources/liquibase.properties` and generates new changelog files under `src/main/resources/db/changelog/changesets/generated/`.

## Local development

```bash
# Check prerequisites (kubectl, helm, active context)
make dev-check

# Deploy the chart to the local cluster
make dev-up

# Run the API locally (reads from .env.dev)
make run-api

# Stop and uninstall the chart
make dev-down
```

## Ops

See [ops/README.md](ops/README.md) for Helm install commands, secrets policy, and environment values.

## Build & test

```bash
mvn verify
```
