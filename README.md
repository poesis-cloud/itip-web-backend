# itip-web-backend

Spring Boot backend service for the ITIP Web Frontend (BFF layer). Exposes REST APIs consumed by
`itip-web-frontend`, delegates to SIE Definition Manager for GSM-governed data.

## Stack

- Java 25 / Spring Boot 3.x
- Spring Web, Spring Security (OAuth2 Resource Server), Spring Actuator
- Kubernetes / Helm deployment

## Liquibase workflow

Database migrations are managed with Liquibase and a Hibernate-backed diff configuration.

```bash
mvn liquibase:diff
mvn liquibase:update
```

The diff configuration is defined in the repository-root `liquibase.properties` so database
credentials are not packaged into the application artifact. Generated diffs are written under
`target/generated-liquibase/`; promote reviewed changes into
`src/main/resources/db/changelog/changesets/curated/` before runtime use.

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
