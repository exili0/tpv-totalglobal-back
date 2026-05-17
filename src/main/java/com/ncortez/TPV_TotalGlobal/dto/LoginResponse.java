package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO que encapsula la respuesta de autenticación exitosa.
 * Se retorna en el endpoint POST /api/auth/login con código 200.
 */
public class LoginResponse {
    /** Rol del usuario autenticado (ADMIN o COMMON_USER) */
    private String role;

    /**
     * Constructor que inicializa la respuesta con el rol del usuario.
     * @param role Rol del usuario autenticado
     */
    public LoginResponse(String role) {
        this.role = role;
    }

    /**
     * Obtiene el rol del usuario.
     * @return Rol (ADMIN o COMMON_USER)
     */
    public String getRole() { return role; }
    /**
     * Establece el rol del usuario.
     * @param role Rol a asignar
     */
    public void setRole(String role) { this.role = role; }
}