# PROJECT_BRIEF.md — itip-web-backend

> Last updated: 2025-07 | Sprint 0 | Status: Scaffolded

## 1. Project Overview

`itip-web-backend` is the REST API backend for the IT Intelligence Platform (ITIP).
It acts as a Backend-for-Frontend (BFF) between `itip-web-frontend` and the SIE services
(`sie-definition-manager`, `sie-definition-blackboard-manager`), translating the
Generative System Model (GSM) for the IT governance domain. It exposes appraisal
indicator APIs, governance framework APIs, and GSM definition query endpoints, with
tenant-aware caching.

## 2. Concept / Product Description

ITIP operationalises GSM-governed IT governance for enterprises. The backend:

- Aggregates **29 appraisal indicator mechanisms** across **7 bilateral classes**
  (meta-governance + governance zones) into per-tenant dashboards.
- Serves **governance framework instances** sourced from TOGAF, ISO 25010, ISO 25012,
  SAFe, ITIL, GDPR, NIS2, and DORA.
- Delegates GSM definition queries to `sie-definition-manager` (Ascription lifecycle,
  Archetype resolution).
- Delegates definition sourcing queries to `sie-definition-blackboard-manager`.
- Caches aggregated payloads per tenant+indicator; TTL is configurable per environment.

## 3. Tech Stack

- **Language/Runtime:** Java 25 (virtual threads enabled)
- **Framework:** Spring Boot 3.5.x (Web, HATEOAS, Validation, Security, Actuator)
- **Build:** Maven (`mvn verify` runs all checks including JaCoCo)
- **Auth:** Spring Security OAuth2 Resource Server (JWT bearer)
- **Caching:** Spring Cache — Caffeine (dev), Redis-compatible (preprod/prod)
- **Testing:** JUnit 5, Mockito, Spring Boot Test, JaCoCo (>=95% instruction coverage)
- **Container/Ops:** Docker, Kubernetes (AKS), Helm 3
- **CI/CD:** GitHub Actions (`.github/workflows/ci.yaml`, `cd.yaml`)

## 4. Architecture

```
+-------------------------------------------------------+
|  itip-web-frontend  (React / TypeScript)              |
+------------------------+------------------------------+
                         | HTTPS REST (JWT bearer)
+------------------------v------------------------------+
|  itip-web-backend  (BFF — Spring Boot 3.5 / Java 25) |
|                                                       |
|  AppraisalController  ->  AppraisalBffService         |
|  FrameworkController  ->  FrameworkBffService         |
|  GsmQueryController   ->  GsmQueryBffService          |
|                                                       |
|  [Tenant-aware cache — Caffeine / Redis]              |
+----------+----------------------------+---------------+
           | REST/HTTP                  | REST/HTTP
+----------v-----------+   +-----------v---------------+
| sie-definition-       |   | sie-definition-blackboard |
| manager               |   | -manager                  |
| (GSM / Ascription)    |   | (definition sourcing)     |
+-----------------------+   +---------------------------+
```

## 5. Key Files Map

| Area           | Path                                                             | Contents                                         |
| -------------- | ---------------------------------------------------------------- | ------------------------------------------------ |
| Entry point    | `src/main/java/cloud/poesis/itip/ItipWebBackendApplication.java` | Spring Boot main                                 |
| Source root    | `src/main/java/cloud/poesis/itip/`                               | All application code                             |
| Resources      | `src/main/resources/`                                            | `application.yaml`, logback config               |
| Tests          | `src/test/java/`                                                 | Unit + integration tests (JaCoCo >=95%)          |
| Helm chart     | `ops/helm/`                                                      | `Chart.yaml`, `templates/`, `environments/`      |
| Dev values     | `ops/helm/environments/dev/values.yaml`                          | Dev Helm overrides                               |
| Preprod values | `ops/helm/environments/preprod/values.yaml`                      | Preprod overrides                                |
| Prod values    | `ops/helm/environments/prod/values.yaml`                         | Prod overrides                                   |
| Ops README     | `ops/README.md`                                                  | Helm install, secrets policy                     |
| Makefile       | `Makefile`                                                       | `dev-up`, `dev-down`, `dev-check`, `prod-deploy` |
| CI             | `.github/workflows/ci.yaml`                                      | Maven build/test + Helm lint                     |
| CD             | `.github/workflows/cd.yaml`                                      | Helm deploy to AKS (Azure OIDC)                  |
| Domain defs    | `def/`                                                           | BFF contract definitions, schema drafts          |
| Sprint docs    | `docs/sprint-N/`                                                 | `plan.md`, `progress.md`, `done.md`              |

## 6. Team Roles

| Agent             | Name     | Role                                                                    |
| ----------------- | -------- | ----------------------------------------------------------------------- |
| Producer          | **Remy** | Sprint planning, backlog governance, GitHub Issues, PR coordination     |
| Dev (backend)     | **Sage** | API implementation, BFF aggregation, caching, security                  |
| Dev (domain)      | **Nova** | Appraisal mechanisms, governance framework endpoints, GSM query layer   |
| Dev (infra/style) | **Milo** | HATEOAS response shaping, OpenAPI spec, config management               |
| QA                | **Ivy**  | JaCoCo coverage, appraisal correctness, cache tests, API contract tests |

> Remy NEVER writes application code.
> Sage / Nova / Milo are the dev team (`@ai-team-dev`).
> Ivy is the QA team (`@ai-team-qa`).

## 7. Sprint Status

| Sprint | Name                  | Status      | Scope                                                     |
| ------ | --------------------- | ----------- | --------------------------------------------------------- |
| 0      | Scaffold & Bootstrap  | Done        | Java 25 / Spring Boot 3.5, Helm, CI/CD, team setup        |
| 1      | Core BFF Skeleton     | Not started | Controllers, service stubs, cache config, health endpoint |
| 2      | Appraisal Indicators  | Not started | 29 mechanisms, 7 bilateral classes, measure types         |
| 3      | Governance Frameworks | Not started | TOGAF, ISO 25010/12, SAFe, ITIL, GDPR, NIS2, DORA         |
| 4      | GSM Query Layer       | Not started | Ascription/Archetype proxy, definition sourcing           |
| 5      | Hardening & Coverage  | Not started | JaCoCo >=95%, security review, load testing               |

## 8. Current State (rewrite every sprint)

**What works:**

- Repo scaffolded: `pom.xml` (Java 25, Spring Boot 3.5), Maven build passes.
- Helm chart with dev/preprod/prod environment value files.
- CI (`ci.yaml`) and CD (`cd.yaml`) workflows in place.
- `Makefile` with `dev-up` / `dev-down` / `dev-check` / `prod-deploy` targets.

**What does not work yet:**

- No controllers or service implementations.
- No caching configuration.
- No integration with `sie-definition-manager` or `sie-definition-blackboard-manager`.

**What's next (Sprint 1):**

- BFF skeleton: appraisal, framework, gsm-query controllers + service stubs.
- Tenant-aware cache configuration (Caffeine for dev).
- Health/readiness actuator endpoints.
- JaCoCo baseline >=95%.

## 9. Security Rules

1. Secrets in environment variables / Kubernetes Secrets only — never in code or git.
2. `.env.dev` is machine-local only — MUST NOT be committed (add to `.gitignore`).
3. JWT validation via Spring Security OAuth2 Resource Server — all endpoints require
   a valid bearer token unless explicitly designated public.
4. Tenant isolation: all cache keys and query parameters MUST include the tenant ID.
5. No direct repository-to-repository access — follow Repository-Service exclusivity rule.

## 10. How to Run Locally

```bash
# Prerequisites: Java 25, Maven, kubectl (local cluster), Helm 3
make dev-check          # Verify prerequisites and cluster context
make dev-up             # Deploy Helm chart to local cluster (itip namespace)

# Configure local secrets
cp .env.dev.example .env.dev   # fill in service URLs, JWT issuer
# Run the API
make run-api            # Reads from .env.dev
# Or directly:
mvn spring-boot:run
```

## 11. How to Deploy

```bash
# Preprod
helm upgrade --install itip-web-backend ops/helm \
  -f ops/helm/environments/preprod/values.yaml --namespace itip

# Prod — via CD pipeline only; do not deploy manually
# See .github/workflows/cd.yaml for Azure OIDC + Helm upgrade commands
make prod-deploy        # Triggers prod-style cluster deployment
```

## 12. Cross-Chat Handoff Protocol

Before finishing any sprint chat:

1. Write `docs/sprint-N/done.md` — what was built, what's deferred, files changed,
   any required manual setup steps.
2. Update `PROJECT_BRIEF.md`: Section 7 (sprint status) + Section 8 (current state).
3. Commit all changes with message `sprint-N: <summary>` plus the Co-authored-by trailer.

**Cold-start recovery prompt:**

```
Read PROJECT_BRIEF.md and docs/sprint-N/progress.md.
Continue from where it left off.
```

## 13. Bug & Fix Tracking

GitHub Issues are the single source of truth. Labels: `bug`, `severity:blocker`,
`severity:major`, `severity:minor`. Components: `appraisal-api`, `framework-api`,
`gsm-query`, `bff-cache`, `security`.

**Dev team:** Check issues before starting. Fix blockers before polish.
Close with `Fixes #NN` in commits.

**QA (Ivy):** File bugs as GitHub Issues. No blockers found -> write
`docs/qa/sprint-N-signoff.md` (test count, pass rate, explicit "no blockers" statement).

**Remy:** Triage new issues each sprint. Keep backlog pruned.

## 14. Multi-Repo Setup

Each team works in their own clone. No worktrees.

```bash
git clone git@github.com:poesis-cloud/itip-web-backend.git itip-web-backend-dev
git clone git@github.com:poesis-cloud/itip-web-backend.git itip-web-backend-qa
```

**Branches:**

- `main` — stable; Remy coordinates merges.
- `feature/sprint-N` — dev team.
- `feature/qa-N` — QA.
- `feature/devops-N` — DevOps (on demand).

**Rules:**

- Feature branches -> PR -> regular merge to main.
- Never push directly to main. Never squash. Never rebase feature branches (causes commit loss).
- Always use `git mv` / `git rm` for tracked file moves and deletes.
