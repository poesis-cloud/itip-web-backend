# Copilot Instructions — itip-web-backend

These instructions are mandatory for all Copilot work in this repository. They apply to the
`itip-web-backend` Spring Boot BFF only. Keep this file concise and update it whenever the
project conventions change.

## Project Anchor

- Always read `PROJECT_BRIEF.md` before non-trivial work and keep it aligned when project state,
  runtime assumptions, deployment flow, or sprint status change.
- Treat `itip-web-backend` as the REST Backend-for-Frontend for ITIP. It translates IT-domain needs
  into calls toward SIE services such as `sie-definition-manager` and
  `sie-definition-blackboard-manager`.
- Preserve the current stack unless explicitly asked otherwise: Java 25, Spring Boot 3.5.x, Maven,
  PostgreSQL, Liquibase, Spring Security OAuth2 Resource Server, Docker, Kubernetes, and Helm.

## Development Rules

- Prefer small, coherent changes. Do not opportunistically refactor unrelated code.
- Fix root causes before adding suppressions. Suppression annotations are last resort only, must be
  narrowly scoped, and must include a short reason.
- Use `git mv` and `git rm` for tracked file moves/deletes. Never use plain `mv` or `rm` on tracked
  files.
- Never commit secrets, credentials, tokens, private keys, or production passwords.
- Keep `.env` limited to checked-in non-secret defaults. Keep `.env.dev` local and uncommitted.
- Use `.yaml` for YAML files. Do not add new tracked `.yml` files.
- Do not add Docker Compose. Local and deployment flows are Helm/Makefile based.

## Java and Spring Boot

- Java version is 25. Keep `pom.xml`, Docker images, CI/CD setup, and documentation consistent with
  Java 25.
- Prefer Spring Boot and Spring Framework annotations when they express the intent clearly and reduce
  custom glue code without hiding important behavior.
- Prefer official libraries and first-party framework integrations over bespoke implementations. Do
  not write custom code when a maintained Spring Boot, Spring Security, Liquibase, Jackson, Jakarta,
  Maven, or other official project library already solves the problem cleanly.
- Every `src/main/java` implementation class must have corresponding tests under `src/test/java`.
- Maintain JaCoCo instruction coverage at 95% or higher at module level.
- Follow Repository-Service exclusivity: each Repository is consumed by exactly one owning Service;
  cross-type data access goes through the owning Service, not a foreign Repository.
- Prefer annotation-driven boilerplate reduction when helpful, but avoid broad Lombok annotations
  such as `@Data` on JPA entities.
- Keep JPA entities explicit: protected no-args constructor, targeted getters/setters, and no
  generated IDs in required-args constructors.
- Handle expected exceptions at the correct boundary, preserve root causes, and use structured logs
  at appropriate levels. Never log secrets or sensitive payloads.

## Security

- Default security posture is authenticated JWT bearer access through Spring Security OAuth2 Resource
  Server.
- Only explicitly public endpoints may bypass authentication, for example health/info actuator
  endpoints.
- Permissive `permitAll` behavior is allowed only for local development and must be profile/property
  gated.
- Tenant isolation is mandatory. Cache keys, request-scoped data, and downstream queries must include
  tenant context when tenant-specific behavior is involved.
- JWT secrets, database passwords, and production OIDC values must come from environment variables or
  Kubernetes Secrets, never from packaged resources.

## Liquibase and Database

- Runtime migrations must be curated under `src/main/resources/db/changelog/changesets/curated/`.
- Except for a documented emergency repair, never hand-author Liquibase migrations from scratch.
  Generate migrations through the configured Liquibase commands, review the generated output, then
  promote the reviewed changeset into the curated runtime migrations directory.
- Generated Liquibase diffs belong under `target/generated-liquibase/` and must not be included by
  the runtime master changelog until reviewed and promoted.
- Keep `liquibase.properties` at repository root so CLI/plugin configuration is not packaged into
  the application artifact.
- Do not hard-code database credentials in `src/main/resources`.
- For every newly created persisted entity/table with a UUID primary key, add a curated Liquibase
  migration in the same change that enforces DB-managed UUIDv7.
- Mandatory DB enforcement for new UUID PK tables:
  - set `id` default to `uuid_v7()`
  - attach assignment trigger via helper (`tgf_assign_id` path)
  - attach UUIDv7 guard trigger via helper (`tgf_reject_non_uuid_v7` path)
- Never rely only on application-side UUID generation for new entities; database enforcement is the
  source of truth.
- Minimum validation for such changes:
  - Liquibase update succeeds
  - DB proves auto-generation of UUIDv7 when `id` is omitted
  - DB rejects explicit non-v7 UUID values
  - full app verification succeeds (`mvn -B verify`)

## Ops and Configuration

- Kubernetes namespace for this service is `itip` by default. Do not confuse it with the OIDC realm
  name, which may still be `sie`.
- Use the Makefile contract: `dev-check`, `dev-up`, `dev-down`, `prod-deploy`, and `package-helm`.
- Helm values live only under:
  - `ops/helm/environments/dev/values.yaml`
  - `ops/helm/environments/preprod/values.yaml`
  - `ops/helm/environments/prod/values.yaml`
- Do not add `ops/helm/values.yaml`.
- Inject Helm secrets through values overrides, `--set-string`, or cluster secret management. Do not
  commit real secret values.

## Validation Protocol

- For Java changes, run `mvn test jacoco:report` when feasible. For full PR readiness, run
  `mvn verify`.
- Never pipe Maven output through `cat`, `tee`, `grep`, or similar tools. Run Maven directly.
- For Helm or ops changes, run `make dev-check` and `helm lint ops/helm -f <environment values>` when
  Helm is available.
- For YAML-only changes, validate syntax and run the most specific project check available.
- If a required tool is missing locally, state the exact missing command and the validation that could
  not be run.

## PR and Review Discipline

- Treat review comments as requirements unless they conflict with an explicit owner decision.
- If a review comment conflicts with current project intent, preserve the owner decision and update
  code/config/docs so the intent is explicit and internally consistent.
- Before marking a PR review item resolved, verify there is no stale contradictory reference in
  `PROJECT_BRIEF.md`, `README.md`, CI/CD, Docker, Helm, Makefile, or runtime configuration.