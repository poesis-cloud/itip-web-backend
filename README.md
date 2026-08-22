# itip-web-backend

[![CI](https://github.com/poesis-cloud/itip-web-backend/actions/workflows/ci.yaml/badge.svg)](https://github.com/poesis-cloud/itip-web-backend/actions/workflows/ci.yaml)
[![Release](https://img.shields.io/github/v/release/poesis-cloud/itip-web-backend)](https://github.com/poesis-cloud/itip-web-backend/releases/latest)
[![Coverage gate](https://img.shields.io/badge/JaCoCo-%E2%89%A595%25%20instruction%20coverage%20enforced-brightgreen)](pom.xml)
[![Java](https://img.shields.io/badge/Java-21-orange)](pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)](pom.xml)
[![License: BUSL-1.1](https://img.shields.io/badge/license-BUSL--1.1-blue)](LICENSE)

Spring Boot backend service for the ITIP Web Frontend (BFF layer). Exposes REST APIs consumed by
`itip-web-frontend`, delegates to SIE Definition Manager for GSM-governed data.

## Stack

- Java 25 / Spring Boot 3.x
- Spring Web, Spring Security (custom JWT filter), Spring Actuator
- Kubernetes / Helm deployment

## Local Docker run

Use the repository `Dockerfile` to build and run the API container:

```bash
docker build -t itip-web-backend:local .
docker run --rm -p 8080:8080 \
	-e DB_URL=jdbc:postgresql://<host>:5432/itip_web_backend \
	-e DB_USER=<user> \
	-e DB_PASSWORD=<password> \
	-e ITIP_SECURITY_JWT_SECRET=<at-least-32-bytes-secret> \
	itip-web-backend:local
```

## Liquibase workflow

Database migrations are managed with Liquibase and a Hibernate-backed diff configuration.

```bash
mvn liquibase:diff
mvn liquibase:update
```

The diff configuration is defined in repository-root `liquibase.properties`. Generated diffs are written under `target/generated-liquibase/`; promote reviewed changes into `src/main/resources/db/changelog/changesets/curated/` before runtime use.

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
