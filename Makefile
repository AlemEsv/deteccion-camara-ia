# Sistema Distribuido de Detección

.PHONY: all up down logs clean cliente

JAVA_SRC = cliente-vigilante/src/com/proyecto/vigilante
JAVA_BIN = cliente-vigilante/bin

all: ## Desplegar todo el sistema
	docker-compose up --build --remove-orphans

up: ## Levantar servicios
	docker-compose up -d --remove-orphans

down: ## Detener servicios
	docker-compose down

logs: ## Ver logs
	docker-compose logs -f

clean: ## Limpiar todo
	docker-compose down -v --remove-orphans

cliente: ## Compilar y ejecutar cliente vigilante
	@if not exist "$(JAVA_BIN)" mkdir "$(JAVA_BIN)"
	javac -d $(JAVA_BIN) $(JAVA_SRC)/*.java
	java -cp $(JAVA_BIN) com.proyecto.vigilante.VigilanteApp
