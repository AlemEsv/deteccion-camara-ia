// Panel personalizado para mostrar la imagen escalada
package com.proyecto.vigilante;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Panel para mostrar una imagen de detección.
 * Escala la imagen al tamaño disponible del panel.
 */
public class ImagePanel extends JPanel {

    private BufferedImage image;

    public ImagePanel() {
        setBackground(Color.DARK_GRAY);
    }

    public void setImage(BufferedImage img) {
        this.image = img;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (image != null) {
            int panelW = getWidth();
            int panelH = getHeight();

            int imgW = image.getWidth();
            int imgH = image.getHeight();

            // Escalado conservando aspecto
            double scale = Math.min(
                    (double) panelW / imgW,
                    (double) panelH / imgH
            );

            int newW = (int) (imgW * scale);
            int newH = (int) (imgH * scale);

            int x = (panelW - newW) / 2;
            int y = (panelH - newH) / 2;

            Image scaled = image.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
            g.drawImage(scaled, x, y, this);
        }
    }
}
