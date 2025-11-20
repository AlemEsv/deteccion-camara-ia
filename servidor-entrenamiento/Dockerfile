FROM node:18-alpine

WORKDIR /app

# Copiar archivos de dependencias
COPY package*.json ./

# Instalar dependencias
RUN npm ci --only=production

# Copiar código fuente
COPY src/ ./src/
COPY config.js ./

# Exponer puerto
EXPOSE 9000

# Comando de inicio
CMD ["node", "src/server.js"]
