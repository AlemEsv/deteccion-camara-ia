// Capa de red: sockets hacia servidor de logs e imágenes 
package com.proyecto.vigilante;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Cliente de red que se comunica con:
 *  - Servidor de logs (puerto logPort)     → comando: GET_LOGS\n
 *  - Servidor de imágenes (puerto imgPort)→ comando: GET_IMAGE:<nombre>\n
 *
 * Este módulo abstrae el uso de sockets para la capa de presentación.
 */
public class VigilanteClient {

    private final String host;
    private final int logPort;
    private final int imgPort;

    public VigilanteClient(String host, int logPort, int imgPort) {
        this.host = host;
        this.logPort = logPort;
        this.imgPort = imgPort;
    }

    /**
     * Pide los últimos logs al servidor de logs.
     * Protocolo:
     *  Cliente: "GET_LOGS\n"
     *  Servidor: una línea JSON con un array de detecciones.
     */
    public List<DetectionDTO> fetchLogs() throws IOException {
        try (Socket s = new Socket(host, logPort);
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
             BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

            bw.write("GET_LOGS\n");
            bw.flush();

            String jsonLine = br.readLine();
            if (jsonLine == null || jsonLine.trim().isEmpty()) {
                return new ArrayList<>();
            }

            return parseJsonArray(jsonLine.trim());
        }
    }

    /**
     * Pide una imagen al servidor de imágenes.
     * Protocolo:
     *  Cliente: "GET_IMAGE:<nombre>\n"
     *  Servidor: "FILESIZE:n\n" + n bytes de la imagen
     */
    public BufferedImage fetchImage(String imageName) throws IOException {
        try (Socket s = new Socket(host, imgPort)) {
            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();

            // Enviar comando
            String cmd = "GET_IMAGE:" + imageName + "\n";
            out.write(cmd.getBytes("UTF-8"));
            out.flush();

            // Leer cabecera FILESIZE:n\n desde InputStream (no usar BufferedReader
            // para no mezclar buffer de texto y binario)
            StringBuilder headerBuilder = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\n') {
                    break;
                }
                headerBuilder.append((char) b);
            }
            String header = headerBuilder.toString().trim();

            if (!header.startsWith("FILESIZE:")) {
                throw new IOException("Cabecera inválida recibida: " + header);
            }

            int size = Integer.parseInt(header.substring("FILESIZE:".length()));
            byte[] bytes = new byte[size];

            int totalRead = 0;
            while (totalRead < size) {
                int r = in.read(bytes, totalRead, size - totalRead);
                if (r == -1) {
                    throw new EOFException("Fin de stream antes de leer toda la imagen");
                }
                totalRead += r;
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            return ImageIO.read(bais);
        }
    }

    /**
     * Parser simple para el JSON esperado:
     * [
     *   {"camara":"CAM1","objeto":"Persona","fecha":"2025-11-16 14:00","imagen":"cam_CAM1_001.jpg"},
     *   ...
     * ]
     *
     * No usa librerías externas, solo parsing manual de strings.
     */
    private List<DetectionDTO> parseJsonArray(String json) {
        List<DetectionDTO> list = new ArrayList<>();

        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            return list;
        }

        String inner = json.substring(1, json.length() - 1).trim();
        if (inner.isEmpty()) {
            return list;
        }

        // Separar cada objeto: tolerar "},{", "}, {", "},\n{", etc.
        String[] objects = inner
                .replace("}, {", "}@@@{")
                .replace("},\n{", "}@@@{")
                .replace("},{", "}@@@{")
                .split("@@@");

        for (String obj : objects) {
            obj = obj.trim();
            if (obj.startsWith("{")) obj = obj.substring(1);
            if (obj.endsWith("}")) obj = obj.substring(0, obj.length() - 1);

            String camara = "";
            String objeto = "";
            String fecha  = "";
            String imagen = "";

            String[] fields = obj.split(",");
            for (String field : fields) {
                String[] kv = field.split(":", 2);
                if (kv.length != 2) continue;
                String key = stripQuotes(kv[0].trim());
                String value = stripQuotes(kv[1].trim());

                switch (key) {
                    case "camara":
                        camara = value;
                        break;
                    case "objeto":
                        objeto = value;
                        break;
                    case "fecha":
                        fecha = value;
                        break;
                    case "imagen":
                        imagen = value;
                        break;
                    default:
                        break;
                }
            }

            if (!camara.isEmpty() || !objeto.isEmpty() || !fecha.isEmpty() || !imagen.isEmpty()) {
                list.add(new DetectionDTO(camara, objeto, fecha, imagen));
            }
        }

        return list;
    }

    private String stripQuotes(String s) {
        s = s.trim();
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
        return s;
    }
}