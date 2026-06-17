# Copilot Instructions — itip-web-backend

This is the **REST API backend for the IT Intelligence Platform (ITIP)**.
It is a BFF (Backend-for-Frontend) built with Java 21 / Spring Boot 3.5, deployed on
Kubernetes (AKS) via Helm. Always read `PROJECT_BRIEF.md` before starting work.

---

## Team Agents

### Remy (Producer) — `@ai-team-producer`

Sprint planning, backlog governance, GitHub Issues, PR coordination.
Remy NEVER writes application code.

**Skills:** `product-manager`, `create-pr`, `update-pr`, `sync`, `commit`,
`chronicle`, `breakdown-epic-pm`, `breakdown-feature-prd`

### Nova / Sage / Milo (Dev) — `@ai-team-dev`

REST API implementation, BFF aggregation, appraisal mechanism design,
caching, security, OpenAPI shaping.

**Skills:** `gsm-knowledge`, `itip-appraisal-indicators`, `itip-framework-sourcing`,
`agent-customization`, `update-skills`, `commit`, `create-pr`, `context-map`,
`refactor-plan`, `breakdown-feature-implementation`

### Ivy (QA) — `@ai-team-qa`

JaCoCo coverage enforcement, appraisal measure type correctness, BFF cache
invalidation tests, API contract tests, E2E sign-off.

**Skills:** `code-review`, `commit`

---

## Domain Rules (mandatory for all agents)

### Repository-Service Exclusivity

Each Repository interface MUST be consumed by exactly one Service. Cross-type data
access goes through the owning Service, never through a foreign Repository.
Use `@Lazy` for circular service dependencies.

### Root-Cause-First

Fix root causes before using suppression annotations (`@SuppressWarnings`,
`// noinspection`, etc.). Suppression is last resort; scope it narrowly (single
field/method, never class-level) and add a one-line comment explaining why.

### JaCoCo >=95% Instruction Coverage

Every `src/main` class MUST have corresponding tests in `src/test`. Run:

```bash
mvn test jacoco:report
```

Check `target/site/jacoco/index.html`. Coverage MUST be >=95% at module level
before merging any PR.

### Never Pipe mvn Output

NEVER use `| cat`, `| tee`, `| grep`, `2>&1 | ...` with `mvn` commands.
Always run `mvn` directly so output renders live in the terminal.

### Git History Preservation

Use `git mv` and `git rm` for all tracked file/folder operations.
Never use plain `mv`, `rm`, or OS-level equivalents on tracked files.

### Archive Folders Are Read-Only

`archives/` and `archive/` folders contain superseded content.
Never edit, update, or delete files inside them.

### Commit Trailer (required on every commit)

```
Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
```

---

## Appraisal Indicator Rules

- There are **29 appraisal mechanisms** across **7 bilateral classes**
  (meta-governance zone + governance zone).
- Each mechanism's return type (measure type) is fixed by the
  `itip-appraisal-indicators` skill catalog:
  `percent` | `count` | `days` | `ratio`
- When implementing or reviewing appraisal rules, ALWAYS load the
  `itip-appraisal-indicators` skill and cross-check measure types.
- NA (meta-governance) and NX (governance) dual-zone semantics must be respected.

## BFF Caching Rules

- Cache keys MUST include **tenant ID** + **indicator ID** (or framework ID).
- TTL is environment-configurable: short in dev, longer in preprod/prod.
- Cache invalidation tests are required for every cached endpoint.
- Use `@CacheEvict` on write paths; document TTL in `application.yaml` comments.

## SIE Repository Structure Consistency

- `ops/helm/` with environments: `dev/`, `preprod/`, `prod/` — no root `values.yaml`.
- `Makefile` with `dev-up`, `dev-down`, `dev-check`, `prod-deploy`.
- CI: `.github/workflows/ci.yaml` (Maven build/test, Helm lint).
- CD: `.github/workflows/cd.yaml` (Azure OIDC, Helm upgrade).
- YAML files use `.yaml` extension; do not add `.yml`.

---

## SE Plugin Agents (global — invoke by name)

These agents are installed globally via the `software-engineering-team` plugin. Invoke them by name in any chat.

| When | Invoke |
|---|---|
| Security review before any merge | `SE: Security` |
| Architecture decision or structurant PR | `SE: Architect` |
| CI/CD pipeline, Helm, deployment debug | `SE: DevOps/CI` |
| Writing/updating API docs, ADRs, README | `SE: Technical Writer` |
| Authoring GitHub Issues or backlog items | `SE: Product Manager Advisor` |
