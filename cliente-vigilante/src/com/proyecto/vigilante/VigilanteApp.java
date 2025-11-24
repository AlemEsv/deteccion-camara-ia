// App Swing principal
package com.proyecto.vigilante;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Aplicación Swing del Cliente Vigilante.
 * Muestra una tabla con detecciones y la imagen asociada al registro seleccionado.
 */
public class VigilanteApp extends JFrame {

    private final VigilanteClient client;
    private final DetectionTableModel tableModel;
    private final JTable table;
    private final ImagePanel imagePanel;
    private final JLabel statusLabel;

    public VigilanteApp(String host, int logPort, int imgPort) {
        super("Cliente-Vigilante-App");

        this.client = new VigilanteClient(host, logPort, imgPort);
        this.tableModel = new DetectionTableModel();
        this.table = new JTable(tableModel);
        this.imagePanel = new ImagePanel();
        this.statusLabel = new JLabel(" ", SwingConstants.CENTER);

        configureWindow();
        initUI();
        loadLogs(); // carga inicial
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Panel izquierdo: tabla + controles
        JPanel leftPanel = new JPanel(new BorderLayout());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        leftPanel.add(scroll, BorderLayout.CENTER);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh = new JButton("Refrescar");
        btnRefresh.addActionListener(e -> loadLogs());
        controlsPanel.add(btnRefresh);

        leftPanel.add(controlsPanel, BorderLayout.NORTH);

        // Panel derecho: imagen + estado
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(imagePanel, BorderLayout.CENTER);
        rightPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        // Listener para selección de fila en tabla
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    DetectionDTO det = tableModel.getDetectionAt(row);
                    if (det != null) {
                        loadImage(det);
                    }
                }
            }
        });

        getContentPane().add(mainPanel);
    }

    /** Carga logs del servidor usando SwingWorker para no bloquear la UI. */
    private void loadLogs() {
        statusLabel.setText("Cargando registros desde el servidor...");
        imagePanel.setImage(null);

        SwingWorker<List<DetectionDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DetectionDTO> doInBackground() throws Exception {
                return client.fetchLogs();
            }

            @Override
            protected void done() {
                try {
                    List<DetectionDTO> logs = get();
                    tableModel.setDetections(logs);
                    statusLabel.setText("Registros cargados: " + logs.size());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            VigilanteApp.this,
                            "Error al cargar logs: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText("Error al cargar logs.");
                }
            }
        };
        worker.execute();
    }

    /** Carga la imagen asociada a una detección seleccionada. */
    private void loadImage(DetectionDTO det) {
        statusLabel.setText("Cargando imagen " + det.getImagen() + "...");
        imagePanel.setImage(null);

        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return client.fetchImage(det.getImagen());
            }

            @Override
            protected void done() {
                try {
                    BufferedImage img = get();
                    if (img != null) {
                        imagePanel.setImage(img);
                        statusLabel.setText(String.format(
                                "Cámara: %s | Objeto: %s | Fecha: %s | Imagen: %s",
                                det.getCamara(), det.getObjeto(), det.getFecha(), det.getImagen()
                        ));
                    } else {
                        statusLabel.setText("No se pudo decodificar la imagen.");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                            VigilanteApp.this,
                            "Error al cargar imagen: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    statusLabel.setText("Error al cargar imagen.");
                }
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        // Configuración por defecto
        String host = "127.0.0.1";
        int logPort = 9001;
        int imgPort = 9002;

        // Aceptar argumentos de línea de comandos
        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            logPort = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            imgPort = Integer.parseInt(args[2]);
        }

        final String finalHost = host;
        final int finalLogPort = logPort;
        final int finalImgPort = imgPort;

        System.out.println("=".repeat(50));
        System.out.println("CLIENTE VIGILANTE - CC4P1");
        System.out.println("=".repeat(50));
        System.out.println("Conectando a:");
        System.out.println("  Host: " + finalHost);
        System.out.println("  Puerto Logs: " + finalLogPort);
        System.out.println("  Puerto Imágenes: " + finalImgPort);
        System.out.println("=".repeat(50));

        SwingUtilities.invokeLater(() -> {
            VigilanteApp app = new VigilanteApp(finalHost, finalLogPort, finalImgPort);
            app.setVisible(true);
        });
    }
}
