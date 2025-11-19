import org.opencv.core.Core;
import java.io.*;
import java.util.*;

/**
 * Servidor Principal de Testeo de Objetos
 * Coordina múltiples cámaras, el log de detecciones y los servidores de socket
 */
public class TestingServer {
    private static final String CONFIG_FILE = "cameras_config.txt";
    
    private final List<Thread> cameraThreads;
    private final List<CameraProcessor> cameraProcessors;
    private Thread logServerThread;
    private Thread imageServerThread;
    private LogServer logServer;
    private ImageServer imageServer;
    
    // Configuración
    private final String pythonScriptPath;
    private final String tempFramePath;
    private final String detectionImagesPath;
    private final int logServerPort;
    private final int imageServerPort;
    private final int frameSkip;
    
    public TestingServer(String pythonScriptPath, 
                        String tempFramePath,
                        String detectionImagesPath,
                        int logServerPort,
                        int imageServerPort,
                        int frameSkip) {
        this.pythonScriptPath = pythonScriptPath;
        this.tempFramePath = tempFramePath;
        this.detectionImagesPath = detectionImagesPath;
        this.logServerPort = logServerPort;
        this.imageServerPort = imageServerPort;
        this.frameSkip = frameSkip;
        
        this.cameraThreads = new ArrayList<>();
        this.cameraProcessors = new ArrayList<>();
    }
    
    /**
     * Inicia el servidor de testeo
     */
    public void start() {
        System.out.println("=".repeat(70));
        System.out.println("SERVIDOR DE TESTEO DE OBJETOS - CC4P1");
        System.out.println("=".repeat(70));
        
        // Cargar librería OpenCV
        System.out.println("\n[INIT] Cargando OpenCV...");
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            System.out.println("[INIT] OpenCV cargado exitosamente");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[ERROR] No se pudo cargar OpenCV: " + e.getMessage());
            System.err.println("[ERROR] Asegúrate de tener OpenCV instalado y configurado");
            return;
        }
        
        // Inicializar DetectionLog
        System.out.println("[INIT] Inicializando sistema de logs...");
        DetectionLog.getInstance(1000);
        
        // Cargar configuración de cámaras
        System.out.println("[INIT] Cargando configuración de cámaras...");
        List<CameraConfig> cameras = loadCameraConfig();
        
        if (cameras.isEmpty()) {
            System.err.println("[ERROR] No se encontraron cámaras configuradas");
            System.err.println("[INFO] Crea un archivo '" + CONFIG_FILE + "' con el formato:");
            System.err.println("       CAM_ID,RTSP_URL");
            System.err.println("       Ejemplo: CAM1,rtsp://192.168.1.100:554/stream");
            return;
        }
        
        System.out.println("[INIT] Se encontraron " + cameras.size() + " cámaras");
        
        // Iniciar servidor de logs (Puerto 9001)
        System.out.println("\n[INIT] Iniciando servidor de logs en puerto " + logServerPort + "...");
        logServer = new LogServer(logServerPort);
        logServerThread = new Thread(logServer);
        logServerThread.start();
        
        // Iniciar servidor de imágenes (Puerto 9002)
        System.out.println("[INIT] Iniciando servidor de imágenes en puerto " + imageServerPort + "...");
        imageServer = new ImageServer(imageServerPort, detectionImagesPath);
        imageServerThread = new Thread(imageServer);
        imageServerThread.start();
        
        // Pequeña pausa para asegurar que los servidores se inicien
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignorar
        }
        
        // Iniciar procesadores de cámara (un hilo por cámara)
        System.out.println("\n[INIT] Iniciando procesamiento de cámaras...");
        for (CameraConfig camera : cameras) {
            CameraProcessor processor = new CameraProcessor(
                camera.id,
                camera.rtspUrl,
                pythonScriptPath,
                tempFramePath,
                detectionImagesPath,
                frameSkip
            );
            
            Thread thread = new Thread(processor);
            thread.setName("Camera-" + camera.id);
            
            cameraProcessors.add(processor);
            cameraThreads.add(thread);
            
            thread.start();
            
            // Pequeña pausa entre cámaras para evitar saturación inicial
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignorar
            }
        }
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("SERVIDOR INICIADO EXITOSAMENTE");
        System.out.println("=".repeat(70));
        System.out.println("Servidor de Logs:     Puerto " + logServerPort);
        System.out.println("Servidor de Imágenes: Puerto " + imageServerPort);
        System.out.println("Cámaras activas:      " + cameras.size());
        System.out.println("Directorio imágenes:  " + detectionImagesPath);
        System.out.println("=".repeat(70));
        System.out.println("\nPresiona Ctrl+C para detener el servidor\n");
    }
    
    /**
     * Detiene el servidor y todos sus hilos
     */
    public void stop() {
        System.out.println("\n[SHUTDOWN] Deteniendo servidor...");
        
        // Detener procesadores de cámara
        System.out.println("[SHUTDOWN] Deteniendo procesamiento de cámaras...");
        for (CameraProcessor processor : cameraProcessors) {
            processor.stop();
        }
        
        // Esperar a que terminen los hilos de cámaras
        for (Thread thread : cameraThreads) {
            try {
                thread.join(5000); // Timeout de 5 segundos
            } catch (InterruptedException e) {
                // Ignorar
            }
        }
        
        // Detener servidores de socket
        System.out.println("[SHUTDOWN] Deteniendo servidores de socket...");
        if (logServer != null) {
            logServer.stop();
        }
        if (imageServer != null) {
            imageServer.stop();
        }
        
        System.out.println("[SHUTDOWN] Servidor detenido");
    }
    
    /**
     * Carga la configuración de cámaras desde archivo
     */
    private List<CameraConfig> loadCameraConfig() {
        List<CameraConfig> cameras = new ArrayList<>();
        
        File configFile = new File(CONFIG_FILE);
        
        if (!configFile.exists()) {
            System.err.println("[CONFIG] Archivo de configuración no encontrado: " + CONFIG_FILE);
            return cameras;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Saltar líneas vacías y comentarios
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split(",");
                
                if (parts.length < 2) {
                    System.err.println("[CONFIG] Línea " + lineNumber + " inválida: " + line);
                    continue;
                }
                
                String id = parts[0].trim();
                String url = parts[1].trim();
                
                cameras.add(new CameraConfig(id, url));
                System.out.println("[CONFIG] Cargada: " + id + " -> " + url);
            }
            
        } catch (IOException e) {
            System.err.println("[CONFIG] Error leyendo configuración: " + e.getMessage());
        }
        
        return cameras;
    }
    
    /**
     * Clase interna para configuración de cámara
     */
    private static class CameraConfig {
        String id;
        String rtspUrl;
        
        CameraConfig(String id, String rtspUrl) {
            this.id = id;
            this.rtspUrl = rtspUrl;
        }
    }
    
    /**
     * Punto de entrada principal
     */
    public static void main(String[] args) {
        // Configuración por defecto (puede ser modificada por argumentos)
        String pythonScript = args.length > 0 ? args[0] : "./detect.py";
        String tempPath = args.length > 1 ? args[1] : "./temp_frames";
        String imagesPath = args.length > 2 ? args[2] : "./detection_images";
        int logPort = args.length > 3 ? Integer.parseInt(args[3]) : 9001;
        int imagePort = args.length > 4 ? Integer.parseInt(args[4]) : 9002;
        int frameSkip = args.length > 5 ? Integer.parseInt(args[5]) : 30; // Procesar 1 de cada 30 frames
        
        final TestingServer server = new TestingServer(
            pythonScript,
            tempPath,
            imagesPath,
            logPort,
            imagePort,
            frameSkip
        );
        
        // Añadir shutdown hook para limpieza ordenada
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
        }));
        
        // Iniciar servidor
        server.start();
        
        // Mantener el programa corriendo
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("\n[MAIN] Programa interrumpido");
        }
    }
}
