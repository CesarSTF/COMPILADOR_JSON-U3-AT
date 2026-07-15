# Makefile para gestionar los 3 microservicios del Compilador

COMPOSE_FILE = docker-compose.yml

.PHONY: help build up down logs

help:
	@echo "================ COMANDOS DE INFRAESTRUCTURA ================"
	@echo "  make build    - Construye las imagenes de los 3 servicios"
	@echo "  make up       - Levanta los contenedores"
	@echo "  make down     - Detiene los contenedores"
	@echo "  make logs     - Muestra los logs en tiempo real"

build:
	docker compose -f $(COMPOSE_FILE) build

up:
	docker compose -f $(COMPOSE_FILE) up -d

down:
	docker compose -f $(COMPOSE_FILE) down --volumes --remove-orphans

logs:
	docker compose -f $(COMPOSE_FILE) logs -f
