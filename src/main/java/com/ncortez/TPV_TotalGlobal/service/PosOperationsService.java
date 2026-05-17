package com.ncortez.TPV_TotalGlobal.service;

import com.ncortez.TPV_TotalGlobal.dto.CloseShiftRequest;
import com.ncortez.TPV_TotalGlobal.dto.CreateOrderRequest;
import com.ncortez.TPV_TotalGlobal.dto.DailyZReportResponse;
import com.ncortez.TPV_TotalGlobal.dto.OpenShiftRequest;
import com.ncortez.TPV_TotalGlobal.dto.OrderItemRequest;
import com.ncortez.TPV_TotalGlobal.dto.PaymentRequest;
import com.ncortez.TPV_TotalGlobal.dto.RefundRequest;
import com.ncortez.TPV_TotalGlobal.dto.TableRequest;
import com.ncortez.TPV_TotalGlobal.dto.TicketDetailResponse;
import com.ncortez.TPV_TotalGlobal.dto.TicketLineResponse;
import com.ncortez.TPV_TotalGlobal.dto.TicketSummaryResponse;
import com.ncortez.TPV_TotalGlobal.entity.BusinessTable;
import com.ncortez.TPV_TotalGlobal.entity.CashRegisterShift;
import com.ncortez.TPV_TotalGlobal.entity.Payment;
import com.ncortez.TPV_TotalGlobal.entity.Product;
import com.ncortez.TPV_TotalGlobal.entity.Refund;
import com.ncortez.TPV_TotalGlobal.entity.SaleOrder;
import com.ncortez.TPV_TotalGlobal.entity.SaleOrderLine;
import com.ncortez.TPV_TotalGlobal.entity.enums.CashShiftStatus;
import com.ncortez.TPV_TotalGlobal.entity.enums.OrderStatus;
import com.ncortez.TPV_TotalGlobal.entity.enums.PaymentMethod;
import com.ncortez.TPV_TotalGlobal.entity.enums.TableStatus;
import com.ncortez.TPV_TotalGlobal.repository.BusinessTableRepository;
import com.ncortez.TPV_TotalGlobal.repository.CashRegisterShiftRepository;
import com.ncortez.TPV_TotalGlobal.repository.PaymentRepository;
import com.ncortez.TPV_TotalGlobal.repository.ProductRepository;
import com.ncortez.TPV_TotalGlobal.repository.RefundRepository;
import com.ncortez.TPV_TotalGlobal.repository.SaleOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio principal de negocio del TPV: mesas, ÃƒÂ³rdenes, cobros, turnos y reporte Z.
 */
@Service
public class PosOperationsService {

    @Autowired
    private BusinessTableRepository businessTableRepository;

    @Autowired
    private SaleOrderRepository saleOrderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CashRegisterShiftRepository cashRegisterShiftRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private StockService stockService;

    /**
     * Devuelve todas las mesas activas del negocio, ordenadas por número.
     * Se usa al cargar el plano de sala en el TPV.
     *
     * @return Lista de mesas activas ordenadas ascendentemente
     */
    public List<BusinessTable> getActiveTables() {
        return businessTableRepository.findByActiveTrueOrderByTableNumberAsc();
    }

    /**
     * Crea una nueva mesa en el sistema.
     * Si el número de mesa es 0, se asigna automáticamente el nombre "Bar".
     *
     * @param request DTO con número, nombre y capacidad de la mesa
     * @return Mesa creada y persistida
     * @throws RuntimeException si ya existe una mesa con ese número
     */
    public BusinessTable createTable(TableRequest request) {
        if (request == null || request.getTableNumber() == null) {
            throw new RuntimeException("El nÃƒÂºmero de mesa es obligatorio");
        }

        businessTableRepository.findByTableNumber(request.getTableNumber()).ifPresent(existing -> {
            throw new RuntimeException("Ya existe una mesa con ese nÃƒÂºmero");
        });

        String displayName = request.getDisplayName() != null && !request.getDisplayName().isBlank()
                ? request.getDisplayName().trim()
                : "Table " + request.getTableNumber();

        Integer capacity = 1;
        if (request.getCapacity() != null && request.getCapacity() > 0) {
            capacity = request.getCapacity();
        }

        BusinessTable table = new BusinessTable(request.getTableNumber(), displayName, capacity);
        if (request.getTableNumber() == 0) {
            table.setDisplayName("Bar");
        }

        return businessTableRepository.save(table);
    }

    /**
     * Bloquea una mesa para el operador indicado y su sesión activa.
     * Implementa un mecanismo de concurrencia optimista: si la mesa ya está en uso
     * por otra sesión, se lanza una excepción a menos que sea el mismo operador o un admin.
     * La mesa 0 (barra) nunca se bloquea por sesión.
     *
     * @param tableNumber  Número de la mesa a reclamar
     * @param username     Usuario que intenta tomar la mesa
     * @param sessionToken Token único de sesión del cliente Angular
     * @param role         Rol del usuario (ADMIN puede hacer override)
     * @return Mesa con el operador y token de bloqueo actualizados
     * @throws RuntimeException si la mesa está bloqueada por otra sesión
     */
    @Transactional
    public BusinessTable claimTable(Integer tableNumber, String username, String sessionToken, String role) {
        if (tableNumber == null) {
            throw new RuntimeException("El nÃƒÂºmero de mesa es obligatorio");
        }

        String safeUsername = normalizeUsername(username);
        if (safeUsername == null) {
            throw new RuntimeException("El usuario es obligatorio");
        }

        String safeSessionToken = normalizeToken(sessionToken);
        if (safeSessionToken == null) {
            throw new RuntimeException("La sesiÃƒÂ³n es obligatoria");
        }

        BusinessTable table = businessTableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new RuntimeException("Mesa no encontrada: " + tableNumber));

        if (!table.isActive() || table.getStatus() == TableStatus.INACTIVE) {
            throw new RuntimeException("La mesa no estÃƒÂ¡ operativa");
        }

        if (tableNumber != 0) {
            String attendedBy = normalizeUsername(table.getAttendedBy());
            String lockToken = normalizeToken(table.getLockToken());
            boolean sameOperator = attendedBy != null && attendedBy.equalsIgnoreCase(safeUsername);
            boolean adminOverride = isAdminRole(role);

            if (lockToken != null && !lockToken.equals(safeSessionToken) && !sameOperator && !adminOverride) {
                throw new RuntimeException("La mesa " + tableNumber + " ya estÃƒÂ¡ bloqueada por otra sesiÃƒÂ³n");
            }

            if (attendedBy != null && !sameOperator && lockToken != null && !adminOverride) {
                throw new RuntimeException("La mesa " + tableNumber + " estÃƒÂ¡ siendo atendida por " + attendedBy);
            }

            table.setAttendedBy(safeUsername);
            table.setLockedAt(LocalDateTime.now());
            table.setLockToken(safeSessionToken);
            if (table.getStatus() == TableStatus.FREE) {
                table.setStatus(TableStatus.OCCUPIED);
            }
        }

        return businessTableRepository.save(table);
    }

    /**
     * Libera el bloqueo de una mesa y la vuelve al estado FREE.
     * Solo puede liberar la mesa el mismo operador que la tomó, o un administrador.
     *
     * @param tableNumber  Número de la mesa a liberar
     * @param username     Usuario que intenta liberar la mesa
     * @param sessionToken Token de sesión del cliente
     * @param role         Rol del usuario
     * @return Mesa con bloqueo eliminado y estado FREE
     * @throws RuntimeException si el usuario no tiene permiso para liberar la mesa
     */
    @Transactional
    public BusinessTable releaseTable(Integer tableNumber, String username, String sessionToken, String role) {
        if (tableNumber == null) {
            throw new RuntimeException("El nÃƒÂºmero de mesa es obligatorio");
        }

        String safeUsername = normalizeUsername(username);
        if (safeUsername == null) {
            throw new RuntimeException("El usuario es obligatorio");
        }

        String safeSessionToken = normalizeToken(sessionToken);
        if (safeSessionToken == null) {
            throw new RuntimeException("La sesiÃƒÂ³n es obligatoria");
        }

        BusinessTable table = businessTableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new RuntimeException("Mesa no encontrada: " + tableNumber));

        String attendedBy = normalizeUsername(table.getAttendedBy());
        String lockToken = normalizeToken(table.getLockToken());
        boolean sameOperator = attendedBy != null && attendedBy.equalsIgnoreCase(safeUsername);
        boolean adminOverride = isAdminRole(role);

        if (lockToken != null && !lockToken.equals(safeSessionToken) && !sameOperator && !adminOverride) {
            throw new RuntimeException("Solo la sesiÃƒÂ³n que tomÃƒÂ³ la mesa puede liberarla");
        }

        if (attendedBy != null && !sameOperator && !adminOverride) {
            throw new RuntimeException("Solo el usuario que tomÃƒÂ³ la mesa puede liberarla");
        }

        table.setAttendedBy(null);
        table.setLockedAt(null);
        table.setLockToken(null);
        if (table.getStatus() != TableStatus.INACTIVE) {
            table.setStatus(TableStatus.FREE);
        }

        return businessTableRepository.save(table);
    }

    /**
     * Abre un pedido nuevo o actualiza el pedido abierto de una mesa.
     * Si la lista de ítems está vacía, elimina el pedido abierto existente (cancelación).
     * Al añadir líneas, descuenta el stock de cada producto automáticamente.
     * Recalcula totales (subtotal, IVA, total, coste, beneficio) tras cada cambio.
     *
     * @param request DTO con número de mesa, operador, items y notas opcionales
     * @return Pedido creado o actualizado
     * @throws RuntimeException si la mesa no existe, está ocupada por otro, o hay stock insuficiente
     */
    @Transactional
    public SaleOrder openOrUpdateOrder(CreateOrderRequest request) {
        if (request == null) {
            throw new RuntimeException("El cuerpo de la solicitud es obligatorio");
        }

        Integer tableNumberValue = request.getTableNumber();
        final int tableNumber = tableNumberValue != null ? tableNumberValue : 0;
        String operatorUsername = normalizeUsername(request.getOperatorUsername());
        String operatorSessionToken = normalizeToken(request.getOperatorSessionToken());

        BusinessTable table = businessTableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new RuntimeException("Mesa no encontrada: " + tableNumber));

        if (!table.isActive() || table.getStatus() == TableStatus.INACTIVE) {
            throw new RuntimeException("La mesa no estÃƒÂ¡ operativa");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return saleOrderRepository.findFirstByTableAndStatus(table, OrderStatus.OPEN)
                    .map(existingOrder -> {
                        saleOrderRepository.delete(existingOrder);
                        return existingOrder;
                    })
                    .orElseGet(() -> {
                        SaleOrder emptyOrder = new SaleOrder();
                        emptyOrder.setTable(table);
                        emptyOrder.setStatus(OrderStatus.OPEN);
                        return emptyOrder;
                    });
        }

        if (tableNumber != 0) {
            String attendedBy = normalizeUsername(table.getAttendedBy());
            String lockToken = normalizeToken(table.getLockToken());
            boolean sameOperator = attendedBy != null && attendedBy.equalsIgnoreCase(operatorUsername);

            if (operatorUsername == null) {
                throw new RuntimeException("El usuario que atiende la mesa es obligatorio");
            }

            if (operatorSessionToken == null) {
                throw new RuntimeException("La sesiÃƒÂ³n del operador es obligatoria");
            }

            if (lockToken != null && !lockToken.equals(operatorSessionToken) && !sameOperator) {
                throw new RuntimeException("La mesa " + tableNumber + " estÃƒÂ¡ bloqueada por otra sesiÃƒÂ³n");
            }

            if (attendedBy != null && !sameOperator && lockToken != null) {
                throw new RuntimeException("La mesa " + tableNumber + " estÃƒÂ¡ siendo atendida por " + attendedBy);
            }

            table.setAttendedBy(operatorUsername);
            table.setLockedAt(LocalDateTime.now());
            table.setLockToken(operatorSessionToken);
            businessTableRepository.save(table);
        }

        SaleOrder saleOrder = saleOrderRepository.findFirstByTableAndStatus(table, OrderStatus.OPEN)
                .orElseGet(() -> {
                    SaleOrder order = new SaleOrder();
                    order.setTable(table);
                    order.setStatus(OrderStatus.OPEN);
                    return order;
                });

        saleOrder.getOrderLines().clear();

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            saleOrder.setNotes(request.getNotes().trim());
        }

        for (OrderItemRequest item : request.getItems()) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("Línea de pedido inválida");
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + item.getProductId()));

            // Descontar stock cuando se agrega el producto al pedido
            try {
                stockService.deductStockForSale(product, item.getQuantity(), saleOrder.getId());
            } catch (RuntimeException e) {
                // Si hay problema con el stock, lanzar excepción con mensaje para el usuario
                throw new RuntimeException("Error de stock: " + e.getMessage());
            }

            SaleOrderLine orderLine = buildOrderLine(saleOrder, product, item.getQuantity());
            saleOrder.getOrderLines().add(orderLine);
        }

        recalculateOrderTotals(saleOrder);

        if (tableNumber != 0) {
            table.setStatus(TableStatus.OCCUPIED);
            businessTableRepository.save(table);
        }

        return saleOrderRepository.save(saleOrder);
    }

    /**
     * Devuelve todos los pedidos en estado OPEN del sistema.
     * Usado por el panel de administración para ver qué mesas tienen pedidos activos.
     *
     * @return Lista de pedidos abiertos
     */
    public List<SaleOrder> getOpenOrders() {
        return saleOrderRepository.findByStatus(OrderStatus.OPEN);
    }

    /**
     * Cancela y elimina el pedido abierto de una mesa, liberando también el bloqueo.
     * Solo el operador que tiene la mesa o un admin puede realizar esta acción.
     *
     * @param tableNumber  Número de la mesa cuyo pedido se cancela
     * @param username     Usuario que realiza la cancelación
     * @param sessionToken Token de sesión del cliente
     * @param role         Rol del usuario
     * @throws RuntimeException si el usuario no tiene permisos sobre la mesa
     */
    @Transactional
    public void clearOpenOrder(Integer tableNumber, String username, String sessionToken, String role) {
        if (tableNumber == null) {
            throw new RuntimeException("El nÃƒÂºmero de mesa es obligatorio");
        }

        String safeUsername = normalizeUsername(username);
        if (safeUsername == null) {
            throw new RuntimeException("El usuario es obligatorio");
        }

        String safeSessionToken = normalizeToken(sessionToken);
        if (safeSessionToken == null) {
            throw new RuntimeException("La sesiÃƒÂ³n es obligatoria");
        }

        BusinessTable table = businessTableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new RuntimeException("Mesa no encontrada: " + tableNumber));

        if (tableNumber != 0) {
            String attendedBy = normalizeUsername(table.getAttendedBy());
            String lockToken = normalizeToken(table.getLockToken());
            boolean sameOperator = attendedBy != null && attendedBy.equalsIgnoreCase(safeUsername);
            boolean adminOverride = isAdminRole(role);

            if (lockToken != null && !lockToken.equals(safeSessionToken) && !sameOperator && !adminOverride) {
                throw new RuntimeException("La mesa " + tableNumber + " estÃƒÂ¡ bloqueada por otra sesiÃƒÂ³n");
            }

            if (attendedBy != null && !sameOperator && !adminOverride) {
                throw new RuntimeException("La mesa " + tableNumber + " estÃƒÂ¡ siendo atendida por " + attendedBy);
            }
        }

        saleOrderRepository.findFirstByTableAndStatus(table, OrderStatus.OPEN)
                .ifPresent(saleOrderRepository::delete);

        if (tableNumber != 0) {
            table.setAttendedBy(null);
            table.setLockedAt(null);
            table.setLockToken(null);
            table.setStatus(TableStatus.FREE);
            businessTableRepository.save(table);
        }
    }

    /**
     * Devuelve todas las devoluciones registradas en el sistema, ordenadas de más reciente a más antigua.
     *
     * @return Lista de devoluciones
     */
    public List<Refund> getRefunds() {
        return refundRepository.findAllByOrderByRefundedAtDesc();
    }

    /**
     * Devuelve el resumen de todos los tickets cobrados, incluyendo el importe ya devuelto
     * y el importe pendiente de devolución en cada uno.
     *
     * @return Lista de resúmenes de tickets ordenados por fecha de cobro descendente
     */
    public List<TicketSummaryResponse> getTickets() {
        List<Payment> payments = paymentRepository.findAllWithSaleOrderAndTableOrderByPaidAtDesc();
        List<Object[]> refundedRows = refundRepository.sumAmountGroupedByPayment();

        Map<Long, BigDecimal> refundedByPayment = new HashMap<>();
        for (Object[] row : refundedRows) {
            Long paymentId = (Long) row[0];
            BigDecimal refundedAmount = row[1] instanceof BigDecimal ? (BigDecimal) row[1] : BigDecimal.ZERO;
            refundedByPayment.put(paymentId, refundedAmount);
        }

        List<TicketSummaryResponse> tickets = new ArrayList<>();
        for (Payment payment : payments) {
            BigDecimal refundedAmount = refundedByPayment.getOrDefault(payment.getId(), BigDecimal.ZERO);
            tickets.add(buildTicketSummary(payment, refundedAmount));
        }

        return tickets;
    }

    /**
     * Devuelve el detalle completo de un ticket a partir de su ID de cobro.
     * Incluye todas las líneas del pedido con las cantidades ya devueltas y pendientes.
     *
     * @param paymentId ID del cobro asociado al ticket
     * @return Detalle completo del ticket con líneas y devoluciones
     * @throws RuntimeException si el ticket no existe
     */
    public TicketDetailResponse getTicketByPaymentId(Long paymentId) {
        if (paymentId == null) {
            throw new RuntimeException("El identificador del ticket es obligatorio");
        }

        Payment payment = paymentRepository.findByIdWithTicketDetail(paymentId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado: " + paymentId));

        BigDecimal refundedAmount = refundRepository.sumAmountByPaymentId(paymentId);
        return buildTicketDetail(payment, refundedAmount);
    }

    /**
     * Construye el resumen de un ticket a partir del cobro y el importe ya devuelto.
     * Calcula el importe devuelto acumulado y el que aún se puede devolver.
     */
    private TicketSummaryResponse buildTicketSummary(Payment payment, BigDecimal refundedAmount) {
        SaleOrder saleOrder = payment.getSaleOrder();

        TicketSummaryResponse response = new TicketSummaryResponse();
        response.setPaymentId(payment.getId());
        response.setSaleOrderId(saleOrder.getId());
        response.setTableNumber(saleOrder.getTable() != null ? saleOrder.getTable().getTableNumber() : null);
        response.setServiceLabel(getServiceLabel(saleOrder));
        response.setPaidAt(payment.getPaidAt());
        response.setTotalAmount(scale(payment.getAmount()));
        response.setTotalItems(saleOrder.getOrderLines().stream().mapToInt(SaleOrderLine::getQuantity).sum());
        response.setPaymentMethod(payment.getPaymentMethod());

        BigDecimal safeRefunded = refundedAmount != null ? refundedAmount : BigDecimal.ZERO;
        BigDecimal refundable = payment.getAmount().subtract(safeRefunded);

        response.setRefundedAmount(scale(safeRefunded));
        response.setRefundableAmount(scale(refundable.max(BigDecimal.ZERO)));
        return response;
    }

    /**
     * Construye el detalle completo de un ticket incluyendo cada línea de pedido,
     * con las cantidades devueltas y las que aún son devolvibles.
     */
    private TicketDetailResponse buildTicketDetail(Payment payment, BigDecimal refundedAmount) {
        SaleOrder saleOrder = payment.getSaleOrder();
        TicketSummaryResponse summary = buildTicketSummary(payment, refundedAmount);
        Map<Long, Integer> refundedQuantityByLine = getRefundedQuantityByLine(payment.getId());

        TicketDetailResponse detail = new TicketDetailResponse();
        detail.setPaymentId(summary.getPaymentId());
        detail.setSaleOrderId(summary.getSaleOrderId());
        detail.setTableNumber(summary.getTableNumber());
        detail.setServiceLabel(summary.getServiceLabel());
        detail.setPaidAt(summary.getPaidAt());
        detail.setTotalAmount(summary.getTotalAmount());
        detail.setTotalItems(summary.getTotalItems());
        detail.setPaymentMethod(summary.getPaymentMethod());
        detail.setRefundedAmount(summary.getRefundedAmount());
        detail.setRefundableAmount(summary.getRefundableAmount());
        detail.setNotes(saleOrder.getNotes());

        List<TicketLineResponse> lines = new ArrayList<>();
        for (SaleOrderLine orderLine : saleOrder.getOrderLines()) {
            int refundedQuantity = refundedQuantityByLine.getOrDefault(orderLine.getId(), 0);
            int safeRefundedQuantity = Math.max(0, Math.min(refundedQuantity, orderLine.getQuantity()));

            TicketLineResponse line = new TicketLineResponse();
            line.setLineId(orderLine.getId());
            line.setProductName(orderLine.getProductName());
            line.setQuantity(orderLine.getQuantity());
            line.setRefundedQuantity(safeRefundedQuantity);
            line.setRefundableQuantity(Math.max(orderLine.getQuantity() - safeRefundedQuantity, 0));
            line.setUnitPrice(scale(orderLine.getUnitPrice()));
            line.setLineTotal(scale(orderLine.getTotal()));
            lines.add(line);
        }
        detail.setLines(lines);

        return detail;
    }

    private String getServiceLabel(SaleOrder saleOrder) {
        if (saleOrder.getTable() == null || saleOrder.getTable().getTableNumber() == null) {
            return "Sin mesa";
        }
        Integer tableNumber = saleOrder.getTable().getTableNumber();
        return tableNumber == 0 ? "Barra" : "Mesa " + tableNumber;
    }

    @Transactional
    public Refund registerRefund(RefundRequest request) {
        if (request == null || request.getPaymentId() == null) {
            throw new RuntimeException("El identificador del cobro es obligatorio");
        }

        if (request.getRefundedBy() == null || request.getRefundedBy().isBlank()) {
            throw new RuntimeException("El usuario que realiza la devoluciÃƒÂ³n es obligatorio");
        }

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Cobro no encontrado: " + request.getPaymentId()));

        BigDecimal refundedAmount = refundRepository.sumAmountByPaymentId(payment.getId());
        BigDecimal remainingAmount = payment.getAmount().subtract(refundedAmount != null ? refundedAmount : BigDecimal.ZERO);

        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Este cobro ya está completamente devuelto");
        }

        BigDecimal amount;
        SaleOrderLine refundedLine = null;
        Integer refundedQuantity = null;

        if (request.getSaleOrderLineId() != null) {
            refundedLine = payment.getSaleOrder().getOrderLines().stream()
                    .filter(line -> line.getId().equals(request.getSaleOrderLineId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("La línea indicada no pertenece al ticket"));

            Integer requestedQuantityValue = request.getQuantity();
            int requestedQuantity = requestedQuantityValue != null ? requestedQuantityValue : 0;
            if (requestedQuantity <= 0) {
                throw new RuntimeException("La cantidad a devolver debe ser mayor que cero");
            }

            Long rawRefundedQuantity = refundRepository.sumRefundedQuantityByPaymentAndLine(payment.getId(), refundedLine.getId());
            int alreadyRefundedQuantity = rawRefundedQuantity != null ? rawRefundedQuantity.intValue() : 0;
            int refundableQuantity = refundedLine.getQuantity() - alreadyRefundedQuantity;

            if (refundableQuantity <= 0) {
                throw new RuntimeException("Esa línea ya está completamente devuelta");
            }

            if (requestedQuantity > refundableQuantity) {
                throw new RuntimeException("La cantidad supera las unidades pendientes de devolución para ese producto");
            }

            BigDecimal lineUnitTotal = refundedLine.getTotal()
                    .divide(BigDecimal.valueOf(refundedLine.getQuantity()), 2, RoundingMode.HALF_UP);

            amount = lineUnitTotal.multiply(BigDecimal.valueOf(requestedQuantity));
            refundedQuantity = requestedQuantity;
        } else {
            amount = request.getAmount() != null ? request.getAmount() : remainingAmount;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El importe de la devolución debe ser mayor que cero");
        }

        if (amount.compareTo(remainingAmount) > 0) {
            throw new RuntimeException("La devolución no puede superar el importe pendiente");
        }

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setSaleOrder(payment.getSaleOrder());
        refund.setAmount(scale(amount));
        refund.setSaleOrderLine(refundedLine);
        refund.setRefundedQuantity(refundedQuantity);
        refund.setReason(request.getReason() != null ? request.getReason().trim() : null);
        refund.setRefundedBy(normalizeUsername(request.getRefundedBy()));
        
        // Establecer si el producto regresa al stock o es considerado desecho
        boolean shouldReturnToStock = request.getReturnToStock() != null ? request.getReturnToStock() : true;
        refund.setReturnToStock(shouldReturnToStock);

        Refund savedRefund = refundRepository.save(refund);

        // Manejar el stock según la opción de devolución
        if (refundedLine != null && refundedQuantity != null) {
            Product product = refundedLine.getProduct();
            
            if (shouldReturnToStock) {
                // El producto regresa al stock
                stockService.returnStockFromRefund(product, refundedQuantity, savedRefund.getId());
            } else {
                // El producto se considera desecho (pérdida)
                String wasteReason = request.getReason() != null ? request.getReason().trim() : "Devolución sin retorno a stock";
                stockService.registerStockWaste(product, savedRefund, refundedQuantity, wasteReason);
            }
        }

        return savedRefund;
    }

    /**
     * Calcula las unidades ya devueltas por línea de pedido para un cobro determinado.
     * Se usa al construir el detalle del ticket para saber qué líneas quedan devolvibles.
     */
    private Map<Long, Integer> getRefundedQuantityByLine(Long paymentId) {
        List<Object[]> rows = refundRepository.sumRefundedQuantityGroupedByLine(paymentId);
        Map<Long, Integer> result = new HashMap<>();

        for (Object[] row : rows) {
            Long saleOrderLineId = row[0] instanceof Long ? (Long) row[0] : null;
            Number refundedQty = row[1] instanceof Number ? (Number) row[1] : 0;
            if (saleOrderLineId != null) {
                result.put(saleOrderLineId, refundedQty.intValue());
            }
        }

        return result;
    }

    /**
     * Registra el cobro de un pedido abierto y lo cierra.
     * Libera la mesa si es una mesa numerada, y acumula los importes al turno de caja activo.
     * Si no hay turno abierto, el cobro se registra igualmente sin actualizar la caja.
     *
     * @param request DTO con el ID del pedido, método de pago e importe
     * @return Cobro registrado
     * @throws RuntimeException si el pedido no existe o ya está cobrado
     */
    @Transactional
    public Payment registerPayment(PaymentRequest request) {
        if (request == null || request.getSaleOrderId() == null) {
            throw new RuntimeException("El identificador de la orden es obligatorio");
        }

        SaleOrder saleOrder = saleOrderRepository.findById(request.getSaleOrderId())
                .orElseThrow(() -> new RuntimeException("Orden de venta no encontrada"));

        if (saleOrder.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Solo se pueden cobrar ÃƒÂ³rdenes abiertas");
        }

        PaymentMethod paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.OTHER;
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : saleOrder.getTotal();

        Payment payment = new Payment();
        payment.setSaleOrder(saleOrder);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        saleOrder.setStatus(OrderStatus.PAID);
        saleOrder.setClosedAt(LocalDateTime.now());
        saleOrderRepository.save(saleOrder);

        BusinessTable table = saleOrder.getTable();
        if (table != null && table.getTableNumber() != 0) {
            table.setStatus(TableStatus.FREE);
            table.setAttendedBy(null);
            table.setLockedAt(null);
            table.setLockToken(null);
            businessTableRepository.save(table);
        }

        cashRegisterShiftRepository.findFirstByStatusOrderByOpenedAtDesc(CashShiftStatus.OPEN)
                .ifPresent(shift -> {
                    normalizeShiftTotals(shift);
                    addPaymentToShift(shift, amount, paymentMethod, saleOrder.getTotalProfit());
                    cashRegisterShiftRepository.save(shift);
                });

        return payment;
    }

    /**
     * Abre un nuevo turno de caja con el fondo inicial indicado.
     * Solo puede haber un turno abierto a la vez en el sistema.
     *
     * @param request DTO con el fondo de apertura y el usuario que abre la caja
     * @return Turno de caja recién abierto
     * @throws RuntimeException si ya hay un turno abierto
     */
    public CashRegisterShift openShift(OpenShiftRequest request) {
        cashRegisterShiftRepository.findFirstByStatusOrderByOpenedAtDesc(CashShiftStatus.OPEN).ifPresent(shift -> {
            throw new RuntimeException("Ya existe un turno de caja abierto");
        });

        CashRegisterShift shift = new CashRegisterShift();
        shift.setStatus(CashShiftStatus.OPEN);
        shift.setOpenedAt(LocalDateTime.now());
        shift.setOpeningFloat(request != null && request.getOpeningFloat() != null ? request.getOpeningFloat() : BigDecimal.ZERO);
        shift.setOpenedBy(request != null ? request.getOpenedBy() : null);

        return cashRegisterShiftRepository.save(shift);
    }

    /**
     * Cierra el turno de caja actualmente abierto.
     * Registra la fecha de cierre y el usuario que la realiza.
     *
     * @param request DTO con el usuario que cierra la caja
     * @return Turno de caja cerrado
     * @throws RuntimeException si no hay ningún turno abierto
     */
    public CashRegisterShift closeShift(CloseShiftRequest request) {
        CashRegisterShift shift = cashRegisterShiftRepository.findFirstByStatusOrderByOpenedAtDesc(CashShiftStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("No hay un turno de caja abierto"));

        shift.setStatus(CashShiftStatus.CLOSED);
        shift.setClosedAt(LocalDateTime.now());
        shift.setClosedBy(request != null ? request.getClosedBy() : null);

        return cashRegisterShiftRepository.save(shift);
    }

    /**
     * Genera el cierre Z diario para una fecha determinada.
     * Agrega ventas totales, IVA, coste, beneficio y desglose por método de pago
     * (efectivo, tarjeta y otros) de todos los pedidos cobrados ese día.
     *
     * @param date Fecha del informe (null = hoy)
     * @return Objeto con todos los totales del día
     */
    public DailyZReportResponse getDailyZReport(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<SaleOrder> paidOrders = saleOrderRepository.findByStatusAndClosedAtBetween(OrderStatus.PAID, start, end);
        List<Payment> payments = paymentRepository.findByPaidAtBetween(start, end);

        BigDecimal totalSales = paidOrders.stream().map(SaleOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVat = paidOrders.stream().map(SaleOrder::getTotalVat).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = paidOrders.stream().map(SaleOrder::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = paidOrders.stream().map(SaleOrder::getTotalProfit).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cashSales = sumByMethod(payments, PaymentMethod.CASH);
        BigDecimal cardSales = sumByMethod(payments, PaymentMethod.CARD);
        BigDecimal otherSales = payments.stream()
                .filter(payment -> payment.getPaymentMethod() != PaymentMethod.CASH && payment.getPaymentMethod() != PaymentMethod.CARD)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DailyZReportResponse response = new DailyZReportResponse();
        response.setDate(targetDate);
        response.setTicketsCount(paidOrders.size());
        response.setTotalSales(scale(totalSales));
        response.setTotalVat(scale(totalVat));
        response.setTotalCost(scale(totalCost));
        response.setTotalProfit(scale(totalProfit));
        response.setCashSales(scale(cashSales));
        response.setCardSales(scale(cardSales));
        response.setOtherSales(scale(otherSales));
        return response;
    }

    /**
     * Construye una línea de pedido con todos los cálculos económicos:
     * subtotal sin IVA, importe de IVA, total con IVA, coste y beneficio unitario.
     */
    private SaleOrderLine buildOrderLine(SaleOrder saleOrder, Product product, Integer quantity) {
        SaleOrderLine orderLine = new SaleOrderLine();
        orderLine.setSaleOrder(saleOrder);
        orderLine.setProduct(product);
        orderLine.setProductName(product.getName());
        orderLine.setQuantity(quantity);
        orderLine.setVatPercent(product.getVatPercent());

        BigDecimal unitPrice = product.getPrice();
        BigDecimal unitCost = product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO;

        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity.longValue()));
        BigDecimal vatAmount = subtotal.multiply(BigDecimal.valueOf(product.getVatPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(vatAmount);

        BigDecimal costTotal = unitCost.multiply(BigDecimal.valueOf(quantity.longValue()));
        BigDecimal profit = subtotal.subtract(costTotal);

        orderLine.setUnitPrice(scale(unitPrice));
        orderLine.setUnitCost(scale(unitCost));
        orderLine.setSubtotal(scale(subtotal));
        orderLine.setVatAmount(scale(vatAmount));
        orderLine.setTotal(scale(total));
        orderLine.setCostTotal(scale(costTotal));
        orderLine.setProfit(scale(profit));

        return orderLine;
    }

    /**
     * Recalcula y actualiza los totales del pedido sumando todas sus líneas.
     * Se invoca siempre que se añaden o eliminan líneas del pedido.
     */
    private void recalculateOrderTotals(SaleOrder saleOrder) {
        BigDecimal subtotal = saleOrder.getOrderLines().stream().map(SaleOrderLine::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVat = saleOrder.getOrderLines().stream().map(SaleOrderLine::getVatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = saleOrder.getOrderLines().stream().map(SaleOrderLine::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = saleOrder.getOrderLines().stream().map(SaleOrderLine::getCostTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProfit = saleOrder.getOrderLines().stream().map(SaleOrderLine::getProfit).reduce(BigDecimal.ZERO, BigDecimal::add);

        saleOrder.setSubtotal(scale(subtotal));
        saleOrder.setTotalVat(scale(totalVat));
        saleOrder.setTotal(scale(total));
        saleOrder.setTotalCost(scale(totalCost));
        saleOrder.setTotalProfit(scale(totalProfit));
    }

    /**
     * Garantiza que todos los campos numéricos del turno sean non-null antes de operar.
     * Evita NullPointerException al acumular importes en el primer cobro del turno.
     */
    private void normalizeShiftTotals(CashRegisterShift shift) {
        if (shift.getCashSales() == null) shift.setCashSales(BigDecimal.ZERO);
        if (shift.getCardSales() == null) shift.setCardSales(BigDecimal.ZERO);
        if (shift.getOtherSales() == null) shift.setOtherSales(BigDecimal.ZERO);
        if (shift.getTotalSales() == null) shift.setTotalSales(BigDecimal.ZERO);
        if (shift.getTotalProfit() == null) shift.setTotalProfit(BigDecimal.ZERO);
    }

    /**
     * Acumula el importe de un cobro al turno de caja según el método de pago.
     * También suma el beneficio bruto del pedido al total del turno.
     */
    private void addPaymentToShift(CashRegisterShift shift, BigDecimal amount, PaymentMethod paymentMethod, BigDecimal orderProfit) {
        switch (paymentMethod) {
            case CASH -> shift.setCashSales(scale(shift.getCashSales().add(amount)));
            case CARD -> shift.setCardSales(scale(shift.getCardSales().add(amount)));
            default -> shift.setOtherSales(scale(shift.getOtherSales().add(amount)));
        }

        shift.setTotalSales(scale(shift.getTotalSales().add(amount)));
        shift.setTotalProfit(scale(shift.getTotalProfit().add(orderProfit != null ? orderProfit : BigDecimal.ZERO)));
    }

    /** Suma los importes de los cobros de una lista filtrados por método de pago. */
    private BigDecimal sumByMethod(List<Payment> payments, PaymentMethod paymentMethod) {
        return payments.stream()
                .filter(payment -> payment.getPaymentMethod() == paymentMethod)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Redondea un BigDecimal a 2 decimales con HALF_UP. 
     * Esto es  para que los importes se muestren siempre con 2 decimales en la interfaz, 
     * evitando problemas de formato o redondeo en la presentación.
     */
    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Normaliza el nombre de usuario: elimina espacios y convierte a minúsculas. Devuelve null si está vacío. */
    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return username.trim().toLowerCase();
    }

    /** Normaliza el token de sesión: elimina espacios. Devuelve null si está vacío. */
    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return token.trim();
    }

    private boolean isAdminRole(String role) {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }
}









