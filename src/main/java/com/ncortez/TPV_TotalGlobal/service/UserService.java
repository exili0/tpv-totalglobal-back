package com.ncortez.TPV_TotalGlobal.service;

import com.ncortez.TPV_TotalGlobal.entity.SecurityAnswer;
import com.ncortez.TPV_TotalGlobal.entity.UserEntity;
import com.ncortez.TPV_TotalGlobal.entity.enums.Role;
import com.ncortez.TPV_TotalGlobal.repository.SecurityAnswerRepository;
import com.ncortez.TPV_TotalGlobal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de lógica de negocio para la gestión de usuarios.
 * Encapsula las operaciones CRUD de usuarios y manejo de roles.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAnswerRepository securityAnswerRepository;

    /**
     * Obtiene todos los usuarios del sistema.
     * @return Lista de todos los usuarios existentes
     */
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Obtiene un usuario por su identificador único.
     * @param id Identificador del usuario a buscar
     * @return Optional con el usuario si existe
     */
    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Crea un nuevo usuario en el sistema con invitación (sin contraseña inicial).
     * El usuario entrará en modo primer acceso (firstLogin = true) hasta configurar su contraseña.
     * @param username Nombre de usuario único
     * @param name Nombre del usuario
     * @param lastname Apellido del usuario
     * @param email Correo electrónico del usuario
     * @param roleStr Rol del usuario ("ADMIN" o "COMMON_USER")
     * @return Usuario creado
     * @throws RuntimeException Si el nombre de usuario ya existe
     */
    public UserEntity createUser(String username, String name, String lastname, String email, String roleStr) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("El usuario ya existe");
        }

        Role role = Role.valueOf(roleStr.toUpperCase());
        UserEntity newUser = new UserEntity(username, name, lastname, email, role);
        return userRepository.save(newUser);
    }

    /**
     * Elimina un usuario del sistema.
     * @param id Identificador del usuario a eliminar
     * @throws RuntimeException Si el usuario no existe
     */
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Optional<SecurityAnswer> answers = securityAnswerRepository.findByUser(user);
        answers.ifPresent(securityAnswerRepository::delete);

        user.setSecurityAnswer(null);
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        userRepository.deleteById(id);
    }

    /**
     * Actualiza los datos principales de un usuario.
     */
    public UserEntity updateUser(
        Long id,
        String username,
        String name,
        String lastname,
        String email,
        String roleStr,
        Date dateCreated
    ) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (username != null && !username.trim().isEmpty()) {
            String normalized = username.trim();
            Optional<UserEntity> existingByUsername = userRepository.findByUsername(normalized);
            if (existingByUsername.isPresent() && !existingByUsername.get().getId().equals(id)) {
                throw new RuntimeException("El nombre de usuario ya está en uso");
            }
            user.setUsername(normalized);
        }

        if (name != null) {
            user.setName(name.trim());
        }

        if (lastname != null) {
            user.setLastname(lastname.trim());
        }

        if (email != null) {
            user.setEmail(email.trim());
        }

        if (roleStr != null && !roleStr.trim().isEmpty()) {
            user.setRole(Role.valueOf(roleStr.trim().toUpperCase()));
        }

        if (dateCreated != null) {
            user.setDateCreated(dateCreated);
        }

        return userRepository.save(user);
    }

    /**
     * Activa o desactiva un usuario.
     */
    public UserEntity setUserActive(Long id, boolean active) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setActive(active);
        if (active) {
            user.setLockedDate(null);
            user.setFailedAttempts(0);
        }

        return userRepository.save(user);
    }

    /**
     * Reinicia el usuario al flujo de primer login.
     */
    public UserEntity resetToFirstLogin(Long id) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Optional<SecurityAnswer> answers = securityAnswerRepository.findByUser(user);
        answers.ifPresent(securityAnswerRepository::delete);

        user.setSecurityAnswer(null);
        user.setPassword(null);
        user.setFirstLogin(true);
        user.setActive(true);
        user.setFailedAttempts(0);
        user.setLockedDate(null);

        return userRepository.save(user);
    }
}