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
        
        VideoCapture capture = new VideoCapture();
        
        try {
            // Conectar a la cámara RTSP
            capture.open(rtspUrl);
            
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
            ProcessBuilder pb = new ProcessBuilder("python3", pythonScriptPath, imagePath);
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
            
            process.waitFor();
            
            return output.toString().trim();
            
        } catch (Exception e) {
            System.err.println("[" + cameraId + "] ERROR llamando al script de IA: " + e.getMessage());
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
