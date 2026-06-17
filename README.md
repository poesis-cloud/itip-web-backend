# itip-web-backend

Spring Boot backend service for the ITIP Web Frontend (BFF layer). Exposes REST APIs consumed by
`itip-web-frontend`, delegates to SIE Definition Manager for GSM-governed data.

## Stack

- Java 21 / Spring Boot 3.x
- Spring Web, Spring Security (OAuth2 Resource Server), Spring Actuator
- Kubernetes / Helm deployment

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
