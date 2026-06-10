package com.ncortez.TPV_TotalGlobal.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ncortez.TPV_TotalGlobal.dto.ApiMessageResponse;
import com.ncortez.TPV_TotalGlobal.dto.CloseShiftRequest;
import com.ncortez.TPV_TotalGlobal.dto.CreateOrderRequest;
import com.ncortez.TPV_TotalGlobal.dto.DailyZReportResponse;
import com.ncortez.TPV_TotalGlobal.dto.GlovoSimulatedOrderRequest;
import com.ncortez.TPV_TotalGlobal.dto.MoveTableRequest;
import com.ncortez.TPV_TotalGlobal.dto.OpenShiftRequest;
import com.ncortez.TPV_TotalGlobal.dto.PaymentRequest;
import com.ncortez.TPV_TotalGlobal.dto.RefundRequest;
import com.ncortez.TPV_TotalGlobal.dto.TicketDetailResponse;
import com.ncortez.TPV_TotalGlobal.dto.TicketSummaryResponse;
import com.ncortez.TPV_TotalGlobal.entity.CashRegisterShift;
import com.ncortez.TPV_TotalGlobal.entity.Payment;
import com.ncortez.TPV_TotalGlobal.entity.Refund;
import com.ncortez.TPV_TotalGlobal.entity.SaleOrder;
import com.ncortez.TPV_TotalGlobal.exception.RefundConflictException;
import com.ncortez.TPV_TotalGlobal.service.PosOperationsService;

/**
 * Controlador REST principal del TPV.
 * Agrupa los endpoints de pedidos, cobros, devoluciones, turnos de caja y el cierre Z diario.
 */
@RestController
@RequestMapping("/api/pos")
@CrossOrigin(origins = "http://localhost:4200")
public class PosOperationsController {

    @Autowired
    private PosOperationsService posOperationsService;

    /**
     * Crea un pedido nuevo para una mesa o actualiza el que ya tiene abierto.
     * Si la lista de ítems está vacía, cancela el pedido abierto existente.
     *
     * @param request DTO con mesa, operador, ítems y notas
     * @return Pedido actualizado o error 400 con el motivo
     */
    @PostMapping("/orders")
    public ResponseEntity<?> openOrUpdateOrder(@RequestBody CreateOrderRequest request, Authentication authentication) {
        try {
            // Seguridad crítica: ignoramos usuario enviado por front y usamos el principal del JWT.
            request.setOperatorUsername(authentication.getName());
            SaleOrder saleOrder = posOperationsService.openOrUpdateOrder(request);
            return ResponseEntity.ok(saleOrder);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Dos peticiones simultáneas intentaron modificar el stock del mismo producto a la vez.
            // JPA detecta el conflicto gracias al campo @Version en Product y rechaza la segunda.
            // Devolvemos 409 Conflict para que el front pueda distinguirlo de un error de validación normal.
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiMessageResponse(
                        "El stock de uno de los productos fue modificado por otra operación simultánea. Por favor, inténtalo de nuevo."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

    /**
     * Devuelve todos los pedidos actualmente abiertos en el sistema.
     *
     * @return Lista de pedidos con estado OPEN
     */
    @GetMapping("/orders/open")
    public ResponseEntity<List<SaleOrder>> getOpenOrders() {
        return ResponseEntity.ok(posOperationsService.getOpenOrders());
    }

    /**
     * Cancela el pedido abierto de una mesa y libera el bloqueo del operador.
     *
     * @param tableNumber  Número de la mesa
     * @param username     Usuario que realiza la cancelación
     * @param sessionToken Token de sesión del cliente
     * @param role         Rol del usuario (ADMIN puede forzar cancelación)
     * @return 200 OK o error 400 con el motivo
     */
    @DeleteMapping("/orders/{tableNumber}")
    public ResponseEntity<?> clearOpenOrder(
            @PathVariable Integer tableNumber,
            @RequestParam String sessionToken,
            Authentication authentication) {
        try {
            // Usuario/rol efectivos siempre salen del contexto autenticado, no de query params.
            String username = authentication.getName();
            String role = extractRole(authentication);
            posOperationsService.clearOpenOrder(tableNumber, username, sessionToken, role);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

    /**
     * Mueve una comanda abierta de mesa origen a mesa destino.
     *
     * Reglas:
     * - Solo operador dueño de la mesa origen o ADMIN.
     * - La mesa destino debe estar libre y sin comanda abierta.
     */
    @PostMapping("/orders/move-table")
    public ResponseEntity<?> moveOpenOrderBetweenTables(@RequestBody MoveTableRequest request, Authentication authentication) {
        try {
            String username = authentication.getName();
            String role = extractRole(authentication);
            SaleOrder movedOrder = posOperationsService.moveOpenOrderBetweenTables(request, username, role);
            return ResponseEntity.ok(movedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

    /**
     * Registra el cobro de un pedido y lo cierra.
     * Libera la mesa y acumula el importe al turno de caja activo si lo hay.
     *
     * @param request DTO con ID del pedido, método de pago e importe
     * @return Cobro registrado o error 400
     */
    @PostMapping("/payments")
    public ResponseEntity<?> registerPayment(@RequestBody PaymentRequest request, Authentication authentication) {
        try {
            // Caja auditada: el cajero real lo determina el token autenticado.
            request.setCashierUsername(authentication.getName());
            Payment payment = posOperationsService.registerPayment(request);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

    /**
     * Devuelve el listado de todos los tickets cobrados con resumen de importes.
     *
     * @return Lista de tickets ordenados por fecha de cobro descendente
     */
    @GetMapping("/tickets")
    public ResponseEntity<List<TicketSummaryResponse>> getTickets() {
        return ResponseEntity.ok(posOperationsService.getTickets());
    }

    /**
     * Devuelve el detalle completo de un ticket con sus líneas y devoluciones.
     *
     * @param paymentId ID del cobro
     * @return Detalle del ticket o error 400 si no existe
     */
    @GetMapping("/tickets/{paymentId}")
    public ResponseEntity<?> getTicketByPaymentId(@PathVariable Long paymentId) {
        try {
            TicketDetailResponse ticket = posOperationsService.getTicketByPaymentId(paymentId);
            return ResponseEntity.ok(ticket);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

    /**
     * Devuelve todas las devoluciones registradas en el sistema.
     *
     * @return Lista de devoluciones ordenadas de más reciente a más antigua
     */
    @GetMapping("/refunds")
    public ResponseEntity<List<Refund>> getRefunds() {
        return ResponseEntity.ok(posOperationsService.getRefunds());
    }

    /**
     * Registra una devolución parcial o total sobre un cobro ya realizado.
     * Permite indicar si el producto devuelto regresa al stock o se considera desperdicio.
     *
     * @param request DTO con ID del cobro, línea, cantidad y opción de retorno a stock
     * @return Devolución registrada o error 400 con el motivo
     */
    @PostMapping("/refunds")
    public ResponseEntity<?> registerRefund(
            @RequestBody RefundRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Client-Attempt-At", required = false) String clientAttemptAt
    ) {
        try {
            request.setIdempotencyKey(idempotencyKey);
            request.setClientAttemptAt(parseClientAttemptAt(clientAttemptAt));
            Refund refund = posOperationsService.registerRefund(request);
            return ResponseEntity.ok(refund);
        } catch (RefundConflictException | ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiMessageResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

    private LocalDateTime parseClientAttemptAt(String clientAttemptAt) {
        if (clientAttemptAt == null || clientAttemptAt.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(clientAttemptAt.trim());
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(clientAttemptAt.trim()).toLocalDateTime();
            } catch (DateTimeParseException ignoredOffset) {
                return null;
            }
        }
    }

    /**
     * Abre un nuevo turno de caja. Solo puede existir un turno abierto a la vez.
     *
     * @param request DTO con el fondo inicial y el usuario que abre la caja (opcional)
     * @return Turno de caja abierto o error 400 si ya hay uno activo
     */
    @PostMapping("/shifts/open")
    // Bloque de seguridad crítico para defensa:
    // solo administradores pueden abrir turno de caja.
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> openShift(@RequestBody(required = false) OpenShiftRequest request, Authentication authentication) {
        try {
            OpenShiftRequest effectiveRequest = request != null ? request : new OpenShiftRequest();
            // openedBy también queda blindado por principal autenticado.
            effectiveRequest.setOpenedBy(authentication.getName());
            CashRegisterShift shift = posOperationsService.openShift(effectiveRequest);
            return ResponseEntity.ok(shift);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

    /**
     * Devuelve el turno de caja abierto actualmente.
     *
     * @return Turno abierto o null si no hay ninguno activo
     */
    @GetMapping("/shifts/current")
    public ResponseEntity<CashRegisterShift> getCurrentShift() {
        return ResponseEntity.ok(posOperationsService.getCurrentOpenShift().orElse(null));
    }

    /**
     * Devuelve el histórico de turnos de caja, opcionalmente filtrado por fecha de apertura.
     *
     * Parámetros opcionales (formato yyyy-MM-dd):
     * - 'startDate': incluye turnos abiertos desde esa fecha.
     * - 'endDate': incluye turnos abiertos hasta esa fecha.
     *
     * Si se envían ambos, se aplica el rango completo; si no se envía ninguno,
     * devuelve todo el histórico en orden descendente por apertura.
     *
     * @param startDate fecha inicial opcional
     * @param endDate fecha final opcional
     * @return lista de turnos de caja
     */
    @GetMapping("/shifts")
    public ResponseEntity<?> getShiftHistory(
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(posOperationsService.getShiftHistory(startDate, endDate));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

    /**
     * Devuelve el detalle de un turno de caja con productos vendidos y stock al cierre.
     *
     * @param shiftId identificador del turno
     * @return detalle del turno o error 400
     */
    @GetMapping("/shifts/{shiftId}/detail")
    public ResponseEntity<?> getShiftDetail(@PathVariable Long shiftId) {
        try {
            return ResponseEntity.ok(posOperationsService.getShiftDetail(shiftId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

    /**
     * Cierra el turno de caja actualmente activo.
     *
     * @param request DTO con el usuario que cierra la caja (opcional)
     * @return Turno de caja cerrado o error 400 si no hay ningún turno abierto
     */
    @PostMapping("/shifts/close")
    public ResponseEntity<?> closeShift(@RequestBody(required = false) CloseShiftRequest request, Authentication authentication) {
        try {
            CloseShiftRequest effectiveRequest = request != null ? request : new CloseShiftRequest();
            // closedBy también queda blindado por principal autenticado.
            effectiveRequest.setClosedBy(authentication.getName());
            CashRegisterShift shift = posOperationsService.closeShift(effectiveRequest);
            return ResponseEntity.ok(shift);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }

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

    /**
     * Genera el cierre Z con los totales de ventas, IVA, coste y beneficio para una fecha.
     * Si no se indica fecha se usa el día actual.
     *
     * @param date Fecha del informe (formato: yyyy-MM-dd), opcional
     * @return Objeto con todos los totales del día
     */
    @GetMapping("/reports/z")
    public ResponseEntity<DailyZReportResponse> getDailyZReport(
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(posOperationsService.getDailyZReport(date));
    }

    /**
     * Simula la recepción de un pedido de Glovo y lo registra como ticket cobrado en TPV.
     */
    @PostMapping("/integrations/glovo/simulate")
    public ResponseEntity<?> simulateGlovoOrder(@RequestBody GlovoSimulatedOrderRequest request) {
        try {
            return ResponseEntity.ok(posOperationsService.simulateGlovoOrder(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiMessageResponse(e.getMessage()));
        }
    }
}