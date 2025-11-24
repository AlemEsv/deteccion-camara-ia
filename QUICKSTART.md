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
# IMPORTANTE: Los streams RTSP públicos pueden no funcionar por firewall
# Opción 1: Stream de prueba (puede fallar)
# TEST1,rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4

# Opción 2: Video local (RECOMENDADO para pruebas)
TEST1,test_video.mp4

# Opción 3: Cámara real
# CAM1,rtsp://192.168.1.100:554/stream
```

**⚠️ NOTA:** Si usas video local, asegúrate de tener un archivo `test_video.mp4` en `servidor-testeo/`

## Ejecutar el Sistema

```powershell
# Compilar todos los componentes
make compile-all
```

### Servidor de Testeo

```powershell
# Terminal 1
make run-testing
```

### Servidor de Entrenamiento

```powershell
# Terminal 2
make run-node
```

### Cliente Vigilante

```powershell
# Terminal 3
make run-cliente
```
