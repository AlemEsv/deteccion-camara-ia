# Módulo de IA - YOLOv8

Sistema de detección de objetos usando YOLOv8 para detectar 3 clases:

- Persona, Gato, Perro

## Uso

```bash
# Construir imagen
docker build -t yolo-ia .
# Entrenar modelo
docker run --rm -v ${PWD}:/app yolo-ia python src/train.py 20 8
# Detectar imagen
docker run --rm -v ${PWD}:/app yolo-ia python src/detect.py /app/ruta/a/imagen.jpg
```

## Integración

**Node.js:** `python src/train.py 50`  
**Java:** `python src/detect.py /tmp/frame.jpg`

Salida: `CLASE,CONFIANZA,X,Y,W,H`
