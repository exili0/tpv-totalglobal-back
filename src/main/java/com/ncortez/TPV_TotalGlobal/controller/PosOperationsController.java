package com.ncortez.TPV_TotalGlobal.controller;

import com.ncortez.TPV_TotalGlobal.dto.*;
import com.ncortez.TPV_TotalGlobal.entity.CashRegisterShift;
import com.ncortez.TPV_TotalGlobal.entity.Payment;
import com.ncortez.TPV_TotalGlobal.entity.Refund;
import com.ncortez.TPV_TotalGlobal.entity.SaleOrder;
import com.ncortez.TPV_TotalGlobal.service.PosOperationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
    public ResponseEntity<?> openOrUpdateOrder(@RequestBody CreateOrderRequest request) {
        try {
            SaleOrder saleOrder = posOperationsService.openOrUpdateOrder(request);
            return ResponseEntity.ok(saleOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
            @RequestParam String username,
            @RequestParam String sessionToken,
            @RequestParam(value = "role", required = false) String role) {
        try {
            posOperationsService.clearOpenOrder(tableNumber, username, sessionToken, role);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
    public ResponseEntity<?> registerPayment(@RequestBody PaymentRequest request) {
        try {
            Payment payment = posOperationsService.registerPayment(request);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
            return ResponseEntity.badRequest().body(e.getMessage());
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
    public ResponseEntity<?> registerRefund(@RequestBody RefundRequest request) {
        try {
            Refund refund = posOperationsService.registerRefund(request);
            return ResponseEntity.ok(refund);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Abre un nuevo turno de caja. Solo puede existir un turno abierto a la vez.
     *
     * @param request DTO con el fondo inicial y el usuario que abre la caja (opcional)
     * @return Turno de caja abierto o error 400 si ya hay uno activo
     */
    @PostMapping("/shifts/open")
    public ResponseEntity<?> openShift(@RequestBody(required = false) OpenShiftRequest request) {
        try {
            CashRegisterShift shift = posOperationsService.openShift(request);
            return ResponseEntity.ok(shift);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
     * Cierra el turno de caja actualmente activo.
     *
     * @param request DTO con el usuario que cierra la caja (opcional)
     * @return Turno de caja cerrado o error 400 si no hay ningún turno abierto
     */
    @PostMapping("/shifts/close")
    public ResponseEntity<?> closeShift(@RequestBody(required = false) CloseShiftRequest request) {
        try {
            CashRegisterShift shift = posOperationsService.closeShift(request);
            return ResponseEntity.ok(shift);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
}