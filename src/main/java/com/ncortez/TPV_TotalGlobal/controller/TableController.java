package com.ncortez.TPV_TotalGlobal.controller;

import com.ncortez.TPV_TotalGlobal.dto.TableRequest;
import com.ncortez.TPV_TotalGlobal.entity.BusinessTable;
import com.ncortez.TPV_TotalGlobal.service.PosOperationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<?> createTable(@RequestBody TableRequest request) {
        try {
            return ResponseEntity.ok(posOperationsService.createTable(request));
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
            @RequestParam("username") String username,
            @RequestParam("sessionToken") String sessionToken,
            @RequestParam(value = "role", required = false) String role) {
        try {
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
            @RequestParam("username") String username,
            @RequestParam("sessionToken") String sessionToken,
            @RequestParam(value = "role", required = false) String role) {
        try {
            return ResponseEntity.ok(posOperationsService.releaseTable(tableNumber, username, sessionToken, role));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
