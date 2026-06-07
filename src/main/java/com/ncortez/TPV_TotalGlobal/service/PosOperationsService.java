package com.ncortez.TPV_TotalGlobal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncortez.TPV_TotalGlobal.dto.CloseShiftRequest;
import com.ncortez.TPV_TotalGlobal.dto.CreateOrderRequest;
import com.ncortez.TPV_TotalGlobal.dto.DailyZReportResponse;
import com.ncortez.TPV_TotalGlobal.dto.GlovoSimulatedOrderRequest;
import com.ncortez.TPV_TotalGlobal.dto.GlovoSimulationResponse;
import com.ncortez.TPV_TotalGlobal.dto.OpenShiftRequest;
import com.ncortez.TPV_TotalGlobal.dto.OrderItemRequest;
import com.ncortez.TPV_TotalGlobal.dto.PaymentRequest;
import com.ncortez.TPV_TotalGlobal.dto.RefundRequest;
import com.ncortez.TPV_TotalGlobal.dto.ShiftDetailResponse;
import com.ncortez.TPV_TotalGlobal.dto.ShiftProductSaleResponse;
import com.ncortez.TPV_TotalGlobal.dto.TableRequest;
import com.ncortez.TPV_TotalGlobal.dto.TicketDetailResponse;
import com.ncortez.TPV_TotalGlobal.dto.TicketLineResponse;
import com.ncortez.TPV_TotalGlobal.dto.TicketSummaryResponse;
import com.ncortez.TPV_TotalGlobal.exception.RefundConflictException;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Autowired
    private ObjectMapper objectMapper;

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
        if (request.getTableNumber() < 0) {
            throw new RuntimeException("El nÃƒÂºmero de mesa no puede ser negativo");
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
        if (tableNumber < 0) {
            throw new RuntimeException("El nÃƒÂºmero de mesa no puede ser negativo");
        }

        ensureOpenShiftForOperations(); // Se asegura de que haya un turno abierto antes de permitir operaciones en mesas

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

        if (tableNumber != 0 && !isGlovoTable(table)) {
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
        if (tableNumber < 0) {
            throw new RuntimeException("El nÃƒÂºmero de mesa no puede ser negativo");
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
        if (tableNumberValue == null) {
            // Evita comportamiento silencioso (null -> 0/Barra) cuando falta mesa en el request.
            throw new RuntimeException("El nÃƒÂºmero de mesa es obligatorio");
        }
        final int tableNumber = tableNumberValue;
        if (tableNumber < 0) {
            throw new RuntimeException("El nÃƒÂºmero de mesa no puede ser negativo");
        }
        String operatorUsername = normalizeUsername(request.getOperatorUsername());
        String operatorSessionToken = normalizeToken(request.getOperatorSessionToken());

        BusinessTable table = businessTableRepository.findByTableNumber(tableNumber)
                .orElseThrow(() -> new RuntimeException("Mesa no encontrada: " + tableNumber));

        if (!table.isActive() || table.getStatus() == TableStatus.INACTIVE) {
            throw new RuntimeException("La mesa no estÃƒÂ¡ operativa");
        }
        // Bloqueo de concurrencia optimista: si la mesa ya tiene un pedido abierto con líneas,
        // no se puede modificar desde otro cliente
        Optional<SaleOrder> existingOpenOrder = saleOrderRepository.findFirstByTableAndStatus(table, OrderStatus.OPEN);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            // si el request viene sin líneas, se interpreta como cancelación del pedido abierto: se libera el stock reservado y se borra el pedido
            if (isGlovoTable(table) && existingOpenOrder.isPresent() && !existingOpenOrder.get().getOrderLines().isEmpty()) {
                throw new RuntimeException("Pedido Glovo bloqueado: no se puede eliminar ni vaciar el carrito");
            }

            return existingOpenOrder
                    .map(existingOrder -> {
                        releaseReservedStockForOrder(existingOrder);
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

        if (tableNumber != 0 && !isGlovoTable(table)) {
            // si la mesa no es la barra ni una mesa virtual Glovo, 
            // se aplica lógica de bloqueo por sesión para evitar que varios camareros modifiquen el mismo pedido al mismo tiempo
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

        SaleOrder saleOrder = existingOpenOrder
                .orElseGet(() -> {
                    SaleOrder order = new SaleOrder();
                    order.setTable(table);
                    order.setStatus(OrderStatus.OPEN);
                    return order;
                });

        Map<Long, Integer> existingQuantitiesByProduct = aggregateOrderLineQuantities(saleOrder.getOrderLines());
        Map<Long, Product> existingProductsById = saleOrder.getOrderLines().stream()
                .filter(line -> line.getProduct() != null && line.getProduct().getId() != null)
                .collect(Collectors.toMap(
                        line -> line.getProduct().getId(),
                        SaleOrderLine::getProduct,
                        (first, second) -> first
                ));

        Map<Long, Integer> requestedQuantitiesByProduct = new LinkedHashMap<>();
        for (OrderItemRequest item : request.getItems()) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("Línea de pedido inválida");
            }
            requestedQuantitiesByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        if (isGlovoTable(table) && existingOpenOrder.isPresent() && !existingOpenOrder.get().getOrderLines().isEmpty()) {
            if (!existingQuantitiesByProduct.equals(requestedQuantitiesByProduct)) {
                throw new RuntimeException("Pedido Glovo bloqueado: no se pueden modificar sus productos");
            }
            // Permitimos cobrar con la misma cesta, pero seguimos el flujo normal de guardado
            // para evitar errores de serialización al devolver entidades parcialmente cargadas.
        }

        Map<Long, Product> requestedProductsById = new HashMap<>();
        for (Long productId : requestedQuantitiesByProduct.keySet()) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productId));
            requestedProductsById.put(productId, product);
        }

        Set<Long> affectedProductIds = new java.util.HashSet<>();
        affectedProductIds.addAll(existingQuantitiesByProduct.keySet());
        affectedProductIds.addAll(requestedQuantitiesByProduct.keySet());

        // Regla clave: el stock se ajusta comparando lo que había antes con lo que pide el carrito ahora.
        // - Si el carrito pide MÁS que antes, se descuenta solo la cantidad extra añadida.
        // - Si el carrito pide MENOS que antes (o elimina el producto), se devuelve solo la cantidad quitada.
        // Así evitamos descontar el stock dos veces cuando el front envía la orden completa al modificar el carrito.
        for (Long productId : affectedProductIds) {
            int existingQuantity = existingQuantitiesByProduct.getOrDefault(productId, 0);
            int requestedQuantity = requestedQuantitiesByProduct.getOrDefault(productId, 0);

            if (requestedQuantity > existingQuantity) {
                int additionalQuantity = requestedQuantity - existingQuantity;
                Product product = requestedProductsById.get(productId);
                try {
                    stockService.deductStockForSale(product, additionalQuantity, saleOrder.getId());
                } catch (RuntimeException e) {
                    throw new RuntimeException("Error de stock: " + e.getMessage());
                }
            } else if (requestedQuantity < existingQuantity) {
                int returnedQuantity = existingQuantity - requestedQuantity;
                Product product = existingProductsById.getOrDefault(productId, requestedProductsById.get(productId));
                stockService.releaseReservedStockFromOrder(product, returnedQuantity, saleOrder.getId());
            }
        }

        saleOrder.getOrderLines().clear();

        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            saleOrder.setNotes(request.getNotes().trim()); 
            // actualiza notas aunque no cambien las líneas, para permitir editar solo las notas sin tocar el carrito
        }

        for (Map.Entry<Long, Integer> item : requestedQuantitiesByProduct.entrySet()) {
            Product product = requestedProductsById.get(item.getKey());
            SaleOrderLine orderLine = buildOrderLine(saleOrder, product, item.getValue());
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
        if (tableNumber < 0) {
            throw new RuntimeException("El nÃƒÂºmero de mesa no puede ser negativo");
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

        if (isGlovoTable(table)) {
            throw new RuntimeException("Pedido Glovo bloqueado: no se puede limpiar el carrito desde TPV");
        }

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
                .ifPresent(order -> {
                    // Al cancelar pedido abierto se revierte toda la reserva de stock de sus líneas.
                    releaseReservedStockForOrder(order);
                    saleOrderRepository.delete(order);
                });

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
        response.setCollectedBy(payment.getCollectedBy());
        response.setTipAmount(scale(payment.getTipAmount() != null ? payment.getTipAmount() : BigDecimal.ZERO));

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
        detail.setCollectedBy(summary.getCollectedBy());
        detail.setTipAmount(summary.getTipAmount());
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

        String displayName = trimToNull(saleOrder.getTable().getDisplayName());
        if (displayName != null && displayName.toUpperCase().startsWith("GLOVO ")) {
            return displayName;
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

        String normalizedReason = request.getReason() != null ? request.getReason().trim() : null;
        if (normalizedReason != null && normalizedReason.isBlank()) {
            normalizedReason = null;
        }

        boolean shouldReturnToStock = !Boolean.FALSE.equals(request.getReturnToStock());
        if (!shouldReturnToStock && (normalizedReason == null || normalizedReason.isBlank())) {
            throw new RuntimeException("El motivo es obligatorio cuando la devolución no regresa al stock");
        }

        String idempotencyKey = normalizeIdempotencyKey(request.getIdempotencyKey());
        if (idempotencyKey != null) {
            Optional<Refund> existingRefund = refundRepository.findFirstByPaymentIdAndIdempotencyKey(request.getPaymentId(), idempotencyKey);
            if (existingRefund.isPresent()) {
                return existingRefund.get();
            }
        }

        Payment payment = paymentRepository.findByIdForUpdate(request.getPaymentId())
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
        refund.setReason(normalizedReason);
        refund.setRefundedBy(normalizeUsername(request.getRefundedBy()));
        refund.setIdempotencyKey(idempotencyKey);
        refund.setClientAttemptAt(request.getClientAttemptAt());
        
        // Establecer si el producto regresa al stock o es considerado desecho
        refund.setReturnToStock(shouldReturnToStock);

        Refund savedRefund;
        try {
            // saveAndFlush reduce ventana de carrera para duplicados con misma idempotency key.
            savedRefund = refundRepository.saveAndFlush(refund);
        } catch (DataIntegrityViolationException ex) {
            if (idempotencyKey != null) {
                Optional<Refund> duplicatedRefund = refundRepository.findFirstByPaymentIdAndIdempotencyKey(payment.getId(), idempotencyKey);
                if (duplicatedRefund.isPresent()) {
                    return duplicatedRefund.get();
                }
            }
            throw ex;
        }

        // Manejar el stock según la opción de devolución
        if (refundedLine != null && refundedQuantity != null) {
            Product product = refundedLine.getProduct();
            
            if (shouldReturnToStock) {
                // El producto regresa al stock
                stockService.returnStockFromRefund(product, refundedQuantity, savedRefund.getId());
            } else {
                // El producto se considera desecho (pérdida)
                String wasteReason = normalizedReason != null ? normalizedReason : "Devolución sin retorno a stock";
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
        CashRegisterShift openShift = ensureOpenShiftForOperations();
        // amount conserva el valor real del ticket; receivedAmount solo diferencia el efectivo entregado.
        BigDecimal amount = saleOrder.getTotal();
        BigDecimal tipAmount = request.getTipAmount() != null ? request.getTipAmount() : BigDecimal.ZERO;
        if (tipAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("La propina no puede ser negativa");
        }

        BigDecimal totalToCollect = amount.add(tipAmount);
        BigDecimal receivedAmount = request.getReceivedAmount() != null ? request.getReceivedAmount() : totalToCollect;

        if (paymentMethod == PaymentMethod.CASH && receivedAmount.compareTo(totalToCollect) < 0) {
            throw new RuntimeException("El importe entregado en efectivo no puede ser inferior al total del ticket más propina");
        }

        if (paymentMethod != PaymentMethod.CASH) {
            receivedAmount = totalToCollect;
        }

        String collectedBy = normalizeUsername(request.getCashierUsername());
        if (collectedBy == null && saleOrder.getTable() != null) {
            collectedBy = normalizeUsername(saleOrder.getTable().getAttendedBy());
        }

        Payment payment = new Payment();
        payment.setSaleOrder(saleOrder);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setTipAmount(scale(tipAmount));
        payment.setReceivedAmount(receivedAmount);
        payment.setCollectedBy(collectedBy);
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
            // si es una mesa de integración (Glovo), al quedar libre se desactiva para que no vuelva a salir en el selector de mesas,
            //  ya que solo se usa para pedidos online.
            if (isGlovoTable(table)) {
                // Mesa virtual de integración: una vez cobrada deja de estar disponible en selector.
                table.setActive(false);
            }
            businessTableRepository.save(table);
        }

        normalizeShiftTotals(openShift);
        addPaymentToShift(openShift, amount, paymentMethod, saleOrder.getTotalProfit());
        cashRegisterShiftRepository.save(openShift);

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

    /** Devuelve el turno de caja abierto actualmente, si existe. */
    public Optional<CashRegisterShift> getCurrentOpenShift() {
        return cashRegisterShiftRepository.findFirstByStatusOrderByOpenedAtDesc(CashShiftStatus.OPEN);
    }

    /**
     * Recupera el histórico de turnos de caja para la vista administrativa de ganancias.
     *
     * Reglas de filtrado:
     * - Sin fechas: devuelve todo el histórico.
     * - Solo 'startDate': devuelve desde las 00:00 de esa fecha en adelante.
     * - Solo 'endDate': devuelve hasta las 23:59:59.999 de esa fecha.
     * - Ambas fechas: devuelve aperturas dentro del rango completo (inclusive).
     *
     * @param startDate fecha inicial opcional
     * @param endDate fecha final opcional
     * @return lista de turnos ordenada por apertura descendente
     * @throws RuntimeException si el rango es inválido (startDate mayor que endDate)
     */
    public List<CashRegisterShift> getShiftHistory(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new RuntimeException("La fecha inicial no puede ser posterior a la fecha final");
        }

        if (startDate == null && endDate == null) {
            return cashRegisterShiftRepository.findAllByOrderByOpenedAtDesc();
        }

        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);
            return cashRegisterShiftRepository.findByOpenedAtBetweenOrderByOpenedAtDesc(start, end);
        }

        if (startDate != null) {
            return cashRegisterShiftRepository
                    .findByOpenedAtGreaterThanEqualOrderByOpenedAtDesc(startDate.atStartOfDay());
        }

        LocalDate safeEndDate = endDate != null ? endDate : LocalDate.now();
        LocalDateTime end = safeEndDate.plusDays(1).atStartOfDay().minusNanos(1);
        return cashRegisterShiftRepository.findByOpenedAtLessThanEqualOrderByOpenedAtDesc(end);
    }

    /**
     * Devuelve el detalle de un turno de caja con el acumulado de productos vendidos.
     */
    @Transactional(readOnly = true)
    public ShiftDetailResponse getShiftDetail(Long shiftId) {
        if (shiftId == null) {
            throw new RuntimeException("El identificador del turno es obligatorio");
        }

        CashRegisterShift shift = cashRegisterShiftRepository.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Turno de caja no encontrado"));

        LocalDateTime start = shift.getOpenedAt();
        LocalDateTime end = shift.getClosedAt() != null ? shift.getClosedAt() : LocalDateTime.now();
        List<Payment> payments = paymentRepository.findByPaidAtBetweenWithOrderLinesAndProducts(start, end);

        Map<Long, Integer> snapshotMap = parseClosingStockSnapshot(shift.getClosingStockSnapshot());
        Map<Long, ProductAccumulator> productTotals = new HashMap<>();

        for (Payment payment : payments) {
            if (payment.getSaleOrder() == null || payment.getSaleOrder().getOrderLines() == null) {
                continue;
            }

            for (SaleOrderLine line : payment.getSaleOrder().getOrderLines()) {
                Long productId = line.getProduct() != null ? line.getProduct().getId() : null;
                if (productId == null) {
                    continue;
                }

                ProductAccumulator accumulator = productTotals.computeIfAbsent(productId, id -> {
                    ProductAccumulator created = new ProductAccumulator();
                    created.productId = id;
                    created.productName = line.getProductName();
                    created.quantitySold = 0;
                    created.totalSales = BigDecimal.ZERO;
                    created.totalProfit = BigDecimal.ZERO;
                    return created;
                });

                Integer lineQuantity = line.getQuantity();
                int quantity = lineQuantity != null ? lineQuantity : 0;
                accumulator.quantitySold += quantity;
                accumulator.totalSales = accumulator.totalSales.add(line.getSubtotal() != null ? line.getSubtotal() : BigDecimal.ZERO);
                accumulator.totalProfit = accumulator.totalProfit.add(line.getProfit() != null ? line.getProfit() : BigDecimal.ZERO);
            }
        }

        List<ShiftProductSaleResponse> soldProducts = productTotals.values().stream()
                .map(total -> {
                    ShiftProductSaleResponse response = new ShiftProductSaleResponse();
                    response.setProductId(total.productId);
                    response.setProductName(total.productName);
                    response.setQuantitySold(total.quantitySold);
                    response.setTotalSales(scale(total.totalSales));
                    response.setTotalProfit(scale(total.totalProfit));
                    response.setStockAtClose(snapshotMap.get(total.productId));
                    return response;
                })
                .sorted(Comparator.comparing(
                        ShiftProductSaleResponse::getProductName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();

        ShiftDetailResponse detail = new ShiftDetailResponse();
        detail.setShift(shift);
        detail.setSoldProducts(soldProducts);
        return detail;
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
        List<SaleOrder> pendingOrders = saleOrderRepository.findByStatus(OrderStatus.OPEN).stream()
            .filter(order -> order.getOrderLines() != null && !order.getOrderLines().isEmpty())
            .toList();

        if (!pendingOrders.isEmpty()) {
            String pendingTablesLabel = pendingOrders.stream()
                .map(this::getServiceLabel)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
            throw new RuntimeException("No se puede cerrar la caja: hay servicios sin pagar (" + pendingTablesLabel + ")");
        }

        CashRegisterShift shift = cashRegisterShiftRepository.findFirstByStatusOrderByOpenedAtDesc(CashShiftStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("No hay un turno de caja abierto"));

        LocalDateTime closedAt = LocalDateTime.now();
        shift.setClosingStockSnapshot(buildClosingStockSnapshot(shift.getOpenedAt(), closedAt));
        shift.setStatus(CashShiftStatus.CLOSED);
        shift.setClosedAt(closedAt);
        shift.setClosedBy(request != null ? request.getClosedBy() : null);

        return cashRegisterShiftRepository.save(shift);
    }

    private CashRegisterShift ensureOpenShiftForOperations() {
        return cashRegisterShiftRepository.findFirstByStatusOrderByOpenedAtDesc(CashShiftStatus.OPEN)
                .orElseThrow(() -> new RuntimeException("No hay un turno de caja abierto. Abre el turno para operar mesas y cobros"));
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
    ///////////// SIMULACIÓN DE PEDIDOS GLOVO //////////
    /**
     * Simula la entrada de un pedido despachado de Glovo.
     *
     * - Crea una mesa virtual "Glovo N" para el pedido.
     * - Si el pago es CASH, deja el pedido abierto para cobro manual posterior.
     * - Si no es CASH (ej. DELAYED), genera ticket de forma automática.
     */
    @Transactional
    public GlovoSimulationResponse simulateGlovoOrder(GlovoSimulatedOrderRequest request) {
        if (request == null) {
            throw new RuntimeException("El cuerpo de simulación es obligatorio");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Debes incluir al menos un producto para simular el pedido");
        }

        BusinessTable glovoTable = createGlovoVirtualTable();

        List<OrderItemRequest> mappedItems = request.getItems().stream().map(item -> {
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("Todas las líneas del pedido Glovo deben tener productId y quantity válidos");
            }
            OrderItemRequest orderItem = new OrderItemRequest();
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            return orderItem;
        }).collect(Collectors.toList());

        CreateOrderRequest createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setTableNumber(glovoTable.getTableNumber());
        createOrderRequest.setItems(mappedItems);
        createOrderRequest.setOperatorUsername(resolveSimulationOperator(request));
        createOrderRequest.setOperatorSessionToken("glovo-simulator-session");
        createOrderRequest.setNotes(buildGlovoOrderNotes(request));

        SaleOrder saleOrder = openOrUpdateOrder(createOrderRequest);

        PaymentMethod paymentMethod = mapGlovoPaymentMethod(request.getPaymentMethod());
        Payment payment = null;

        if (paymentMethod != PaymentMethod.CASH) {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setSaleOrderId(saleOrder.getId());
            paymentRequest.setPaymentMethod(paymentMethod);
            paymentRequest.setAmount(saleOrder.getTotal());
            paymentRequest.setReceivedAmount(saleOrder.getTotal());
            paymentRequest.setCashierUsername(resolveSimulationOperator(request));
            payment = registerPayment(paymentRequest);
        }

        GlovoSimulationResponse response = new GlovoSimulationResponse();
        response.setGlovoOrderId(trimToNull(request.getGlovoOrderId()));
        response.setOrderCode(trimToNull(request.getOrderCode()));
        response.setTableNumber(glovoTable.getTableNumber());
        response.setServiceLabel(glovoTable.getDisplayName());
        response.setSaleOrderId(saleOrder.getId());
        response.setPendingCashPayment(paymentMethod == PaymentMethod.CASH);
        response.setTotalAmount(scale(saleOrder.getTotal()));
        response.setTpvPaymentMethod(paymentMethod.name());

        if (payment != null) {
            response.setPaymentId(payment.getId());
            response.setPaidAt(payment.getPaidAt());
            response.setMessage("Pedido Glovo simulado y ticket generado correctamente");
        } else {
            response.setPaymentId(null);
            response.setPaidAt(null);
            response.setMessage("Pedido Glovo creado en CASH pendiente de cobro manual");
        }

        return response;
    }

    private BusinessTable createGlovoVirtualTable() {
        int nextGlovoTableNumber = businessTableRepository
                .findFirstByTableNumberGreaterThanEqualOrderByTableNumberDesc(1000)
                .map(existing -> existing.getTableNumber() + 1)
                .orElse(1000);

        int nextGlovoOrdinal = businessTableRepository
                .findFirstByDisplayNameStartingWithOrderByTableNumberDesc("Glovo ")
                .map(existing -> {
                    String displayName = trimToNull(existing.getDisplayName());
                    if (displayName == null) {
                        return 1;
                    }

                    String numericPart = displayName.replaceFirst("(?i)^glovo\\s+", "").trim();
                    try {
                        return Integer.parseInt(numericPart) + 1;
                    } catch (NumberFormatException ex) {
                        return 1;
                    }
                })
                .orElse(1);

        BusinessTable glovoTable = new BusinessTable(nextGlovoTableNumber, "Glovo " + nextGlovoOrdinal, 1);
        glovoTable.setStatus(TableStatus.OCCUPIED);
        glovoTable.setActive(true);
        glovoTable.setAttendedBy(null);
        glovoTable.setLockedAt(null);
        glovoTable.setLockToken(null);
        return businessTableRepository.save(glovoTable);
    }

    private String resolveSimulationOperator(GlovoSimulatedOrderRequest request) {
        String normalizedOperator = normalizeUsername(request.getOperatorUsername());
        return normalizedOperator != null ? normalizedOperator : "glovo-simulator";
    }

    private String buildGlovoOrderNotes(GlovoSimulatedOrderRequest request) {
        String glovoOrderId = trimToNull(request.getGlovoOrderId());
        String orderCode = trimToNull(request.getOrderCode());
        String storeId = trimToNull(request.getStoreId());
        String customerName = trimToNull(request.getCustomerName());
        String specialRequirements = trimToNull(request.getSpecialRequirements());

        return Stream.of(
                        "[GLOVO-SIM]",
                        glovoOrderId != null ? "order_id=" + glovoOrderId : null,
                        orderCode != null ? "order_code=" + orderCode : null,
                        storeId != null ? "store_id=" + storeId : null,
                        customerName != null ? "customer=" + customerName : null,
                        specialRequirements != null ? "special_requirements=" + specialRequirements : null
                )
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" | "));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PaymentMethod mapGlovoPaymentMethod(String glovoPaymentMethod) {
        if (glovoPaymentMethod == null) {
            return PaymentMethod.OTHER;
        }

        String normalized = glovoPaymentMethod.trim().toUpperCase();
        if ("CASH".equals(normalized)) {
            return PaymentMethod.CASH;
        }

        // DELAYED y cualquier otro método de Glovo se registran como OTHER en TPV.
        return PaymentMethod.OTHER;
    }

    private boolean isGlovoTable(BusinessTable table) {
        if (table == null) {
            return false;
        }

        String displayName = trimToNull(table.getDisplayName());
        return displayName != null && displayName.toUpperCase().startsWith("GLOVO ");
    }
    ///////////////////////
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
     * Acumula cantidades por producto en una lista de líneas de pedido.
     * Se usa para comparar estado anterior vs. nuevo de una orden y calcular el DELTA de stock.
     * Delta es la diferencia entre el valor inicial y el valor final*
     */
    private Map<Long, Integer> aggregateOrderLineQuantities(List<SaleOrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> totals = new HashMap<>();
        for (SaleOrderLine line : lines) {
            if (line.getProduct() == null || line.getProduct().getId() == null) {
                continue;
            }

            Long productId = line.getProduct().getId();
            Integer quantityValue = line.getQuantity();
            int quantity = quantityValue != null ? quantityValue : 0;
            totals.merge(productId, quantity, Integer::sum);
        }

        return totals;
    }

    /**
     * Libera la reserva de stock de todas las líneas de una orden abierta.
     * Se utiliza al cancelar una orden o al limpiar sus líneas antes de rearmarla con un nuevo estado.
     */
    private void releaseReservedStockForOrder(SaleOrder saleOrder) {
        if (saleOrder == null || saleOrder.getOrderLines() == null || saleOrder.getOrderLines().isEmpty()) {
            return;
        }

        for (SaleOrderLine line : saleOrder.getOrderLines()) {
            stockService.releaseReservedStockFromOrder(
                    line.getProduct(),
                    line.getQuantity(),
                    saleOrder.getId()
            );
        }
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

    /**
     * Construye un snapshot JSON {productId: stockAlCerrar} solo para productos vendidos en el turno.
     */
    private String buildClosingStockSnapshot(LocalDateTime start, LocalDateTime end) {
        List<Payment> payments = paymentRepository.findByPaidAtBetweenWithOrderLinesAndProducts(start, end);
        Set<Long> soldProductIds = payments.stream()
                .filter(payment -> payment.getSaleOrder() != null)
                .flatMap(payment -> {
                    List<SaleOrderLine> lines = payment.getSaleOrder().getOrderLines();
                    return lines != null ? lines.stream() : java.util.stream.Stream.empty();
                })
                .map(line -> line.getProduct() != null ? line.getProduct().getId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (soldProductIds.isEmpty()) {
            return "{}";
        }

        Map<Long, Integer> stockByProduct = productRepository.findAllById(soldProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getStock));

        try {
            return objectMapper.writeValueAsString(stockByProduct);
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo generar el snapshot de stock al cierre", ex);
        }
    }

    private Map<Long, Integer> parseClosingStockSnapshot(String rawSnapshot) {
        if (rawSnapshot == null || rawSnapshot.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(rawSnapshot, new TypeReference<Map<Long, Integer>>() {});
        } catch (IOException ex) {
            return Map.of();
        }
    }

    private static class ProductAccumulator {
        private Long productId;
        private String productName;
        private Integer quantitySold;
        private BigDecimal totalSales;
        private BigDecimal totalProfit;
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

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String normalized = idempotencyKey.trim();
        if (normalized.length() > 120) {
            throw new RefundConflictException("La clave de idempotencia excede la longitud permitida");
        }

        return normalized;
    }

    private boolean isAdminRole(String role) {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }
}









