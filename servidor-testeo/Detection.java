import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo que representa una detección realizada por el sistema de IA
 */
public class Detection {
    private String camara;
    private String objeto;
    private String fecha;
    private String imagen;
    private double confidence;
    
    private static final DateTimeFormatter formatter = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    public Detection(String camara, String objeto, String imagen, double confidence) {
        this.camara = camara;
        this.objeto = objeto;
        this.fecha = LocalDateTime.now().format(formatter);
        this.imagen = imagen;
        this.confidence = confidence;
    }
    
    // Getters
    public String getCamara() { return camara; }
    public String getObjeto() { return objeto; }
    public String getFecha() { return fecha; }
    public String getImagen() { return imagen; }
    public double getConfidence() { return confidence; }
    
    /**
     * Convierte la detección a formato JSON
     */
    public String toJSON() {
        return String.format(
            "{\"camara\":\"%s\",\"objeto\":\"%s\",\"fecha\":\"%s\",\"imagen\":\"%s\",\"confidence\":%.2f}",
            camara, objeto, fecha, imagen, confidence
        );
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s detectó: %s (%.2f%%) - %s", 
            fecha, camara, objeto, confidence * 100, imagen);
    }
}
