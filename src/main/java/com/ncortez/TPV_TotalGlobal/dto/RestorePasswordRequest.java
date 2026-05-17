package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO que encapsula los datos para restaurar contraseña mediante preguntas de seguridad.
 * Se utiliza en el endpoint POST /api/auth/restore-password.
 */
public class RestorePasswordRequest {
    /** Nombre de usuario del que se restaura la contraseña */
    private String username;
    /** Respuesta a la primera pregunta de seguridad */
    private String firstAnswer;
    /** Respuesta a la segunda pregunta de seguridad */
    private String secondAnswer;
    
    /**
     * Constructor vacío para serialización JSON.
     */
    public RestorePasswordRequest() {}
    
    /**
     * Constructor que inicializa todas las respuestas de seguridad.
     * @param username Nombre de usuario
     * @param firstAnswer Respuesta a la primera pregunta
     * @param secondAnswer Respuesta a la segunda pregunta
     */
    public RestorePasswordRequest(String username, String firstAnswer, String secondAnswer) {
        this.username = username;
        this.firstAnswer = firstAnswer;
        this.secondAnswer = secondAnswer;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getFirstAnswer() {
        return firstAnswer;
    }
    
    public void setFirstAnswer(String firstAnswer) {
        this.firstAnswer = firstAnswer;
    }
    
    public String getSecondAnswer() {
        return secondAnswer;
    }
    
    public void setSecondAnswer(String secondAnswer) {
        this.secondAnswer = secondAnswer;
    }
}