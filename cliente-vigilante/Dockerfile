FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Instalar dependencias para JavaFX y GUI
RUN apt-get update && apt-get install -y \
    libgtk-3-0 \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    x11-apps \
    && rm -rf /var/lib/apt/lists/*

# Copiar código fuente
COPY src/ ./src/

# Compilar aplicación Java
RUN javac -d bin src/com/proyecto/vigilante/*.java

# Variable de entorno para display
ENV DISPLAY=:0

# Comando de inicio
CMD ["java", "-cp", "bin", "com.proyecto.vigilante.VigilanteApp"]
