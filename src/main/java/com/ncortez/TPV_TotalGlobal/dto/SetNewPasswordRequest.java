package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO que encapsula los datos para establecer una nueva contraseña.
 * Se utiliza en el endpoint POST /api/auth/set-new-password después de restauración.
 */
public class SetNewPasswordRequest {
    /** Nombre de usuario cuya contraseña se actualiza */
    private String username;
    /** Nueva contraseña a establecer */
    private String newPassword;
    
    /**
     * Constructor vacío para serialización JSON.
     */
    public SetNewPasswordRequest() {}
    
    /**
     * Constructor que inicializa username y nueva contraseña.
     * @param username Nombre de usuario
     * @param newPassword Nueva contraseña
     */
    public SetNewPasswordRequest(String username, String newPassword) {
        this.username = username;
        this.newPassword = newPassword;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}