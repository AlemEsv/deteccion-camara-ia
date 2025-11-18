// Modelo de tabla para conectar List<DetectionDTO> y JTable
package com.proyecto.vigilante;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * TableModel para mostrar la lista de detecciones en un JTable.
 * Columnas: Objeto | Cámara | Fecha hora | Imagen
 */
public class DetectionTableModel extends AbstractTableModel {

    private final String[] columns = {"Objeto", "Cámara", "Fecha hora", "Imagen"};
    private List<DetectionDTO> data = new ArrayList<>();

    public void setDetections(List<DetectionDTO> detections) {
        this.data = detections != null ? detections : new ArrayList<>();
        fireTableDataChanged();
    }

    public DetectionDTO getDetectionAt(int row) {
        if (row < 0 || row >= data.size()) return null;
        return data.get(row);
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DetectionDTO d = data.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> d.getObjeto();
            case 1 -> d.getCamara();
            case 2 -> d.getFecha();
            case 3 -> d.getImagen();
            default -> "";
        };
    }
}