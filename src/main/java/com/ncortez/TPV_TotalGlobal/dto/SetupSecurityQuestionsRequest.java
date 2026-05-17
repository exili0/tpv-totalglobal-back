package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO que encapsula los datos para configurar las preguntas de seguridad en el primer acceso.
 * Se utiliza en el endpoint POST /api/auth/setup-security-questions.
 */
public class SetupSecurityQuestionsRequest {
    /** Nombre de usuario que configura sus preguntas de seguridad */
    private String username;
    /** Respuesta a la primera pregunta de seguridad */
    private String firstAnswer;
    /** Respuesta a la segunda pregunta de seguridad */
    private String secondAnswer;
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFirstAnswer() { return firstAnswer; }
    public void setFirstAnswer(String firstAnswer) { this.firstAnswer = firstAnswer; }
    
    public String getSecondAnswer() { return secondAnswer; }
    public void setSecondAnswer(String secondAnswer) { this.secondAnswer = secondAnswer; }
}