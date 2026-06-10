package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO que encapsula la respuesta de autenticación exitosa.
 * Se retorna en el endpoint POST /api/auth/login con código 200.
 */
public class LoginResponse {
    /** Rol del usuario autenticado (ADMIN o COMMON_USER) */
    private String role;
    /** Token JWT de sesión para autorizar llamadas posteriores. */
    private String token;
    /** Username autenticado, útil para trazabilidad en front. */
    private String username;

    /**
     * Constructor que inicializa la respuesta con el rol del usuario.
     * @param role Rol del usuario autenticado
     */
    public LoginResponse(String role, String token, String username) {
        this.role = role;
        this.token = token;
        this.username = username;
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

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}