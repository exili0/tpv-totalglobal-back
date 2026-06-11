package com.ncortez.TPV_TotalGlobal.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ncortez.TPV_TotalGlobal.entity.UserEntity;
import com.ncortez.TPV_TotalGlobal.service.UserService;

/**
 * Controlador REST para la gestión de usuarios del sistema.
 * Proporciona operaciones CRUD sobre usuarios con validación de roles.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Obtiene la lista completa de usuarios del sistema.
     * Operación accesible solo por administradores.
     * @return Lista de todos los usuarios existentes
     */
    @GetMapping
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Crea un nuevo usuario en el sistema mediante invitación.
     * El usuario creado no tendrá contraseña inicial y deberá completar el proceso de primer acceso.
     * @param request DTO con los datos del usuario a crear (username, name, lastname, email, role)
     * @return Usuario creado o mensaje de error si falla
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            UserEntity newUser = userService.createUser(
                request.getUsername(), 
                request.getName(), 
                request.getLastname(),
                request.getEmail(), 
                request.getRole()
            );
            return ResponseEntity.ok(newUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un usuario del sistema.
     * @param id Identificador del usuario a eliminar
     * @return Mensaje de éxito o error
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Actualiza los datos principales de un usuario.
     * @param id Identificador del usuario
     * @param request DTO con campos editables
     * @return Usuario actualizado o mensaje de error
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        try {
            Date parsedDate = parseDateOrNull(request.getDate());
            UserEntity updated = userService.updateUser(
                id,
                request.getUsername(),
                request.getName(),
                request.getLastname(),
                request.getEmail(),
                request.getRole(),
                parsedDate
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Activa o desactiva una cuenta de usuario.
     */
    @PatchMapping("/{id}/active")
    public ResponseEntity<?> updateUserActive(@PathVariable Long id, @RequestBody ActiveRequest request) {
        try {
            UserEntity updated = userService.setUserActive(id, request.isActive());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Reinicia el usuario al flujo de primer acceso.
     */
    @PatchMapping("/{id}/first-login")
    public ResponseEntity<?> resetUserToFirstLogin(@PathVariable Long id) {
        try {
            UserEntity updated = userService.resetToFirstLogin(id);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Convierte una fecha en formato ISO YYYY-MM-DD a java.util.Date.
     * Devuelve null si la entrada viene vacía.
     */
    private Date parseDateOrNull(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }

        try {
            LocalDate localDate = LocalDate.parse(rawDate.trim());
            return java.sql.Date.valueOf(localDate);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Formato de fecha inválido. Usa YYYY-MM-DD");
        }
    }

    /**
     * DTO que encapsula los datos necesarios para crear un nuevo usuario.
     */
    public static class CreateUserRequest {
        /** Nombre de usuario único en el sistema */
        private String username;
        /** Nombre del usuario */
        private String name;
        /** Apellido del usuario */
        private String lastname;
        /** Correo electrónico del usuario */
        private String email;
        /** Rol del usuario ("ADMIN" o "COMMON_USER") */
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLastname() { return lastname; }
        public void setLastname(String lastname) { this.lastname = lastname; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    /**
     * DTO para actualizar un usuario existente.
     */
    public static class UpdateUserRequest {
        private String username;
        private String name;
        private String lastname;
        private String email;
        private String role;
        private String date;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLastname() { return lastname; }
        public void setLastname(String lastname) { this.lastname = lastname; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
    }

    /**
     * DTO para cambio de estado activo de usuario.
     */
    public static class ActiveRequest {
        private boolean active;

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}