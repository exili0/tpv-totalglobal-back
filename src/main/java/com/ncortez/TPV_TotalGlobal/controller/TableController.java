package com.ncortez.TPV_TotalGlobal.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ncortez.TPV_TotalGlobal.dto.TableRequest;
import com.ncortez.TPV_TotalGlobal.entity.BusinessTable;
import com.ncortez.TPV_TotalGlobal.service.PosOperationsService;

/**
 * API REST para la gestión de mesas del negocio.
 * Incluye operaciones de consulta, creación y control de bloqueo de mesas por operador.
 */
@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "http://localhost:4200")
public class TableController {

    @Autowired
    private PosOperationsService posOperationsService;

    /**
     * Devuelve todas las mesas activas ordenadas por número.
     *
     * @return Lista de mesas operativas del negocio
     */
    @GetMapping
    public ResponseEntity<List<BusinessTable>> getActiveTables() {
        return ResponseEntity.ok(posOperationsService.getActiveTables());
    }

    /**
     * Crea una nueva mesa en el sistema.
     * El número 0 se reserva automáticamente para la barra.
     *
     * @param request DTO con número, nombre y capacidad de la nueva mesa
     * @return Mesa creada o error 400 si el número ya existe
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTable(@RequestBody TableRequest request) {
        try {
            return ResponseEntity.ok(posOperationsService.createTable(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Desactiva una mesa para que deje de mostrarse en operación.
     * Operación reservada a perfil administrador.
     */
    @DeleteMapping("/{tableNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTable(@PathVariable Integer tableNumber) {
        try {
            return ResponseEntity.ok(posOperationsService.deleteTable(tableNumber));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Bloquea una mesa para el operador indicado.
     * Si la mesa ya está tomada por otra sesión, devuelve error 400 a menos que sea admin.
     *
     * @param tableNumber  Número de la mesa
     * @param username     Nombre del operador que toma la mesa
     * @param sessionToken Token único de sesión del cliente Angular
     * @param role         Rol del usuario (ADMIN puede hacer override del bloqueo)
     * @return Mesa con bloqueo actualizado o error 400
     */
    @PostMapping("/{tableNumber}/claim")
    public ResponseEntity<?> claimTable(
            @PathVariable Integer tableNumber,
            @RequestParam("sessionToken") String sessionToken,
            Authentication authentication) {
        try {
            // No confiamos en username/role del cliente: tomamos identidad desde JWT.
            String username = authentication.getName();
            String role = extractRole(authentication);
            return ResponseEntity.ok(posOperationsService.claimTable(tableNumber, username, sessionToken, role));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Libera el bloqueo de una mesa y la vuelve al estado FREE.
     * Solo puede liberarla el mismo operador que la tomó o un administrador.
     *
     * @param tableNumber  Número de la mesa a liberar
     * @param username     Nombre del operador
     * @param sessionToken Token de sesión del cliente
     * @param role         Rol del usuario
     * @return Mesa liberada o error 400 si no tiene permiso
     */
    @PostMapping("/{tableNumber}/release")
    public ResponseEntity<?> releaseTable(
            @PathVariable Integer tableNumber,
            @RequestParam("sessionToken") String sessionToken,
            Authentication authentication) {
        try {
            // Misma regla en release: identidad autorizada solo por token.
            String username = authentication.getName();
            String role = extractRole(authentication);
            return ResponseEntity.ok(posOperationsService.releaseTable(tableNumber, username, sessionToken, role));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Extrae el rol efectivo desde la autenticación para aplicar reglas de acceso.
     */
    private String extractRole(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return null;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                return value.substring("ROLE_".length());
            }
        }

        return null;
    }
}
