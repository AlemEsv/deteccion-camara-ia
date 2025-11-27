# Guía de Inicio Rápido

## Requisitos Previos

- **Java JDK 11+**
- **Python 3.8+**
- **Node.js 14+**
- **OpenCV 4.12.0**

## Instalar Dependencias

### Python (Modelo IA)

```powershell
cd modelo-ia
pip install -r requirements.txt
cd ..
```

### Node.js (Servidor Entrenamiento)

```powershell
cd servidor-entrenamiento
npm install
cd ..
```

## Configurar Cámaras

Edita `servidor-testeo/cameras_config.txt`:

```txt
# OPCIÓN RECOMENDADA: Video de prueba local
TEST1,test_video.mp4

# Cámara real
# CAM1,rtsp://192.168.1.100:554/stream
```

## Ejecutar el Sistema

```powershell
# Compilar todos los componentes
make compile-all
```

### Servidor de Entrenamiento

```powershell
# Terminal 1
make run-node
```

### Servidor de Testeo

```powershell
# Terminal 1
make run-testing
# esperar aproximadamente 5/8 minutos hasta detectar la camara
```

### Cliente Vigilante

```powershell
# Terminal 2
make run-cliente
```
