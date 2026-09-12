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

- Java 25 / Spring Boot 3.5
- Spring Web, Spring Security (custom JWT filter), Spring Actuator
- PostgreSQL 16, schema owned by Liquibase
- Kubernetes / Helm deployment

## Local development

The backend needs its database first. `itip-web-database` is a sibling component
under `itip/`, deployed separately and reachable in-cluster as `itipdatabase`.

```bash
# 1. Database — from itip/itip-web-database
make dev-up

# 2. Backend — from itip/itip-web-backend
make dev-check
make dev-up

# Run the API locally instead of in-cluster — from itip/itip-web-backend
make run-api

# 3. Teardown, in reverse order
make dev-down   # from itip/itip-web-backend
make dev-down   # from itip/itip-web-database
```

The database port-forward listens on `localhost:5433`, because `5432` is usually
taken by the SIE database.

### Dev accounts

The dev environment seeds two accounts (`admin@itip.local`, `reviewer@itip.local`)
holding different role capability grants, but they are created **disabled** with an
unusable password hash — no working credentials are committed. To use them locally,
set your own password and enable the account against the port-forwarded dev database:

```bash
NEW_PASSWORD='choose-a-local-password'
HASH=$(htpasswd -bnBC 10 "" "$NEW_PASSWORD" | tr -d ':\n')
PGPASSWORD=dev-password psql -h localhost -p 5433 -U itip_local -d itip_web_backend \
  -c "UPDATE account SET password_hash = '$HASH', enabled = true WHERE email = 'admin@itip.local';"
```

Repeat for `reviewer@itip.local` as needed. This keeps working credentials out of
source control while still exercising the seeded role/capability grants.

## Ops

See [ops/README.md](ops/README.md) for Helm install commands, secrets policy, and environment values.

## Build & test

```bash
mvn verify
```
