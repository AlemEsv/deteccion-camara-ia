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
    private final int maxDetections;
    
    private DetectionLog(int maxDetections) {
        this.detections = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.maxDetections = maxDetections;
    }
    
    /**
     * Obtiene la instancia única del log (Singleton)
     */
    public static synchronized DetectionLog getInstance(int maxDetections) {
        if (instance == null) {
            instance = new DetectionLog(maxDetections);
        }
        return instance;
    }
    
    public static DetectionLog getInstance() {
        return getInstance(1000); // Por defecto 1000 registros
    }
    
    /**
     * Añade una detección al log de forma thread-safe
     * CRÍTICO: Protegido con WriteLock para evitar corrupción de registros
     */
    public void addDetection(Detection detection) {
        writeLock.lock();
        try {
            detections.add(detection);
            
            // Mantener solo las últimas N detecciones para no consumir demasiada memoria
            if (detections.size() > maxDetections) {
                detections.remove(0);
            }
            
            System.out.println("[LOG] " + detection);
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Obtiene todas las detecciones de forma thread-safe
     * Usa ReadLock para permitir lecturas concurrentes
     */
    public List<Detection> getAllDetections() {
        readLock.lock();
        try {
            return new ArrayList<>(detections);
        } finally {
            readLock.unlock();
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
     * Convierte las últimas N detecciones a JSON
     */
    public String getLastDetectionsJSON(int n) {
        List<Detection> lastDetections = getLastDetections(n);
        StringBuilder json = new StringBuilder("[");
        
        for (int i = 0; i < lastDetections.size(); i++) {
            json.append(lastDetections.get(i).toJSON());
            if (i < lastDetections.size() - 1) {
                json.append(",");
            }
        }
        
        json.append("]");
        return json.toString();
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
