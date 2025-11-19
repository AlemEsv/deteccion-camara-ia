import java.io.*;
import java.net.*;
import java.nio.file.*;

/**
 * Servidor de Imágenes en puerto 9002
 * Sirve las imágenes de detecciones a los clientes
 */
public class ImageServer implements Runnable {
    private final int port;
    private final String imagesPath;
    private volatile boolean running;
    private ServerSocket serverSocket;
    
    public ImageServer(int port, String imagesPath) {
        this.port = port;
        this.imagesPath = imagesPath;
        this.running = true;
        
        // Asegurar que el directorio existe
        new File(imagesPath).mkdirs();
    }
    
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[IMAGE_SERVER] Servidor de imágenes iniciado en puerto " + port);
            
            while (running) {
                try {
                    // Aceptar conexión de cliente
                    Socket clientSocket = serverSocket.accept();
                    
                    // Procesar la petición en un nuevo hilo
                    Thread clientHandler = new Thread(() -> handleClient(clientSocket));
                    clientHandler.start();
                    
                } catch (SocketException e) {
                    if (!running) {
                        break; // Salida normal
                    }
                    System.err.println("[IMAGE_SERVER] Error en socket: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("[IMAGE_SERVER] ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeServer();
        }
    }
    
    /**
     * Maneja la petición de un cliente individual
     */
    private void handleClient(Socket clientSocket) {
        String clientAddress = clientSocket.getInetAddress().getHostAddress();
        
        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
            );
            OutputStream out = clientSocket.getOutputStream();
        ) {
            System.out.println("[IMAGE_SERVER] Cliente conectado: " + clientAddress);
            
            // Leer comando del cliente
            String command = in.readLine();
            
            if (command == null) {
                return;
            }
            
            System.out.println("[IMAGE_SERVER] Comando recibido: " + command);
            
            // Procesar comando GET_IMAGE:filename.jpg
            if (command.trim().startsWith("GET_IMAGE:")) {
                String filename = command.split(":", 2)[1].trim();
                
                // Validar nombre de archivo (seguridad básica)
                if (!isValidFilename(filename)) {
                    sendError(out, "Nombre de archivo inválido");
                    return;
                }
                
                File imageFile = new File(imagesPath, filename);
                
                if (!imageFile.exists() || !imageFile.isFile()) {
                    sendError(out, "Imagen no encontrada");
                    System.err.println("[IMAGE_SERVER] Imagen no encontrada: " + filename);
                    return;
                }
                
                // Enviar imagen
                sendImage(out, imageFile);
                
                System.out.println("[IMAGE_SERVER] Imagen enviada: " + filename + 
                    " (" + imageFile.length() + " bytes) a " + clientAddress);
            } 
            else if (command.trim().equals("LIST_IMAGES")) {
                // Comando adicional: listar todas las imágenes disponibles
                sendImageList(out);
            }
            else {
                sendError(out, "Comando desconocido");
                System.err.println("[IMAGE_SERVER] Comando desconocido: " + command);
            }
            
        } catch (IOException e) {
            System.err.println("[IMAGE_SERVER] Error manejando cliente " + 
                clientAddress + ": " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignorar
            }
        }
    }
    
    /**
     * Envía una imagen al cliente
     * Formato: FILESIZE:<bytes>\n[...DATA...]
     */
    private void sendImage(OutputStream out, File imageFile) throws IOException {
        long fileSize = imageFile.length();
        
        // Enviar header con tamaño
        String header = "FILESIZE:" + fileSize + "\n";
        out.write(header.getBytes());
        out.flush();
        
        // Enviar bytes de la imagen
        try (FileInputStream fis = new FileInputStream(imageFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            
            out.flush();
        }
    }
    
    /**
     * Envía un mensaje de error al cliente
     */
    private void sendError(OutputStream out, String errorMessage) throws IOException {
        String response = "ERROR:" + errorMessage + "\n";
        out.write(response.getBytes());
        out.flush();
    }
    
    /**
     * Envía la lista de imágenes disponibles
     */
    private void sendImageList(OutputStream out) throws IOException {
        File dir = new File(imagesPath);
        File[] files = dir.listFiles((d, name) -> 
            name.toLowerCase().endsWith(".jpg") || 
            name.toLowerCase().endsWith(".jpeg") ||
            name.toLowerCase().endsWith(".png")
        );
        
        StringBuilder list = new StringBuilder("IMAGES:");
        
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                list.append(files[i].getName());
                if (i < files.length - 1) {
                    list.append(",");
                }
            }
        }
        
        list.append("\n");
        out.write(list.toString().getBytes());
        out.flush();
    }
    
    /**
     * Valida que el nombre de archivo sea seguro
     * Previene path traversal attacks
     */
    private boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        // No permitir caracteres peligrosos
        if (filename.contains("..") || 
            filename.contains("/") || 
            filename.contains("\\")) {
            return false;
        }
        
        // Solo permitir extensiones de imagen
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || 
               lower.endsWith(".jpeg") || 
               lower.endsWith(".png");
    }
    
    /**
     * Detiene el servidor
     */
    public void stop() {
        running = false;
        closeServer();
    }
    
    private void closeServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("[IMAGE_SERVER] Servidor cerrado");
            } catch (IOException e) {
                System.err.println("[IMAGE_SERVER] Error cerrando servidor: " + e.getMessage());
            }
        }
    }
}
