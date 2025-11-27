import socket
import json
import requests
import time
import base64
import os

# Configuración
TESTEO_SERVER_HOST = '127.0.0.1'
LOG_PORT = 9001
IMAGE_PORT = 9002
FLASK_SERVER_URL = 'http://127.0.0.1:5000/api/receive'

def fetch_logs_from_testeo():
    """Obtiene logs del servidor de testeo Java."""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(5)
            s.connect((TESTEO_SERVER_HOST, LOG_PORT))
            
            # Enviar comando GET_LOGS
            s.sendall(b'GET_LOGS\n')
            
            # Recibir respuesta JSON
            response = s.recv(65536).decode('utf-8')
            
            if not response:
                return []
            
            # Parsear JSON
            logs = json.loads(response)
            return logs
            
    except Exception as e:
        print(f"Error obteniendo logs: {e}")
        return []

def fetch_image_from_testeo(image_name):
    """Obtiene una imagen del servidor de imágenes."""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(5)
            s.connect((TESTEO_SERVER_HOST, IMAGE_PORT))
            
            # Enviar comando GET_IMAGE
            command = f'GET_IMAGE:{image_name}\n'
            s.sendall(command.encode('utf-8'))
            
            # Leer cabecera FILESIZE
            header = b''
            while True:
                byte = s.recv(1)
                if byte == b'\n':
                    break
                header += byte
            
            header_str = header.decode('utf-8').strip()
            
            if not header_str.startswith('FILESIZE:'):
                print(f"Cabecera inválida: {header_str}")
                return None
            
            # Obtener tamaño del archivo
            file_size = int(header_str.split(':')[1])
            
            # Leer bytes de la imagen
            image_bytes = b''
            while len(image_bytes) < file_size:
                chunk = s.recv(min(4096, file_size - len(image_bytes)))
                if not chunk:
                    break
                image_bytes += chunk
            
            # Convertir a base64
            return base64.b64encode(image_bytes).decode('utf-8')
            
    except Exception as e:
        print(f"Error obteniendo imagen {image_name}: {e}")
        return None

def send_to_flask(detected_object, image_base64, confidence=0.95):
    """Envía un log al servidor Flask."""
    try:
        payload = {
            'object': detected_object,
            'image': image_base64,
            'confidence': confidence
        }
        
        response = requests.post(FLASK_SERVER_URL, json=payload, timeout=5)
        
        if response.status_code == 201:
            print(f"✓ Enviado a Flask: {detected_object}")
            return True
        else:
            print(f"✗ Error en Flask: {response.status_code}")
            return False
            
    except Exception as e:
        print(f"✗ Error enviando a Flask: {e}")
        return False

def main():
    """Bucle principal del puente."""
    print("=" * 60)
    print("PUENTE: Servidor Testeo → Servidor Flask")
    print("=" * 60)
    print(f"Escuchando logs en: {TESTEO_SERVER_HOST}:{LOG_PORT}")
    print(f"Servidor de imágenes: {TESTEO_SERVER_HOST}:{IMAGE_PORT}")
    print(f"Enviando a Flask: {FLASK_SERVER_URL}")
    print("=" * 60)
    
    processed_images = set()  # Para evitar enviar duplicados
    
    while True:
        try:
            # Obtener logs del servidor de testeo
            logs = fetch_logs_from_testeo()
            
            if logs:
                print(f"\n[INFO] Recibidos {len(logs)} logs del servidor de testeo")
                
                for log in logs:
                    image_name = log.get('imagen', '')
                    detected_object = log.get('objeto', 'Desconocido')
                    confidence = log.get('confidence', 0.0)
                    
                    # Solo subir si confianza > 0.6
                    if confidence < 0.6:
                        print(f"[!] Ignorado por baja confianza: {image_name} ({confidence})")
                        continue
                    
                    # Evitar duplicados
                    if image_name in processed_images:
                        continue
                    
                    print(f"[→] Procesando: {image_name} ({detected_object}) Confianza: {confidence}")
                    
                    # Obtener imagen
                    image_base64 = fetch_image_from_testeo(image_name)
                    
                    if image_base64:
                        # Enviar a Flask
                        if send_to_flask(detected_object, image_base64, confidence):
                            processed_images.add(image_name)
                    else:
                        print(f"[!] No se pudo obtener imagen: {image_name}")
            
            # Esperar antes de la siguiente consulta
            time.sleep(2)
            
        except KeyboardInterrupt:
            print("\n[INFO] Deteniendo puente...")
            break
        except Exception as e:
            print(f"[ERROR] {e}")
            time.sleep(5)

if __name__ == '__main__':
    main()
