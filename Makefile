# Makefile para gestionar la infraestructura Local y de Producción

# Variables Entorno Local
COMPOSE_LOCAL = docker-compose.yml
API_REPLICAS_LOCAL = 3

# Variables Entorno Producción (Swarm)
COMPOSE_SWARM = docker-compose.swarm.yml
STACK_NAME = validador_stack

.PHONY: help build-local up-local down-local logs-local scale-local swarm-init deploy-prod rm-prod logs-prod

help:
	@echo "================ ENTORNO LOCAL (Desarrollo) ================"
	@echo "  make build-local    - Construye la imagen de la API"
	@echo "  make up-local       - Levanta todo en local (con $(API_REPLICAS_LOCAL) réplicas)"
	@echo "  make down-local     - Detiene los contenedores locales"
	@echo "  make logs-local     - Muestra los logs en tiempo real"
	@echo "  make scale-local N=5 - Escala la API local a N réplicas"
	@echo ""
	@echo "================ ENTORNO PRODUCCIÓN (Swarm) ================"
	@echo "  make push-prod      - (1) Construye y sube la imagen a Docker Hub"
	@echo "  make swarm-init     - (2) Inicializa esta máquina como Manager del clúster"
	@echo "  make deploy-prod    - (3) Despliega la granja en el clúster"
	@echo "  make rm-prod        - Elimina el despliegue del clúster"
	@echo "  make logs-prod      - Muestra los logs de la API en el clúster"

# --- COMANDOS LOCALES ---
build-local:
	docker compose -f $(COMPOSE_LOCAL) build

up-local:
	docker compose -f $(COMPOSE_LOCAL) up -d --scale api=$(API_REPLICAS_LOCAL)

down-local:
	docker compose -f $(COMPOSE_LOCAL) down --volumes --remove-orphans

logs-local:
	docker compose -f $(COMPOSE_LOCAL) logs -f

scale-local:
	@if [ -z "$(N)" ]; then echo "Uso: make scale-local N=numero"; exit 1; fi
	docker compose -f $(COMPOSE_LOCAL) up -d --scale api=$(N)

# --- COMANDOS PRODUCCIÓN (SWARM) ---
push-prod:
	@echo "Construyendo y subiendo imagen a Docker Hub..."
	docker build -t tu_usuario_dockerhub/validador-api:v1.0 ./app
	docker push tu_usuario_dockerhub/validador-api:v1.0

swarm-init:
	docker swarm init || echo "El nodo ya es parte de un clúster Swarm."

deploy-prod:
	docker stack deploy -c $(COMPOSE_SWARM) $(STACK_NAME)

rm-prod:
	docker stack rm $(STACK_NAME)

logs-prod:
	docker service logs -f $(STACK_NAME)_api
