# Servidor de Entrenamiento (Node.js)

Servidor TCP que maneja la recepción de imágenes para entrenamiento y la ejecución del proceso de entrenamiento del modelo YOLOv8.

## Características

- **Servidor TCP** en puerto 9000
- **Protocolo personalizado** para subir imágenes (`UPLOAD`)
- **Ejecución de entrenamiento** mediante proceso hijo (`START_TRAIN`)
- **Integración con módulo Python** existente

## Estructura

```
servidor-entrenamiento-nodejs/
├── src/
│   ├── server.js              # Servidor principal
│   ├── protocol/
│   │   └── parser.js          # Parser del protocolo
│   └── handlers/
│       ├── uploadHandler.js   # Maneja UPLOAD
│       └── trainHandler.js    # Maneja START_TRAIN
├── test/
|   ├── cliente-train.js       # Cliente de prueba
│   ├── img_13                 # imagen 13 de prueba
|   └── img_14                 # imagen 14 de prueba
├── config.js                  # Configuración
└── package.json
```

## Instalación

No requiere dependencias externas (solo módulos nativos de Node.js).

```bash
# Asegúrate de tener Node.js instalado (>=14.0.0)
node --version
```

## Uso

### Iniciar el servidor

```bash
npm start
# o
node src/server.js
```

El servidor escuchará en el puerto 9000.

### Cliente de Prueba

#### Subir una imagen

```bash
node test/cliente-train.js upload ../servidor-entrenamiento/dataset/images/train/img_1.jpg Perro
```

#### Iniciar entrenamiento

```bash
node test/cliente-train.js train
```

## Protocolo

### Comando UPLOAD

**Formato:**
```
UPLOAD:<label>:<filename>:<filesize>\n[...DATA...]
```

**Ejemplo:**
```
UPLOAD:Perro:dog1.jpg:50000\n[50000 bytes de imagen]
```

**Respuesta:**
```
UPLOAD_SUCCESS:dog1.jpg\n
```

### Comando START_TRAIN

**Formato:**
```
START_TRAIN\n
```

**Respuesta:**
```
TRAINING_STARTED\n
... (salida del entrenamiento) ...
TRAINING_COMPLETE\n
```

## Configuración

Edita `config.js` para cambiar:
- Puerto del servidor
- Rutas del dataset
- Parámetros de entrenamiento por defecto

## Integración

Este servidor se integra con:
- **Módulo Python** (`../servidor-entrenamiento/`): Ejecuta `train.py` como proceso hijo
- **Cliente Admin**: Se conecta al puerto 9000 para subir imágenes e iniciar entrenamiento

