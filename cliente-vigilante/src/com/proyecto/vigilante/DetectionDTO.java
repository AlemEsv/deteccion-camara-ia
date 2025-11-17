package com.proyecto.vigilante;

/**
 * DTO que representa una detección realizada por el sistema distribuido.
 * Contiene la cámara que detectó, el objeto reconocido, la fecha/hora
 * y el nombre de archivo de la imagen asociada.
 */
public class DetectionDTO {
    private final String camara;
    private final String objeto;
    private final String fecha;
    private final String imagen;

    public DetectionDTO(String camara, String objeto, String fecha, String imagen) {
        this.camara = camara;
        this.objeto = objeto;
        this.fecha = fecha;
        this.imagen = imagen;
    }

    public String getCamara() {
        return camara;
    }

    public String getObjeto() {
        return objeto;
    }

    public String getFecha() {
        return fecha;
    }

    public String getImagen() {
        return imagen;
    }
}