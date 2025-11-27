import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Log sincronizado de detecciones usando ReentrantReadWriteLock
 * para evitar corrupción de registros en ambiente concurrente.
 * Implementa patrón Singleton.
 */
public class DetectionLog {
    private static DetectionLog instance;
    private final List<Detection> detections;
    private final ReentrantReadWriteLock lock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    
    private DetectionLog() {
        this.detections = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }
    
    /**
     * Obtiene la instancia única del log (Singleton)
     */
    public static synchronized DetectionLog getInstance() {
        if (instance == null) {
            instance = new DetectionLog();
        }
        return instance;
    }
    
    /**
     * Añade una detección al log de forma thread-safe
     * CRÍTICO: Protegido con WriteLock para evitar corrupción de registros
     */
    public void addDetection(Detection detection) {
        writeLock.lock();
        try {
            detections.add(detection);
            System.out.println("[LOG] " + detection);
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Obtiene todas las detecciones de forma thread-safe
     * Usa ReadLock para permitir lecturas concurrentes
     * Al leer, limpia el log y reactiva la captura
     */
    public List<Detection> getAllDetections() {
        writeLock.lock();
        try {
            List<Detection> result = new ArrayList<>(detections);
            detections.clear();
            System.out.println("[LOG] Enviando " + result.size() + " detecciones. Log limpiado.");
            return result;
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Obtiene las últimas N detecciones
     */
    public List<Detection> getLastDetections(int n) {
        readLock.lock();
        try {
            int size = detections.size();
            int fromIndex = Math.max(0, size - n);
            return new ArrayList<>(detections.subList(fromIndex, size));
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Convierte las últimas N detecciones a JSON y limpia el log
     * Esto reactiva la captura de nuevas detecciones
     */
    public String getLastDetectionsJSON(int n) {
        writeLock.lock();
        try {
            // Obtener todas las detecciones actuales
            List<Detection> currentDetections = new ArrayList<>(detections);
            
            // Construir JSON
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < currentDetections.size(); i++) {
                json.append(currentDetections.get(i).toJSON());
                if (i < currentDetections.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");
            
            // Limpiar el log y reactivar captura
            detections.clear();
            System.out.println("[LOG] Enviando " + currentDetections.size() + " detecciones.");
            
            return json.toString();
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Obtiene el número de detecciones registradas
     */
    public int getSize() {
        readLock.lock();
        try {
            return detections.size();
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * Limpia todas las detecciones
     */
    public void clear() {
        writeLock.lock();
        try {
            detections.clear();
            System.out.println("[LOG] Log limpiado");
        } finally {
            writeLock.unlock();
        }
    }
}
