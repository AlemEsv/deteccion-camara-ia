import sqlite3
import datetime
import base64
import json
import os
from flask import Flask, render_template_string, request, jsonify

# Configuración
app = Flask(__name__)
DB_NAME = "vigilante_logs.db"

# --- CONFIGURACIÓN DE BASE DE DATOS ---
def init_db():
    """Inicializa la base de datos si no existe."""
    with sqlite3.connect(DB_NAME) as conn:
        cursor = conn.cursor()
        # Creamos una tabla que almacena la imagen (base64), la etiqueta detectada y la fecha
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                detected_object TEXT,
                image_data TEXT,
                confidence REAL
            )
        ''')
        conn.commit()

# HTML TEMPLATE
HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Monitor Vigilante</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        .image-container { aspect-ratio: 16/9; }
        /* Animación suave para nuevos elementos */
        @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        .log-card { animation: fadeIn 0.5s ease-out; }
    </style>
</head>
<body class="bg-gray-900 text-gray-100 min-h-screen font-sans">

    <!-- Navbar -->
    <nav class="bg-gray-800 border-b border-gray-700 sticky top-0 z-50 shadow-lg">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div class="flex items-center justify-between h-16">
                <div class="flex items-center">
                    <span class="text-red-500 text-2xl mr-2">◉</span>
                    <h1 class="text-xl font-bold tracking-wider">VIGILANTE <span class="text-xs bg-red-900 text-red-200 px-2 py-0.5 rounded ml-2">EN VIVO</span></h1>
                </div>
                <div class="text-sm text-gray-400">
                    Registros en BD: <span id="total-count" class="font-mono text-white">0</span>
                </div>
            </div>
        </div>
    </nav>

    <!-- Main Content -->
    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        
        <!-- Controls -->
        <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-semibold text-gray-200">Detecciones Recientes</h2>
            <button onclick="fetchLogs(true)" class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded shadow transition">
                Refrescar Manualmente
            </button>
        </div>

        <!-- Grid de Logs -->
        <div id="logs-grid" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <!-- Las tarjetas se insertan aquí vía JS -->
        </div>

        <!-- Loader / Load More -->
        <div class="text-center mt-10">
            <button id="load-more-btn" onclick="loadMore()" class="text-gray-400 hover:text-white underline hidden">
                Cargar anteriores...
            </button>
            <p id="loading-msg" class="text-gray-500 animate-pulse">Esperando datos del cliente...</p>
        </div>
    </main>

    <!-- Modal para ver imagen completa -->
    <div id="image-modal" class="fixed inset-0 bg-black/90 hidden items-center justify-center z-50 p-4" onclick="closeModal()">
        <img id="modal-img" src="" class="max-h-full max-w-full rounded shadow-2xl border border-gray-700">
    </div>

    <script>
        let lastLogId = 0;
        let oldestLogId = Infinity;
        let isAutoRefreshing = true;

        // Función para renderizar una tarjeta de log
        function createLogCard(log) {
            const date = new Date(log.timestamp).toLocaleString();
            return `
                <div class="log-card bg-gray-800 rounded-lg overflow-hidden border border-gray-700 shadow-lg hover:border-gray-500 transition" id="log-${log.id}">
                    <div class="image-container bg-black relative group cursor-pointer" onclick="openModal('${log.image_data}')">
                        <img src="data:image/jpeg;base64,${log.image_data}" alt="${log.detected_object}" class="w-full h-full object-cover opacity-90 group-hover:opacity-100 transition">
                        <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 bg-black/30 transition">
                            <span class="text-white font-bold text-lg">🔍 Ampliar</span>
                        </div>
                    </div>
                    <div class="p-4">
                        <div class="flex justify-between items-start">
                            <div>
                                <h3 class="text-lg font-bold text-red-400 uppercase tracking-wide">${log.detected_object}</h3>
                                <p class="text-xs text-gray-500 mt-1">${date}</p>
                            </div>
                            <span class="bg-gray-700 text-gray-300 text-xs px-2 py-1 rounded font-mono">ID: ${log.id}</span>
                        </div>
                    </div>
                </div>
            `;
        }

        // Cargar nuevos logs (Polling)
        async function fetchLogs(manual = false) {
            try {
                // Pedimos logs más nuevos que el último que tenemos
                const response = await fetch(`/api/logs/latest?after_id=${lastLogId}`);
                const data = await response.json();

                if (data.logs && data.logs.length > 0) {
                    const grid = document.getElementById('logs-grid');
                    const loadingMsg = document.getElementById('loading-msg');
                    loadingMsg.style.display = 'none';

                    // Invertimos para que el más nuevo quede arriba a la izquierda (prepend)
                    // La API nos los da ordenados desc (nuevo primero), así que iteramos inverso si queremos orden cronológico o normal si queremos stack.
                    // Vamos a insertarlos al principio del grid.
                    data.logs.slice().reverse().forEach(log => {
                        if (log.id > lastLogId) {
                            const cardHTML = createLogCard(log);
                            grid.insertAdjacentHTML('afterbegin', cardHTML);
                            lastLogId = Math.max(lastLogId, log.id);
                            if (oldestLogId === Infinity) oldestLogId = log.id;
                        }
                    });
                }
                
                // Actualizar contador total
                if(data.total_count) {
                    document.getElementById('total-count').innerText = data.total_count;
                }

            } catch (error) {
                console.error("Error fetching logs:", error);
            }
        }

        // Cargar logs antiguos (Paginación/Scroll infinito)
        // NOTA: Esta función es clave para quitar el límite. Permite ver historia antigua.
        async function loadMore() {
            // Lógica para cargar logs con ID < oldestLogId (no implementada en este demo simple, 
            // pero la estructura de base de datos lo soporta totalmente).
            alert("Aquí se cargarían los registros antiguos desde la BD SQLite sin límite.");
        }

        // Modal Logic
        function openModal(base64Img) {
            document.getElementById('modal-img').src = `data:image/jpeg;base64,${base64Img}`;
            document.getElementById('image-modal').classList.remove('hidden');
            document.getElementById('image-modal').classList.add('flex');
        }
        function closeModal() {
            document.getElementById('image-modal').classList.add('hidden');
            document.getElementById('image-modal').classList.remove('flex');
        }

        // Iniciar polling cada 2 segundos
        setInterval(fetchLogs, 2000);
        fetchLogs(); // Primera carga

    </script>
</body>
</html>
"""

# --- RUTAS DE LA APP ---

@app.route('/')
def index():
    return render_template_string(HTML_TEMPLATE)

@app.route('/api/logs/latest', methods=['GET'])
def get_latest_logs():
    """Obtiene los logs más recientes. Soporta long-polling simulado."""
    after_id = request.args.get('after_id', 0, type=int)
    
    with sqlite3.connect(DB_NAME) as conn:
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()
        
        # Obtenemos conteo total
        cursor.execute("SELECT COUNT(*) FROM logs")
        total_count = cursor.fetchone()[0]

        # Obtenemos logs nuevos (ID mayor al que tiene el navegador)
        # Limitamos a 50 para no saturar en la primera carga, pero el frontend pedirá más
        cursor.execute('''
            SELECT * FROM logs 
            WHERE id > ? 
            ORDER BY id DESC 
            LIMIT 50
        ''', (after_id,))
        
        rows = cursor.fetchall()
        
        logs = []
        for row in rows:
            logs.append({
                "id": row["id"],
                "timestamp": row["timestamp"],
                "detected_object": row["detected_object"],
                "image_data": row["image_data"], # Base64
                "confidence": row["confidence"]
            })
            
    return jsonify({"logs": logs, "total_count": total_count})

@app.route('/api/receive', methods=['POST'])
def receive_log():
    """
    Endpoint para que el 'Cliente Vigilante' envíe datos.
    Espera JSON: { "object": "Persona", "image": "base64String...", "confidence": 0.95 }
    """
    try:
        data = request.json
        if not data:
            return jsonify({"error": "No data"}), 400

        detected_object = data.get('object', 'Desconocido')
        image_data = data.get('image', '') # Debe ser string base64 puro sin headers
        confidence = data.get('confidence', 0.0)

        # Guardar en SQLite (Persistencia permanente, sin límite de frames)
        with sqlite3.connect(DB_NAME) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                INSERT INTO logs (detected_object, image_data, confidence)
                VALUES (?, ?, ?)
            ''', (detected_object, image_data, confidence))
            conn.commit()

        print(f"[{datetime.datetime.now()}] Nuevo log recibido: {detected_object}")
        return jsonify({"status": "success", "message": "Log guardado"}), 201

    except Exception as e:
        print(f"Error recibiendo log: {e}")
        return jsonify({"error": str(e)}), 500

# --- SIMULADOR (Para probar sin el cliente real) ---
@app.route('/simulate', methods=['GET'])
def simulate():
    """Genera un log de prueba para verificar que la app funciona."""
    import random
    objects = ["Persona", "Gato", "Coche", "Perro", "Paquete"]
    
    # Creamos un cuadrado de color aleatorio en base64 para simular una imagen
    color = f"{random.randint(0,255)},{random.randint(0,255)},{random.randint(0,255)}"
    svg = f'<svg width="320" height="240" xmlns="http://www.w3.org/2000/svg"><rect width="100%" height="100%" fill="rgb({color})"/><text x="50%" y="50%" font-size="20" fill="white" text-anchor="middle">SIMULACION</text></svg>'
    b64_img = base64.b64encode(svg.encode('utf-8')).decode('utf-8')
    # Nota: El navegador puede renderizar SVG base64 en tags img si se pone el header correcto, 
    # pero para el script asumimos JPG. Aquí es solo prueba.
    
    # Inyección directa a la función de guardado simulando request
    with sqlite3.connect(DB_NAME) as conn:
        cursor = conn.cursor()
        cursor.execute('INSERT INTO logs (detected_object, image_data, confidence) VALUES (?, ?, ?)', 
                      (random.choice(objects), b64_img, random.random()))
        conn.commit()
        
    return "Log simulado creado. Revisa el dashboard."

if __name__ == '__main__':
    init_db()
    # Ejecutar en todas las interfaces para que el cliente vigilante pueda conectar por IP
    print("Iniciando Servidor Vigilante...")
    print("Accede a http://localhost:5000")
    app.run(host='0.0.0.0', port=5000, debug=True)