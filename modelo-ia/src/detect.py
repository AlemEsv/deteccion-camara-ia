import sys
import os
from pathlib import Path
from ultralytics import YOLO

def detect_objects(image_path, model_path=None, conf_threshold=0.25):
    # Ruta del modelo
    if model_path is None:
        model_path = '/app/models/best.pt'
    
    # Validar que existe la imagen
    if not os.path.exists(image_path):
        sys.exit(1)
    
    # Validar que existe el modelo
    if not os.path.exists(model_path):
        sys.exit(1)
    
    # Cargar modelo
    try:
        model = YOLO(model_path)
    except Exception:
        sys.exit(1)
    
    # Realizar detección
    try:
        results = model(image_path, conf=conf_threshold, verbose=False)
    except Exception:
        sys.exit(1)
    
    detections = []
    
    # Procesar resultados
    for result in results:
        boxes = result.boxes
        
        if boxes is None or len(boxes) == 0:
            continue
        
        for box in boxes:
            # Obtener datos de la caja
            cls_id = int(box.cls[0])
            conf = float(box.conf[0])
            xyxy = box.xyxy[0].tolist()  # [x1, y1, x2, y2]
            
            # Calcular centro y dimensiones
            x1, y1, x2, y2 = xyxy
            x_center = int((x1 + x2) / 2)
            y_center = int((y1 + y2) / 2)
            width = int(x2 - x1)
            height = int(y2 - y1)
            
            # Obtener nombre de clase
            class_name = model.names[cls_id]
            
            detections.append({
                'class_id': cls_id,
                'class_name': class_name,
                'confidence': conf,
                'x': x_center,
                'y': y_center,
                'w': width,
                'h': height
            })
    
    return detections

def print_detections(detections):
    if not detections:
        print("NO_DETECTIONS")
        return
    
    for det in detections:
        # Formato: CLASE,CONFIANZA,X,Y,W,H
        print(f"{det['class_name']},{det['confidence']:.2f},{det['x']},{det['y']},{det['w']},{det['h']}")

def main():
    if len(sys.argv) < 2:
        print("Uso: python detect.py <imagen>", file=sys.stderr)
        sys.exit(1)
    
    image_path = sys.argv[1]
    detections = detect_objects(image_path, conf_threshold=0.01)
    print_detections(detections)

if __name__ == "__main__":
    main()
