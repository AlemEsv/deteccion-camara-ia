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
    private volatile boolean paused;
    
    private DetectionLog(int maxDetections) {
        this.detections = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
        this.maxDetections = maxDetections;
        this.paused = false;
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
        // Si está pausado, no agregar más detecciones
        if (paused) {
            return;
        }
        
        writeLock.lock();
        try {
            detections.add(detection);
            
            // Si alcanzamos el máximo, pausar hasta que el cliente haga refresh
            if (detections.size() >= maxDetections) {
                paused = true;
                System.out.println("[LOG] Límite de " + maxDetections + " detecciones alcanzado. Pausando captura.");
            }
            
            System.out.println("[LOG] " + detection);
        } finally {
            writeLock.unlock();
        }
    }
    
    /**
     * Verifica si el log está pausado
     */
    public boolean isPaused() {
        return paused;
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
            paused = false;
            System.out.println("[LOG] Enviando " + result.size() + " detecciones. Log limpiado y captura reactivada.");
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
     * Limpia todas las detecciones y reactiva la captura
     */
    public void clear() {
        writeLock.lock();
        try {
            detections.clear();
            paused = false;
            System.out.println("[LOG] Log limpiado y captura reactivada");
        } finally {
            writeLock.unlock();
        }
    }
}
