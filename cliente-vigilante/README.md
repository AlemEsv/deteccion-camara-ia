# Cliente Vigilante de Objetos (Java - Swing)

Este módulo implementa el **cliente vigilante** del sistema distribuido de detección de objetos.

Se conecta mediante **sockets TCP** al backend desarrollado en Python:

- Servidor de logs: puerto **9001**
  - Protocolo: `GET_LOGS\n` → respuesta: JSON con una lista de detecciones
- Servidor de imágenes: puerto **9002**
  - Protocolo: `GET_IMAGE:<nombre>\n` → respuesta: `FILESIZE:n\n` + bytes de la imagen

## Estructura

- `DetectionDTO.java`  
  Modelo de datos para una detección (cámara, objeto, fecha/hora, nombre de imagen).

- `VigilanteClient.java`  
  Capa de comunicación por sockets. Expone:
  - `List<DetectionDTO> fetchLogs()`
  - `BufferedImage fetchImage(String imageName)`

- `DetectionTableModel.java`  
  Modelo de tabla (`AbstractTableModel`) para mostrar las detecciones en un `JTable`.

- `ImagePanel.java`  
  Panel personalizado que muestra la imagen asociada a la detección seleccionada.

- `VigilanteApp.java`  
  Aplicación Swing principal. Construye la interfaz gráfica:
  - Panel izquierdo: tabla de detecciones + botón "Refrescar"
  - Panel derecho: imagen de la detección seleccionada + resumen de datos

## Ejecución en VS Code

1. Instalar el **Extension Pack for Java**.
2. Abrir la carpeta `cliente-vigilante` en VS Code.
3. Asegurarse de que el servidor Python (`log_server.py`) esté corriendo
   y escuchando en los puertos 9001 y 9002.
4. Ejecutar la clase `VigilanteApp` (botón de "Run" en VS Code sobre el método `main`).

Si el servidor se ejecuta en otra máquina de la LAN, editar en `VigilanteApp.java`:

```java
String host = "IP_DEL_SERVIDOR";