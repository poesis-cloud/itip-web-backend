SHELL := /bin/bash
.EXPORT_ALL_VARIABLES:

.PHONY: dev-check dev-up dev-down deploy-check prod-deploy package-helm run-api test verify

-include .env.dev

NAMESPACE        ?= itip
DEPLOY_ENV       ?= preprod
RELEASE          ?= itip-web-backend

CHART            ?= ./ops/helm
ENV_FILE         ?= $(CHART)/environments/$(DEPLOY_ENV)/values.yaml

PORT_FORWARD_PID_FILE ?= .dev-port-forwards.pids

dev-check:
	@command -v kubectl >/dev/null 2>&1 || { echo "Missing required command: kubectl"; exit 1; }
	@command -v helm    >/dev/null 2>&1 || { echo "Missing required command: helm";    exit 1; }
	@kubectl config current-context >/dev/null 2>&1 || { echo "No active Kubernetes context."; exit 1; }
	@kubectl get ns >/dev/null 2>&1 || { echo "Cannot reach Kubernetes API."; exit 1; }
	@test -d "$(CHART)" || { echo "Missing chart directory: $(CHART)"; exit 1; }
	@test -f "$(CHART)/environments/dev/values.yaml" || { echo "Missing dev values file"; exit 1; }
	@echo "dev-check passed"

deploy-check:
	@command -v kubectl >/dev/null 2>&1 || { echo "Missing required command: kubectl"; exit 1; }
	@command -v helm    >/dev/null 2>&1 || { echo "Missing required command: helm";    exit 1; }
	@kubectl config current-context >/dev/null 2>&1 || { echo "No active Kubernetes context."; exit 1; }
	@kubectl get ns >/dev/null 2>&1 || { echo "Cannot reach Kubernetes API."; exit 1; }
	@test -d "$(CHART)" || { echo "Missing chart directory: $(CHART)"; exit 1; }
	@test -f "$(ENV_FILE)" || { echo "Missing environment values file: $(ENV_FILE)"; exit 1; }
	@: "$${IMAGE_REPOSITORY:?Missing IMAGE_REPOSITORY in environment}"
	@: "$${IMAGE_TAG:?Missing IMAGE_TAG in environment}"
	@echo "deploy-check passed"

dev-up:
	kubectl get ns $(NAMESPACE) >/dev/null 2>&1 || kubectl create ns $(NAMESPACE) >/dev/null
	helm upgrade --install $(RELEASE) $(CHART) -n $(NAMESPACE) --create-namespace --wait --timeout 5m0s \
	-f $(CHART)/environments/dev/values.yaml \
	--set persistence.enabled=true
	@echo "itip-web-backend deployed. Run: make run-api"

dev-down:
	@PID=$$(lsof -ti :8080 2>/dev/null); \
	if [[ -n "$$PID" ]]; then kill $$PID 2>/dev/null || true; echo "Stopped process on port 8080 (PID $$PID)"; fi
	helm uninstall $(RELEASE) -n $(NAMESPACE) || true

run-api:
	SERVER_PORT="$(SERVER_PORT)" \
	mvn spring-boot:run

prod-deploy:
	@$(MAKE) deploy-check
	helm upgrade --install $(RELEASE) $(CHART) -n $(NAMESPACE) --create-namespace --wait --timeout 10m0s \
	-f $(ENV_FILE) \
	--set image.repository="$${IMAGE_REPOSITORY}" \
	--set image.tag="$${IMAGE_TAG}"

package-helm:
	@command -v helm >/dev/null 2>&1 || { echo "Missing required command: helm"; exit 1; }
	@test -d "$(CHART)" || { echo "Missing chart directory: $(CHART)"; exit 1; }
	helm package $(CHART)

test:
	mvn test

verify:
	mvn verify
