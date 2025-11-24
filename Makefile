# Sistema Distribuido de Detección - CC4P1

.PHONY: all up down logs clean help
.PHONY: compile-cliente run-cliente compile-testing run-testing
.PHONY: install-node train-modelo test-train

JAVA_SRC_CLIENTE = cliente-vigilante/src/com/proyecto/vigilante
JAVA_BIN_CLIENTE = cliente-vigilante/bin
JAVA_SRC_TESTING = servidor-testeo
NODE_DIR = servidor-entrenamiento
MODELO_DIR = modelo-ia

# Configuración
SERVER_HOST = localhost
LOG_PORT = 9001
IMAGE_PORT = 9002
TRAIN_PORT = 9000

all: ## Desplegar todo el sistema con Docker
	docker-compose up --build --remove-orphans

up: ## Levantar servicios Docker
	docker-compose up -d --remove-orphans

down: ## Detener servicios Docker
	docker-compose down

logs: ## Ver logs de Docker
	docker-compose logs -f

clean: ## Limpiar todo
	docker-compose down -v --remove-orphans
	@if exist "$(JAVA_BIN_CLIENTE)" rmdir /s /q "$(JAVA_BIN_CLIENTE)"
	@if exist "$(JAVA_SRC_TESTING)\*.class" del /q "$(JAVA_SRC_TESTING)\*.class"
	@if exist "$(JAVA_SRC_TESTING)\temp_frames" rmdir /s /q "$(JAVA_SRC_TESTING)\temp_frames"
	@if exist "$(JAVA_SRC_TESTING)\detection_images" rmdir /s /q "$(JAVA_SRC_TESTING)\detection_images"

# ==============================================================================
# Cliente Vigilante (Java)
# ==============================================================================

compile-cliente: ## Compilar cliente vigilante
	@echo Compilando Cliente Vigilante...
	@if not exist "$(JAVA_BIN_CLIENTE)" mkdir "$(JAVA_BIN_CLIENTE)"
	javac -d $(JAVA_BIN_CLIENTE) $(JAVA_SRC_CLIENTE)/*.java
	@echo Cliente compilado exitosamente

run-cliente: compile-cliente ## Ejecutar cliente vigilante
	@echo Ejecutando Cliente Vigilante...
	@echo Conectando a $(SERVER_HOST):$(LOG_PORT) y $(SERVER_HOST):$(IMAGE_PORT)
	cd $(JAVA_BIN_CLIENTE)/.. && java -cp bin com.proyecto.vigilante.VigilanteApp $(SERVER_HOST) $(LOG_PORT) $(IMAGE_PORT)

# ==============================================================================
# Servidor de Testeo (Java)
# ==============================================================================

compile-testing: ## Compilar servidor de testeo
	@echo Compilando Servidor de Testeo...
	cd $(JAVA_SRC_TESTING) && javac -cp .;* *.java
	@echo Servidor compilado exitosamente

run-testing: compile-testing ## Ejecutar servidor de testeo
	@echo Ejecutando Servidor de Testeo...
	@if not exist "$(JAVA_SRC_TESTING)\temp_frames" mkdir "$(JAVA_SRC_TESTING)\temp_frames"
	@if not exist "$(JAVA_SRC_TESTING)\detection_images" mkdir "$(JAVA_SRC_TESTING)\detection_images"
	cd $(JAVA_SRC_TESTING) && java -Djava.library.path=. TestingServer ../modelo-ia/src/detect.py ./temp_frames ./detection_images $(LOG_PORT) $(IMAGE_PORT) 30

# ==============================================================================
# Servidor de Entrenamiento (Node.js)
# ==============================================================================

install-node: ## Instalar dependencias de Node.js
	@echo Instalando dependencias de Node.js...
	cd $(NODE_DIR) && npm install
	@echo Dependencias instaladas

run-node: ## Ejecutar servidor de entrenamiento
	@echo Ejecutando Servidor de Entrenamiento...
	cd $(NODE_DIR) && node src/server.js

test-train: ## Ejecutar cliente de prueba de entrenamiento
	@echo Ejecutando cliente de prueba...
	cd $(NODE_DIR) && node test/cliente-train.js

# ==============================================================================
# Modelo de IA (Python)
# ==============================================================================

install-python: ## Instalar dependencias de Python
	@echo Instalando dependencias de Python...
	pip install -r $(MODELO_DIR)/requirements.txt
	@echo Dependencias instaladas

train-modelo: ## Entrenar modelo de IA
	@echo Entrenando modelo...
	cd $(MODELO_DIR)/src && python train.py

test-detect: ## Probar detección con imagen de prueba
	@echo Probando detección...
	cd $(MODELO_DIR)/src && python detect.py ../dataset/images/val/img_1.jpg

# ==============================================================================
# Ayuda
# ==============================================================================

help: ## Mostrar esta ayuda
	@echo Sistema Distribuido de Deteccion - CC4P1
	@echo ========================================
	@echo.
	@echo Targets disponibles:
	@echo.
	@findstr /R /C:"^[a-zA-Z_-]*:.*##" $(MAKEFILE_LIST) | findstr /V findstr
