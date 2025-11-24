import org.opencv.core.*;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.*;
import java.util.UUID;

/**
 * Procesador de cámara individual que se ejecuta en su propio hilo.
 * Lee frames de una cámara RTSP, llama al script de IA para detección,
 * y registra los resultados.
 */
public class CameraProcessor implements Runnable {
    private final String cameraId;
    private final String rtspUrl;
    private final String pythonScriptPath;
    private final String tempFramePath;
    private final String detectionImagesPath;
    private final DetectionLog detectionLog;
    private final int frameSkip; // Procesar 1 de cada N frames
    private volatile boolean running;
    
    public CameraProcessor(String cameraId, String rtspUrl, 
                          String pythonScriptPath, 
                          String tempFramePath,
                          String detectionImagesPath,
                          int frameSkip) {
        this.cameraId = cameraId;
        this.rtspUrl = rtspUrl;
        this.pythonScriptPath = pythonScriptPath;
        this.tempFramePath = tempFramePath;
        this.detectionImagesPath = detectionImagesPath;
        this.detectionLog = DetectionLog.getInstance();
        this.frameSkip = frameSkip;
        this.running = true;
        
        // Crear directorio temporal si no existe
        new File(tempFramePath).mkdirs();
        new File(detectionImagesPath).mkdirs();
    }
    
    @Override
    public void run() {
        System.out.println("[" + cameraId + "] Iniciando procesamiento de cámara: " + rtspUrl);
        
        // Verificar si es una URL RTSP/HTTP o un archivo local
        boolean isStreamUrl = rtspUrl.startsWith("rtsp://") || rtspUrl.startsWith("http://") || rtspUrl.startsWith("https://");
        
        if (isStreamUrl) {
            processVideoStream();
        } else {
            processVideoFile();
        }
    }
    
    private void processVideoFile() {
        try {
            // Convertir ruta relativa a absoluta
            File videoFile = new File(rtspUrl);
            if (!videoFile.exists()) {
                System.err.println("[" + cameraId + "] ERROR: Archivo no encontrado: " + rtspUrl);
                return;
            }
            
            String videoPath = videoFile.getAbsolutePath();
            System.out.println("[" + cameraId + "] Usando ruta absoluta: " + videoPath);
            
            VideoCapture capture = new VideoCapture();
            capture.open(videoPath);
            
            if (!capture.isOpened()) {
                System.err.println("[" + cameraId + "] ERROR: No se pudo abrir el video");
                System.err.println("[" + cameraId + "] Intentando modo alternativo: procesamiento de imágenes...");
                processImageSequence();
                return;
            }
            
            System.out.println("[" + cameraId + "] Video abierto exitosamente");
            
            // Obtener información del video
            double fps = capture.get(Videoio.CAP_PROP_FPS);
            int totalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);
            System.out.println("[" + cameraId + "] FPS: " + fps + ", Total frames: " + totalFrames);
            
            Mat frame = new Mat();
            int frameCount = 0;
            int processedFrames = 0;
            int errorCount = 0;
            final int MAX_ERRORS = 5;
            
            while (running && capture.isOpened()) {
                boolean success = capture.read(frame);
                
                if (!success || frame.empty()) {
                    errorCount++;
                    
                    // Si llegamos al final del video o hay muchos errores, reiniciar
                    if (frameCount >= totalFrames - 1 || errorCount >= MAX_ERRORS) {
                        System.out.println("[" + cameraId + "] Fin del video o errores consecutivos. Cerrando y reabriendo...");
                        capture.release();
                        Thread.sleep(1000);
                        
                        // Reabrir el video
                        capture.open(videoPath);
                        if (!capture.isOpened()) {
                            System.err.println("[" + cameraId + "] ERROR al reabrir video. Cambiando a modo de imágenes...");
                            processImageSequence();
                            return;
                        }
                        
                        frameCount = 0;
                        errorCount = 0;
                        System.out.println("[" + cameraId + "] Video reabierto exitosamente");
                        continue;
                    }
                    
                    Thread.sleep(500);
                    continue;
                }
                
                errorCount = 0; // Reset error count on successful read
                frameCount++;
                
                // Procesar solo 1 de cada N frames
                if (frameCount % frameSkip != 0) {
                    continue;
                }
                
                processedFrames++;
                System.out.println("[" + cameraId + "] Procesando frame " + frameCount + "/" + totalFrames);
                
                // Guardar frame temporalmente
                String tempImagePath = tempFramePath + "/" + cameraId + "_frame.jpg";
                boolean saved = Imgcodecs.imwrite(tempImagePath, frame);
                
                if (!saved) {
                    System.err.println("[" + cameraId + "] ERROR: No se pudo guardar el frame");
                    continue;
                }
                
                // Llamar al script de detección
                String detectionResult = callDetectionScript(tempImagePath);
                
                if (detectionResult != null && !detectionResult.trim().isEmpty()) {
                    processDetectionResult(detectionResult, frame);
                }
                
                Thread.sleep(1000); // Pausa entre frames procesados
            }
            
            capture.release();
            
        } catch (Exception e) {
            System.err.println("[" + cameraId + "] ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void processImageSequence() {
        // Modo alternativo: procesar imágenes del dataset en bucle
        try {
            File datasetDir = new File("../modelo-ia/dataset/images/val");
            if (!datasetDir.exists()) {
                datasetDir = new File("modelo-ia/dataset/images/val");
            }
            
            if (!datasetDir.exists()) {
                System.err.println("[" + cameraId + "] ERROR: No se encontró el directorio de imágenes");
                return;
            }
            
            File[] images = datasetDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
            
            if (images == null || images.length == 0) {
                System.err.println("[" + cameraId + "] ERROR: No hay imágenes en el directorio");
                return;
            }
            
            System.out.println("[" + cameraId + "] Modo secuencia de imágenes: " + images.length + " imágenes encontradas");
            
            int imageIndex = 0;
            while (running) {
                File imageFile = images[imageIndex % images.length];
                System.out.println("[" + cameraId + "] Procesando imagen: " + imageFile.getName());
                
                // Leer imagen
                Mat frame = Imgcodecs.imread(imageFile.getAbsolutePath());
                
                if (frame.empty()) {
                    System.err.println("[" + cameraId + "] ERROR al leer imagen: " + imageFile.getName());
                    imageIndex++;
                    continue;
                }
                
                // Guardar como frame temporal
                String tempImagePath = tempFramePath + "/" + cameraId + "_frame.jpg";
                Imgcodecs.imwrite(tempImagePath, frame);
                
                // Llamar al script de detección
                String detectionResult = callDetectionScript(tempImagePath);
                
                if (detectionResult != null && !detectionResult.trim().isEmpty()) {
                    processDetectionResult(detectionResult, frame);
                }
                
                frame.release();
                imageIndex++;
                Thread.sleep(3000); // Procesar una imagen cada 3 segundos
            }
            
        } catch (Exception e) {
            System.err.println("[" + cameraId + "] ERROR en secuencia de imágenes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void processVideoStream() {
        VideoCapture capture = new VideoCapture();
        
        try {
            // Convertir ruta relativa a absoluta si es un archivo local
            String videoPath = rtspUrl;
            if (!rtspUrl.startsWith("rtsp://") && !rtspUrl.startsWith("http://") && !rtspUrl.startsWith("https://")) {
                File videoFile = new File(rtspUrl);
                if (videoFile.exists()) {
                    videoPath = videoFile.getAbsolutePath();
                    System.out.println("[" + cameraId + "] Usando ruta absoluta: " + videoPath);
                } else {
                    System.err.println("[" + cameraId + "] ERROR: Archivo no encontrado: " + rtspUrl);
                    return;
                }
            }
            
            // Conectar a la cámara RTSP o archivo de video
            capture.open(videoPath);
            
            if (!capture.isOpened()) {
                System.err.println("[" + cameraId + "] ERROR: No se pudo conectar a la cámara");
                return;
            }
            
            // Configurar propiedades de captura
            capture.set(Videoio.CAP_PROP_BUFFERSIZE, 1); // Minimizar latencia
            
            System.out.println("[" + cameraId + "] Conectado exitosamente");
            
            Mat frame = new Mat();
            int frameCount = 0;
            
            while (running) {
                // Leer frame
                if (!capture.read(frame) || frame.empty()) {
                    System.err.println("[" + cameraId + "] ERROR: No se pudo leer frame");
                    Thread.sleep(1000);
                    continue;
                }
                
                frameCount++;
                
                // Procesar solo 1 de cada N frames para optimizar rendimiento
                if (frameCount % frameSkip != 0) {
                    continue;
                }
                
                // Guardar frame temporalmente
                String tempImagePath = tempFramePath + "/" + cameraId + "_frame.jpg";
                Imgcodecs.imwrite(tempImagePath, frame);
                
                // Llamar al script de detección de IA
                String detectionResult = callDetectionScript(tempImagePath);
                
                // Procesar resultado
                if (detectionResult != null && !detectionResult.isEmpty()) {
                    processDetectionResult(detectionResult, frame);
                }
                
                // Pequeña pausa para no saturar el CPU
                Thread.sleep(100);
            }
            
        } catch (InterruptedException e) {
            System.out.println("[" + cameraId + "] Hilo interrumpido");
        } catch (Exception e) {
            System.err.println("[" + cameraId + "] ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            capture.release();
            System.out.println("[" + cameraId + "] Procesamiento finalizado");
        }
    }
    
    /**
     * Llama al script Python de detección usando ProcessBuilder
     */
    private String callDetectionScript(String imagePath) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", pythonScriptPath, imagePath);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Leer salida del script
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                System.err.println("[" + cameraId + "] Script Python terminó con código: " + exitCode);
                System.err.println("[" + cameraId + "] Salida: " + output.toString());
            }
            
            return output.toString().trim();
            
        } catch (Exception e) {
            System.err.println("[" + cameraId + "] ERROR llamando al script de IA: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Procesa el resultado de la detección
     * Formato esperado: OBJETO,CONFIDENCE,X,Y,W,H
     * Ejemplo: CARRO,0.95,100,200,50,30
     */
    private void processDetectionResult(String result, Mat frame) {
        try {
            String[] lines = result.split("\n");
            
            for (String line : lines) {
                if (line.isEmpty() || !line.contains(",")) {
                    continue;
                }
                
                String[] parts = line.split(",");
                
                if (parts.length < 2) {
                    continue;
                }
                
                String objeto = parts[0].trim();
                double confidence = Double.parseDouble(parts[1].trim());
                
                // Solo registrar detecciones con confianza mayor a 0.5
                if (confidence < 0.5) {
                    continue;
                }
                
                // Generar nombre único para la imagen
                String imageFileName = UUID.randomUUID().toString() + ".jpg";
                String imageFilePath = detectionImagesPath + "/" + imageFileName;
                
                // Guardar imagen de la detección
                Imgcodecs.imwrite(imageFilePath, frame);
                
                // Crear registro de detección
                Detection detection = new Detection(cameraId, objeto, imageFileName, confidence);
                
                // Añadir al log (thread-safe)
                detectionLog.addDetection(detection);
            }
            
        } catch (Exception e) {
            System.err.println("[" + cameraId + "] ERROR procesando resultado: " + e.getMessage());
        }
    }
    
    /**
     * Detiene el procesamiento de la cámara
     */
    public void stop() {
        running = false;
    }
    
    public String getCameraId() {
        return cameraId;
    }
}
