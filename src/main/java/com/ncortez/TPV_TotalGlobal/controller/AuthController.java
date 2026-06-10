package com.ncortez.TPV_TotalGlobal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ncortez.TPV_TotalGlobal.dto.ApiMessageResponse;
import com.ncortez.TPV_TotalGlobal.dto.LoginRequest;
import com.ncortez.TPV_TotalGlobal.dto.LoginResponse;
import com.ncortez.TPV_TotalGlobal.dto.RestorePasswordRequest;
import com.ncortez.TPV_TotalGlobal.dto.SetNewPasswordRequest;
import com.ncortez.TPV_TotalGlobal.dto.SetupSecurityQuestionsRequest;
import com.ncortez.TPV_TotalGlobal.security.JwtService;
import com.ncortez.TPV_TotalGlobal.service.AuthService;

/**
 * Controlador REST encargado de gestionar los procesos de autenticación,
 * recuperación de contraseñas y configuración de seguridad para TPV TotalGlobal.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    /**
     * Autentica un usuario en el sistema.
     * 
     * Retornos específicos:
     * 428: FIRST_LOGIN - Debe establecer contraseña y preguntas.
     * 423: Cuenta bloqueada.
     * 401: Credenciales inválidas.
     *
     * @param request Datos de acceso (username y password).
     * @return Respuesta con el rol del usuario o error detallado.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String role = authService.authenticate(request.getUsername(), request.getPassword());
            String username = request.getUsername().trim().toLowerCase();
            // Tras validar credenciales, emitimos JWT firmado con username + role.
            // Este token será la única fuente de identidad/autorización en requests protegidas.
            String token = jwtService.generateToken(username, role);
            return ResponseEntity.ok(new LoginResponse(role, token, username));
        } catch (RuntimeException ex) {
            String message = ex.getMessage();

            if ("FIRST_LOGIN".equals(message)) {
                return ResponseEntity.status(428).body(new ApiMessageResponse("FIRST_LOGIN"));
            }

            if (message != null && message.toLowerCase().contains("bloqueada")) {
                return ResponseEntity.status(423).body(new ApiMessageResponse(message));
            }

            if ("Usuario o contraseña inválidos".equals(message)) {
                return ResponseEntity.status(401).body(new ApiMessageResponse(message));
            }

            return ResponseEntity.badRequest().body(new ApiMessageResponse(message));
        }
    }

    /**
     * Verifica si las respuestas a las preguntas de seguridad son correctas.
     * 
     * @param request Username y respuestas a validar.
     * @return 200 OK si coinciden, 400 Bad Request si fallan.
     */
    @PostMapping("/restore-password")
    public ResponseEntity<?> restore(@RequestBody RestorePasswordRequest request) {
        String result = authService.verifySecurityQuestions(request);
            
        if ("OK".equals(result)) {
            return ResponseEntity.ok(new ApiMessageResponse("Preguntas verificadas correctamente"));
        }

        return ResponseEntity.badRequest().body(new ApiMessageResponse(result));
    }

    /**
     * Establece una nueva contraseña tras la recuperación o desbloqueo.
     * 
     * @param request Username y la nueva contraseña.
     * @return Mensaje de éxito o error en la actualización.
     */
    @PostMapping("/set-new-password")
    public ResponseEntity<?> setNewPassword(@RequestBody SetNewPasswordRequest request) {
        String result = authService.setNewPassword(request.getUsername(), request.getNewPassword());

        if ("OK".equals(result)) {
            return ResponseEntity.ok(new ApiMessageResponse("Contraseña actualizada correctamente"));
        }

        return ResponseEntity.badRequest().body(new ApiMessageResponse(result));
    }

    /**
     * Registra las preguntas de seguridad por primera vez para completar el alta.
     * 
     * @param request Username y respuestas seleccionadas.
     * @return 200 OK o error de configuración.
     */
    @PostMapping("/setup-security-questions")
    public ResponseEntity<?> setupSecurityQuestions(@RequestBody SetupSecurityQuestionsRequest request) {        
        String result = authService.setupSecurityQuestions(request);

        if ("OK".equals(result)) {
            return ResponseEntity.ok(new ApiMessageResponse("Preguntas de seguridad configuradas correctamente"));
        }

        return ResponseEntity.badRequest().body(new ApiMessageResponse(result));
    }
}