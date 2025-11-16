# PROYECTO: Sistema Distribuido de Detección (CC4P1)

## 1. Stack Tecnológico

- **IA (Detección):** Python + YOLOv8 (usando `ultralytics` para entrenar y `cv2.dnn` u `onnxruntime` para inferencia rápida).

- **Servidor de Testeo (Alto Rendimiento):** Java. Manejará la concurrencia (hilos) para las cámaras y el log.

- **Servidor de Entrenamiento:** Node.js. Manejará la recepción de datos de entrenamiento vía sockets.

- **Cliente Vigilante:** Java (JavaFX o Swing).

- **Protocolo de Video:** RTSP.

- **Protocolo de Comunicación:** Sockets TCP crudos (protocolo binario/texto personalizado).

## 2. Restricciones Clave (Recordatorio)

- **PROHIBIDO:** `websocket`, `socketio`, `frameworks` (Flask, Express, Spring), `RabbitMQ`, `MQ`, o cualquier librería de comunicación de alto nivel.

- **OBLIGATORIO:** Usar `Sockets` puros para toda la comunicación entre módulos.

- **OBLIGATORIO:** Usar `Hilos` para concurrencia y evitar corrupción de registros (especialmente en el Servidor de Testeo).

- **OBLIGATORIO:** Desplegar y probar en redes LAN y WIFI.

---

## 3. Protocolo de Sockets (Definición Central)

### Puerto 9000 (Node.js): Servidor de Entrenamiento

- **Cliente (Admin) -> Servidor (Node.js)**

  - **Propósito:** Subir una imagen para entrenar.

  - **Mensaje:** `UPLOAD:<label>:<filename>:<filesize>\n[...DATA..._]`

    - `[...DATA..._]` son los bytes exactos de la imagen, de tamaño `filesize`.

- **Cliente (Admin) -> Servidor (Node.js)**

  - **Propósito:** Iniciar el proceso de entrenamiento.

  - **Mensaje:** `START_TRAIN\n`

  - **Respuesta (Servidor):** `TRAINING_STARTED\n` (y luego `TRAINING_COMPLETE\n` cuando termine).

### Puerto 9001 (Java): Servidor de Testeo (Log de Detecciones)

- **Cliente (Vigilante) -> Servidor (Java)**

  - **Propósito:** Pedir los últimos registros de detección.

  - **Mensaje:** `GET_LOGS\n`

  - **Respuesta (Servidor):** Un string JSON con los últimos N registros.

    - `[{"camara": "CAM1", "objeto": "Carro", "fecha": "...", "imagen": "uuid-123.jpg"}, ...]\n`

### Puerto 9002 (Java): Servidor de Testeo (Servidor de Imágenes)

- **Cliente (Vigilante) -> Servidor (Java)**

  - **Propósito:** Solicitar una imagen específica del log.

  - **Mensaje:** `GET_IMAGE:uuid-123.jpg\n`

  - **Respuesta (Servidor):** `FILESIZE:<bytes>\n[...DATA..._]`

    - `[...DATA..._]` son los bytes crudos de la imagen JPEG.

---

## 4.  División de Tareas

### Servidor de Testeo (Java)

El módulo más complejo, maneja la concurrencia de las cámaras y las peticiones del cliente.

- **Módulo:** `Servidor de Testeo de Objetos` (Java).

- **Tareas:**

    1. **Gestor de Cámaras (Hilos):** Crear una clase `CameraProcessor` que implemente `Runnable`. Al iniciar el servidor, se debe crear un Hilo (`Thread`) por cada cámara ("c" cámaras).

    2. **Lector RTSP:** Cada hilo debe usar una librería (ej. **OpenCV para Java** o `vlcj`) para conectarse al stream RTSP de su cámara y obtener _frames_.

    3. **Llamada a IA:** Por cada _frame_, guardarlo temporalmente en disco y usar `ProcessBuilder` de Java para llamar al script `detect.py` de la Persona 2 (Ej: `python detect.py /tmp/cam1_frame.jpg`).

    4. **Log Sincronizado:** Crear una clase `DetectionLog` (Singleton o compartida) que contenga una `List<Detection>`. **CRÍTICO:** Todos los métodos para escribir (`addDetection`) en esta lista deben estar protegidos por un **`ReentrantLock`** o un bloque **`synchronized`** para cumplir el requisito de "evitar corrupción de registros".

    5. **Servidor de Logs (Socket 9001):** Implementar un `ServerSocket` en el puerto 9001. Debe tener un hilo que escuche conexiones. Cuando un Cliente Vigilante envía `GET_LOGS\n`, este hilo debe (usando el _lock_ de lectura) tomar los datos del `DetectionLog`, serializarlos a JSON y enviarlos por el socket.

    6. **Servidor de Imágenes (Socket 9002):** Implementar un `ServerSocket` en el puerto 9002. Cuando un cliente pide `GET_IMAGE:file.jpg\n`, debe leer el archivo de imagen del disco (en la carpeta de detecciones) y transmitir los bytes crudos por el socket.

### Módulo de IA (python)

Entrenamiento y detección, orquesta _llamados_ por los servidores de Java y Node.js.

- **Módulo:** Módulo de IA (Python + YOLOv8).

- **Tareas:**

    1. **Configurar YOLOv8:** Instalar `ultralytics` y descargar un modelo pre-entrenado (ej. `yolov8n.pt`).

    2. **Dataset:** Definir el dataset para las "n" clases (ej. "Naranja", "Maria", "Perro").

    3. **Script `train.py`:** Crear un script de Python que:

        - Cargue el modelo YOLOv8 pre-entrenado.

        - Realice _transfer learning_ (entrenamiento) con el dataset local.

        - Guarde el modelo entrenado (ej. `best.onnx` o `best.pt`) en una carpeta compartida.

    4. **Script `detect.py`:** Crear un script que **NO es un servidor**.

        - Debe aceptar un path de imagen como argumento de línea de comandos (ej. `sys.argv[1]`).

        - Debe cargar el modelo entrenado (`best.onnx` o `best.pt`) usando el método más rápido posible (ej. `cv2.dnn.readNetFromOnnx`).

        - Realizar la detección en la imagen.

        - **CRÍTICO:** Imprimir las detecciones en `stdout` en un formato simple que Java pueda _parsear_ (ej: `CARRO,0.95,100,200,50,30`).

        - Este script debe ser ligero y rápido, ya que se llamará por cada _frame_.

### Servidor de Entrenamiento (Node.js)

Construye el servidor que permite al equipo "alimentar" al modelo de IA con nuevos datos de entrenamiento.

- **Módulo:** `Servidor de Entrenamiento de Objetos` (Node.js).

- **Tareas:**

    1. **Servidor Socket (Puerto 9000):** Crear un servidor TCP usando el módulo `net` de Node.js (`net.createServer()`). **No usar** `socket.io`.

    2. **Parser de Protocolo:** Implementar la lógica para _parsear_ los mensajes de socket (el protocolo `UPLOAD:...` definido arriba). Esto implicará manejar _buffers_ de datos binarios para recibir las imágenes.

    3. **Guardar Archivos:** Cuando se recibe un `UPLOAD`, guardar la imagen en la carpeta del dataset que usa la Persona 2.

    4. **Llamada al Entrenamiento:** Al recibir `START_TRAIN`, usar `child_process.spawn()` para ejecutar el script `train.py` de la Persona 2.

    5. **Retroalimentación:** Capturar el `stdout` del script de Python y reportar el estado (ej. `TRAINING_COMPLETE\n`) de vuelta al cliente que lo solicitó.

    6. **Cliente de Prueba:** Crear un script simple `client-train.js` (usando el módulo `net`) que permita probar tu servidor subiendo una imagen.

### Cliente Vigilante (Java)

Construir la interfaz que el usuario final utilizará para monitorear las detecciones del sistema.

- **Módulo:** `Cliente Vigilante de Objetos` (Java - JavaFX o Swing).

- **Tareas:**

    1. **Diseño de UI:** Crear la interfaz gráfica. Debe tener:

        - Una **tabla** para mostrar el log (Tipo, Fecha, Hora, Cámara).

        - Un **visor de imágenes** para mostrar la foto de la detección.

    2. **Cliente de Logs (Socket 9001):** Implementar la lógica de cliente (`Socket`) para conectarse al Servidor de Testeo (Puerto 9001).

    3. **Hilo de Actualización:** Crear un hilo (`Thread` o `TimerTask`) que, cada 2 segundos:

        - Se conecte al socket 9001.

        - Envíe el comando `GET_LOGS\n`.

        - Reciba la respuesta (el string JSON).

        - _Parsear_ el JSON y actualizar la tabla de la UI (usando `Platform.runLater` si es JavaFX).

    4. **Cliente de Imágenes (Socket 9002):**

        - Añadir un _listener_ a la tabla.

        - Cuando el usuario haga clic en una fila, obtener el nombre del archivo de imagen (ej. `uuid-123.jpg`).

        - Conectarse al Servidor de Testeo (Puerto 9002), enviar `GET_IMAGE:uuid-123.jpg\n`.

        - Recibir los bytes de la imagen (leer el `FILESIZE` primero) y mostrarla en el visor de imágenes.

### Integración y Documentación

Asegura que todas las partes funcionen juntas, gestionar la documentación y liderar las pruebas finales.

- **Módulo:** Integración, Despliegue y Documentación.

- **Tareas:**

    1. **Diagramas:** Crear los gráficos obligatorios:

        - **Diagrama de Arquitectura** (similar al del PDF pero con el stack).

        - **Diagrama de Protocolo** (detallando los mensajes de socket).

    2. **Pruebas de Despliegue:** Configurar las IPs correctas en todos los módulos y probar la conectividad.

    3. **Redacción de Entregables:** Recopilar la información de todos y redactar los entregables finales:

        - **Informe (PDF):** Explicar la arquitectura, el protocolo, las decisiones de diseño y los desafíos (especialmente la concurrencia).

        - **Presentación (PPTX):** Resumir el proyecto para la exposición.

---
