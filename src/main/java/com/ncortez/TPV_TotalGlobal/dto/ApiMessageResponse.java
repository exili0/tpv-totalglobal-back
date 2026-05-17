package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO de respuesta genérica para mensajes del API.
 * Se utiliza para comunicar mensajes de error, éxito o información adicional.
 */
public class ApiMessageResponse {
    /** Mensaje de respuesta */
    private String message;

    public ApiMessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}