package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO que encapsula los datos de autenticación del usuario.
 * Se utiliza en el endpoint POST /api/auth/login.
 */
public class LoginRequest {
    /** Nombre de usuario único en el sistema */
    private String username;
    /** Contraseña asociada al usuario */
    private String password;

    /**
     * Obtiene el nombre de usuario.
     * @return Nombre de usuario
     */
    public String getUsername() { return username; }
    /**
     * Establece el nombre de usuario.
     * @param username Nombre de usuario
     */
    public void setUsername(String username) { this.username = username; }
    /**
     * Obtiene la contraseña del usuario.
     * @return Contraseña
     */
    public String getPassword() { return password; }
    /**
     * Establece la contraseña del usuario.
     * @param password Contraseña
     */
    public void setPassword(String password) { this.password = password; }
}