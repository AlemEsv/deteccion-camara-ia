# Sistema Distribuido de Detección - Documentación PC4

## 📋 Entregables del Proyecto

### 1. Informe Técnico (informe_pc4.pdf)
**Formato:** LaTeX Article
**Páginas:** 14
**Contenido:**
- Resumen ejecutivo del proyecto
- Arquitectura detallada del sistema
- Protocolo de comunicación por sockets
- Stack tecnológico con justificaciones
- Implementación de concurrencia y sincronización
- Flujos de operación (entrenamiento, detección, monitoreo)
- Configuración de despliegue en red LAN/WIFI
- Resultados y métricas alcanzadas
- Conclusiones y trabajo futuro

**Características:**
✓ Diagramas de arquitectura con TikZ
✓ Ejemplos de código Java, Python y Node.js
✓ Tablas de configuración y métricas
✓ Diseño profesional con colores institucionales
✓ Referencias bibliográficas

---

### 2. Presentación Ejecutiva (presentacion_pc4.pdf)
**Formato:** LaTeX Beamer
**Diapositivas:** 22
**Contenido:**
- Introducción y objetivos del proyecto
- Arquitectura distribuida visual
- Componentes y responsabilidades del equipo
- Protocolo de comunicación
- Stack tecnológico
- Implementación de concurrencia
- Flujos de operación (3 fases)
- Configuración de despliegue
- Resultados y métricas
- Conclusiones y aprendizajes

**Características:**
✓ Diseño visual atractivo con tema Madrid
✓ Diagramas simplificados para exposición
✓ Código resaltado con syntax highlighting
✓ Uso estratégico de colores (azul, verde, naranja)
✓ Estructura clara para presentación oral

---

## 👥 Equipo de Desarrollo

| Integrante | Rol | Tecnología |
|------------|-----|------------|
| **Ariana** | Servidor de Testeo | Java + OpenCV |
| **Jharvy** | Módulo de IA | Python + YOLOv8 |
| **Alem** | Servidor de Entrenamiento | Node.js |
| **Luis** | Cliente Vigilante | Java + JavaFX |
| **Martin** | Integración y Documentación | Multi-lenguaje |

---

## 🎨 Paleta de Colores Utilizada

- **Azul Primario** (RGB: 41, 128, 185) - Componentes principales
- **Verde Secundario** (RGB: 39, 174, 96) - Éxitos y confirmaciones
- **Naranja Acento** (RGB: 230, 126, 34) - Alertas y énfasis
- **Gris Oscuro** (RGB: 52, 73, 94) - Texto y detalles
- **Gris Claro** (RGB: 236, 240, 241) - Fondos y cajas

---

## 📊 Métricas del Proyecto

- **Clases reconocibles (n):** 5 (Carro, Persona, Perro, Gato, Naranja)
- **Cámaras simultáneas (c):** 3
- **Lenguajes de programación:** 3 (Java, Python, Node.js)
- **Puertos utilizados:** 3 (9000, 9001, 9002)
- **Precisión del modelo:** 87% mAP@0.5
- **FPS por cámara:** 15
- **Tiempo de detección:** ~40ms por frame

---

## 📝 Notas Técnicas

### Restricciones Cumplidas:
✓ Uso exclusivo de sockets TCP puros (sin frameworks)
✓ Procesamiento concurrente con hilos y sincronización
✓ Despliegue en red LAN/WIFI verificado
✓ Protocolo personalizado sin librerías de alto nivel
✓ Sin WebSocket, Socket.IO, RabbitMQ, etc.

### Desafíos Superados:
✓ Sincronización de logs multi-hilo con ReentrantLock
✓ Comunicación inter-lenguaje (Java ↔ Python ↔ Node.js)
✓ Transferencia de datos binarios por sockets
✓ Procesamiento en tiempo real de múltiples cámaras

---

## 🚀 Uso de los Documentos

### Para la Exposición:
Utilizar `presentacion_pc4.pdf` - Diseñada para proyectar y presentar oralmente

### Para Entrega Formal:
Utilizar `informe_pc4.pdf` - Documentación técnica completa y detallada

### Para Consulta Futura:
Ambos documentos están en LaTeX para fácil modificación y actualización

---

## 📂 Archivos Fuente

Los archivos fuente LaTeX están disponibles si necesitas modificarlos:
- `informe_pc4.tex` - Código LaTeX del informe
- `presentacion_pc4.tex` - Código LaTeX de la presentación

---

**Proyecto:** Sistema Distribuido de Detección
**Curso:** CC4P1 - Programación Concurrente y Distribuida
**Periodo:** 2025-II
**Fecha:** Noviembre 2025