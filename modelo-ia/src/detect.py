import sys
import os
from pathlib import Path
from ultralytics import YOLO

def detect_objects(image_path, model_path=None, conf_threshold=0.01):
    """
    Detecta objetos en una imagen usando YOLOv8
    
    Args:
        image_path: Ruta a la imagen a analizar
        model_path: Ruta al modelo YOLO (opcional, busca automáticamente)
        conf_threshold: Umbral mínimo de confianza (0.01 = 1%)
    
    Returns:
        Lista de detecciones encontradas
    """
    # Buscar modelo en múltiples ubicaciones
    if model_path is None:
        script_dir = Path(__file__).parent
        possible_paths = [
            script_dir.parent / 'yolov8n.pt',         # YOLO base primero
            script_dir.parent / 'models' / 'best.pt',  # Modelo entrenado
            Path('/app/models/best.pt'),              # Docker
        ]
        
        model_path = None
        for path in possible_paths:
            if path.exists():
                model_path = str(path)
                print(f"INFO: Usando modelo: {model_path}", file=sys.stderr)
                break
        
        if model_path is None:
            print("ERROR: No se encontró ningún modelo", file=sys.stderr)
            sys.exit(1)
    
    # Validar imagen
    if not os.path.exists(image_path):
        print(f"ERROR: Imagen no encontrada: {image_path}", file=sys.stderr)
        sys.exit(1)
    
    # Cargar modelo
    try:
        model = YOLO(model_path)
        print(f"INFO: Modelo cargado exitosamente", file=sys.stderr)
    except Exception as e:
        print(f"ERROR al cargar modelo: {e}", file=sys.stderr)
        sys.exit(1)
    
    # Ejecutar detección
    try:
        results = model(image_path, conf=conf_threshold, verbose=False)
    except Exception as e:
        print(f"ERROR en detección: {e}", file=sys.stderr)
        sys.exit(1)
    
    # Procesar resultados
    detections = []
    for result in results:
        boxes = result.boxes
        
        if boxes is None or len(boxes) == 0:
            continue
        
        for box in boxes:
            cls_id = int(box.cls[0])
            conf = float(box.conf[0])
            xyxy = box.xyxy[0].tolist()
            
            # Calcular centro y dimensiones de la caja
            x1, y1, x2, y2 = xyxy
            x_center = int((x1 + x2) / 2)
            y_center = int((y1 + y2) / 2)
            width = int(x2 - x1)
            height = int(y2 - y1)
            
            detections.append({
                'class_name': model.names[cls_id],
                'confidence': conf,
                'x': x_center,
                'y': y_center,
                'w': width,
                'h': height
            })
    
    return detections

def print_detections(detections):
    """Imprime detecciones en formato CSV: CLASE,CONFIANZA,X,Y,W,H"""
    if not detections:
        print("NO_DETECTIONS")
        return
    
    for det in detections:
        print(f"{det['class_name']},{det['confidence']:.2f},{det['x']},{det['y']},{det['w']},{det['h']}")

def main():
    if len(sys.argv) < 2:
        print("Uso: python detect.py <imagen>", file=sys.stderr)
        sys.exit(1)
    
    image_path = sys.argv[1]
    detections = detect_objects(image_path)
    print_detections(detections)

if __name__ == "__main__":
    main()
