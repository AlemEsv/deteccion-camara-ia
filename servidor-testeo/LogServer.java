import java.io.*;
import java.net.*;

/**
 * Servidor de Logs en puerto 9001
 * Escucha conexiones de clientes y responde con el log de detecciones en formato JSON
 */
public class LogServer implements Runnable {
    private final int port;
    private final DetectionLog detectionLog;
    private volatile boolean running;
    private ServerSocket serverSocket;
    
    public LogServer(int port) {
        this.port = port;
        this.detectionLog = DetectionLog.getInstance();
        this.running = true;
    }
    
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[LOG_SERVER] Servidor de logs iniciado en puerto " + port);
            
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
                    System.err.println("[LOG_SERVER] Error en socket: " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            System.err.println("[LOG_SERVER] ERROR: " + e.getMessage());
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
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            System.out.println("[LOG_SERVER] Cliente conectado: " + clientAddress);
            
            // Leer comando del cliente
            String command = in.readLine();
            
            if (command == null) {
                return;
            }
            
            System.out.println("[LOG_SERVER] Comando recibido: " + command);
            
            // Procesar comando
            if (command.trim().equals("GET_LOGS")) {
                // Obtener logs en formato JSON (últimos 100 registros)
                String jsonResponse = detectionLog.getLastDetectionsJSON(100);
                
                // Enviar respuesta
                out.println(jsonResponse);
                
                System.out.println("[LOG_SERVER] Enviados " + 
                    detectionLog.getSize() + " registros a " + clientAddress);
            } 
            else if (command.trim().startsWith("GET_LOGS:")) {
                // GET_LOGS:N - obtener últimos N registros
                try {
                    int n = Integer.parseInt(command.split(":")[1].trim());
                    String jsonResponse = detectionLog.getLastDetectionsJSON(n);
                    out.println(jsonResponse);
                    
                    System.out.println("[LOG_SERVER] Enviados últimos " + n + 
                        " registros a " + clientAddress);
                } catch (Exception e) {
                    out.println("ERROR:Formato inválido");
                }
            }
            else if (command.trim().equals("GET_COUNT")) {
                // Comando adicional: obtener solo el conteo
                out.println("{\"count\":" + detectionLog.getSize() + "}");
            }
            else {
                out.println("ERROR:Comando desconocido");
                System.err.println("[LOG_SERVER] Comando desconocido: " + command);
            }
            
        } catch (IOException e) {
            System.err.println("[LOG_SERVER] Error manejando cliente " + 
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
                System.out.println("[LOG_SERVER] Servidor cerrado");
            } catch (IOException e) {
                System.err.println("[LOG_SERVER] Error cerrando servidor: " + e.getMessage());
            }
        }
    }
}
