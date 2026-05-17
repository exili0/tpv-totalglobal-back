package com.ncortez.TPV_TotalGlobal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

/**
 * Representa las respuestas a las preguntas de seguridad para TPV TotalGlobal.
 * Mantiene una relación OneToOne con UserEntity.
 */
@Entity
@Table(name = "security_answers")
public class SecurityAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstAnswer;
    private String secondAnswer;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties("securityAnswer") 
    private UserEntity user; // Cambiado de User a UserEntity

    public SecurityAnswer() {
    }

    public SecurityAnswer(String firstAnswer, String secondAnswer, UserEntity user) {
        this.firstAnswer = firstAnswer;
        this.secondAnswer = secondAnswer;
        this.user = user;
    }

    /**
     * Obtiene el identificador único de las respuestas de seguridad.
     * @return ID de la entidad
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador único.
     * @param id Nuevo ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene la respuesta a la primera pregunta de seguridad.
     * @return Respuesta a la primera pregunta
     */
    public String getFirstAnswer() {
        return firstAnswer;
    }

    /**
     * Establece la respuesta a la primera pregunta de seguridad.
     * @param firstAnswer Respuesta a la primera pregunta
     */
    public void setFirstAnswer(String firstAnswer) {
        this.firstAnswer = firstAnswer;
    }

    /**
     * Obtiene la respuesta a la segunda pregunta de seguridad.
     * @return Respuesta a la segunda pregunta
     */
    public String getSecondAnswer() {
        return secondAnswer;
    }

    /**
     * Establece la respuesta a la segunda pregunta de seguridad.
     * @param secondAnswer Respuesta a la segunda pregunta
     */
    public void setSecondAnswer(String secondAnswer) {
        this.secondAnswer = secondAnswer;
    }

    /**
     * Obtiene el usuario asociado a estas respuestas de seguridad.
     * @return Usuario propietario de las respuestas
     */
    public UserEntity getUser() {
        return user;
    }

    /**
     * Establece el usuario asociado a estas respuestas de seguridad.
     * @param user Usuario propietario de las respuestas
     */
    public void setUser(UserEntity user) {
        this.user = user;
    }
}