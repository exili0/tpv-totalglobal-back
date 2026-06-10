package com.ncortez.TPV_TotalGlobal.service;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ncortez.TPV_TotalGlobal.dto.RestorePasswordRequest;
import com.ncortez.TPV_TotalGlobal.dto.SetupSecurityQuestionsRequest;
import com.ncortez.TPV_TotalGlobal.entity.SecurityAnswer;
import com.ncortez.TPV_TotalGlobal.entity.UserEntity;
import com.ncortez.TPV_TotalGlobal.repository.SecurityAnswerRepository;
import com.ncortez.TPV_TotalGlobal.repository.UserRepository;

/**
 * Servicio de lógica de negocio para la autenticación y seguridad del TPV.
 * Gestiona login, recuperación de contraseña, preguntas de seguridad y bloqueos de cuenta.
 */
@Service
public class AuthService {

    @Autowired 
    private UserRepository userRepository;

    @Autowired
    private SecurityAnswerRepository securityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Valida credenciales, gestiona bloqueos por intentos fallidos y detecta primer acceso.
     * Una cuenta se bloquea automáticamente después de 4 intentos fallidos.
     * @param username Nombre de usuario
     * @param password Contraseña
     * @return Rol del usuario si autenticación es exitosa
     * @throws RuntimeException con mensaje "FIRST_LOGIN" si es primer acceso,
     *         "Cuenta bloqueada..." si está bloqueada, o
     *         "Usuario o contraseña inválidos" si las credenciales no coinciden
     */
    public String authenticate(String username, String password) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuario o contraseña inválidos");
        }

        UserEntity user = userOpt.get();

        // 1. Verificar si la cuenta está activa
        if (!user.isActive()) {
            throw new RuntimeException("Cuenta bloqueada. Contacta con un administrador");
        }

        // 2. Detectar primer acceso (sin contraseña definida)
        if (user.isFirstLogin()) {
            throw new RuntimeException("FIRST_LOGIN");
        }

        // 3. Validar contraseña
        if (!isPasswordValid(user, password)) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);

            if (user.getFailedAttempts() >= 4) {
                user.setActive(false);
                user.setLockedDate(new Date());
                userRepository.save(user);
                throw new RuntimeException("Cuenta bloqueada por exceso de intentos fallidos");
            }

            userRepository.save(user);
            throw new RuntimeException("Usuario o contraseña inválidos");
        }

        // 4. Login exitoso: resetear intentos
        user.setFailedAttempts(0);
        userRepository.save(user);
        return user.getRole().name();
    }

    /**
     * Configura las preguntas de seguridad en el primer acceso y marca el usuario como inicializado.
     * @param req Request con username y respuestas a las preguntas de seguridad
     * @return "OK" si se configuró correctamente, mensaje de error en caso contrario
     */
    public String setupSecurityQuestions(SetupSecurityQuestionsRequest req) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(req.getUsername());

        if (userOpt.isEmpty()) return "Usuario no encontrado";

        UserEntity user = userOpt.get();

        Optional<SecurityAnswer> existing = securityRepository.findByUser(user);
        if (existing.isPresent()) {
            return "Este usuario ya tiene preguntas de seguridad configuradas";
        }

        SecurityAnswer newAnswers = new SecurityAnswer(
            req.getFirstAnswer().trim(), 
            req.getSecondAnswer().trim(), 
            user
        );
        
        user.setSecurityAnswer(newAnswers);
        securityRepository.save(newAnswers);

        if (user.isFirstLogin()) {
            user.setFirstLogin(false);
            userRepository.save(user);
        }

        return "OK";
    }

    /**
     * Verifica si las respuestas de seguridad coinciden para autorizar la recuperación de contraseña.
     * @param request Request con username y respuestas a verificar
     * @return "OK" si las respuestas coinciden, mensaje de error en caso contrario
     */
    public String verifySecurityQuestions(RestorePasswordRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty()) return "Usuario no encontrado";

        UserEntity user = userOpt.get();

        if (!user.isActive()) {
            throw new RuntimeException("Cuenta bloqueada. Contacta con un administrador");
        }

        Optional<SecurityAnswer> answersOpt = securityRepository.findByUser(user);
        if (answersOpt.isEmpty()) {
            return "El usuario no tiene preguntas configuradas";
        }

        SecurityAnswer answers = answersOpt.get();

        boolean firstMatch = answers.getFirstAnswer().trim().equalsIgnoreCase(request.getFirstAnswer().trim());
        boolean secondMatch = answers.getSecondAnswer().trim().equalsIgnoreCase(request.getSecondAnswer().trim());

        return (firstMatch && secondMatch) ? "OK" : "Las respuestas no coinciden";
    }

    /**
     * Establece una nueva contraseña tras recuperación o en el primer login.
     * @param username Nombre de usuario
     * @param newPassword Nueva contraseña a establecer
     * @return "OK" si se estableció correctamente, mensaje de error en caso contrario
     */
    public String setNewPassword(String username, String newPassword) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) return "Usuario no encontrado";

        UserEntity user = userOpt.get();

        if (!user.isFirstLogin() && !user.isActive()) {
            throw new RuntimeException("Cuenta bloqueada");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return "La contraseña no puede estar vacía";
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setFailedAttempts(0);
        
        if (user.isFirstLogin()) {
            user.setActive(true);
        }

        userRepository.save(user);
        return "OK";
    }

    /**
     * Valida contraseña soportando compatibilidad temporal con registros legacy en texto plano.
     * Si detecta coincidencia legacy, migra automáticamente a hash BCrypt.
     */
    private boolean isPasswordValid(UserEntity user, String rawPassword) {
        if (rawPassword == null || user.getPassword() == null) {
            return false;
        }

        boolean matchesHashed;
        try {
            matchesHashed = passwordEncoder.matches(rawPassword, user.getPassword());
        } catch (IllegalArgumentException ex) {
            matchesHashed = false;
        }

        if (matchesHashed) {
            return true;
        }

        if (rawPassword.equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            return true;
        }

        return false;
    }
}