# itip-web-backend deployables

This folder contains ops/runtime assets for the itip-web-backend service.

- `ops/helm/`: Helm chart for Kubernetes deployments (dev/preprod/prod)

## Helm

Chart path: `ops/helm`

Install with defaults:

```bash
helm upgrade --install itip-web-backend \
  itip-web-backend/ops/helm \
  -n sie --create-namespace
```

Environment values:

- `environments/dev/values.yaml`
- `environments/preprod/values.yaml`
- `environments/prod/values.yaml`

Each environment file is self-contained and carries the chart defaults for that target environment.

Recommended deploy command:

```bash
helm upgrade --install itip-web-backend \
  itip-web-backend/ops/helm \
  -n sie --create-namespace \
  -f itip-web-backend/ops/helm/environments/preprod/values.yaml
```

Secrets policy:

- Never commit production secrets in values files.
- Inject secrets at deploy time (`--set-string`) or from a cluster secret manager.

Schema validation:

- Validate before deploy:

```bash
helm lint itip-web-backend/ops/helm \
  -f itip-web-backend/ops/helm/environments/preprod/values.yaml
```

## Dev deployment

Use `make dev-up` from `itip-web-backend` to deploy the service to your local cluster.

```bash
cd itip-web-backend && make dev-check
cd itip-web-backend && make dev-up
```

Run locally:

```bash
cd itip-web-backend && make run-api
```

Stop:

```bash
cd itip-web-backend && make dev-down
```
