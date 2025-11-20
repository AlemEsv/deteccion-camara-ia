import os
import sys
from pathlib import Path
from ultralytics import YOLO
import yaml
import shutil

def train_yolo(epochs=50, imgsz=640, batch=16, device='cpu'):
    # Rutas
    script_dir = Path(__file__).parent.parent
    dataset_yaml = script_dir / 'dataset' / 'data.yaml'
    model_pretrained = script_dir / 'yolov8n.pt'
    models_dir = script_dir / 'models'
    
    # Validar que exista el dataset
    if not dataset_yaml.exists():
        sys.exit(1)
    
    # Validar que exista el modelo pre-entrenado
    if not model_pretrained.exists():
        sys.exit(1)
    
    # Cargar configuración del dataset
    with open(dataset_yaml, 'r', encoding='utf-8') as f:
        data_config = yaml.safe_load(f)
    
    # Cargar modelo pre-entrenado
    model = YOLO(str(model_pretrained))
    
    # Entrenar el modelo
    try:
        results = model.train(
            data=str(dataset_yaml),
            epochs=epochs,
            imgsz=imgsz,
            batch=batch,
            device=device,
            project=str(models_dir),
            name='train',
            exist_ok=True,
            pretrained=True,  # Usar pesos pre-entrenados (transfer learning)
            verbose=True
        )
        
        # Ruta del mejor modelo
        best_model = models_dir / 'train' / 'weights' / 'best.pt'
        
        if best_model.exists():
            # Exportar a ONNX
            model_export = YOLO(str(best_model))
            onnx_path = model_export.export(format='onnx', simplify=True)
            
            # Copiar modelo
            best_pt_copy = models_dir / 'best.pt'
            best_onnx_copy = models_dir / 'best.onnx'
            
            shutil.copy(best_model, best_pt_copy)
            if Path(onnx_path).exists():
                shutil.copy(onnx_path, best_onnx_copy)
        
        # captura la finalización (para Node.js)
        print("TRAINING_COMPLETE")
        
    except Exception as e:
        sys.exit(1)

def main():
    epochs = int(sys.argv[1]) if len(sys.argv) > 1 else 50
    batch = int(sys.argv[2]) if len(sys.argv) > 2 else 16
    train_yolo(epochs=epochs, batch=batch)

if __name__ == "__main__":
    main()
