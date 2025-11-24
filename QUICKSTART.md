# Guía de Inicio Rápido

## Instalar Dependencias

### Python (Modelo IA)

```powershell
cd modelo-ia
pip install -r requirements.txt
cd ..
```

#### Node.js (Servidor Entrenamiento)

```powershell
cd servidor-entrenamiento
npm install
cd ..
```

## Configurar Cámaras

Edita `servidor-testeo/cameras_config.txt` y agrega al menos una cámara de prueba:

```txt
# Descomenta esta línea para usar un stream de prueba
TEST1,rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4
```

O si tienes cámaras reales:

```txt
CAM1,rtsp://192.168.1.100:554/stream
```

## Ejecutar los Componentes

```powershell
# Terminal 1
make run-testing

# Terminal 2
make run-node

# Terminal 3
make run-cliente
```
