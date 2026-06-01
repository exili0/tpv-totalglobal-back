package com.ncortez.TPV_TotalGlobal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Petición para registrar una devolución sobre un cobro existente.
 */
public class RefundRequest {

    private Long paymentId;
    private Long saleOrderLineId;
    private Integer quantity;
    private BigDecimal amount;
    private String reason;
    private String refundedBy;
    private String idempotencyKey;
    private LocalDateTime clientAttemptAt;

    /**
     * Indica si el producto devuelto regresa al stock (true) o es considerado desperdicio (false).
     * Por defecto es true para mantener compatibilidad.
     */
    private Boolean returnToStock = true;

    /**
     * @return Identificador del cobro que se devuelve.
     */
    public Long getPaymentId() { return paymentId; }

    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    /**
     * @return Identificador de línea de ticket para devoluciones por producto.
     */
    public Long getSaleOrderLineId() { return saleOrderLineId; }

    public void setSaleOrderLineId(Long saleOrderLineId) { this.saleOrderLineId = saleOrderLineId; }

    /**
     * @return Cantidad de unidades a devolver para la línea seleccionada.
     */
    public Integer getQuantity() { return quantity; }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    /**
     * @return Importe de la devolución.
     */
    public BigDecimal getAmount() { return amount; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }

    /**
     * @return Motivo de la devolución.
     */
    public String getReason() { return reason; }

    public void setReason(String reason) { this.reason = reason; }

    /**
     * @return Usuario que registra la devolución.
     */
    public String getRefundedBy() { return refundedBy; }

    public void setRefundedBy(String refundedBy) { this.refundedBy = refundedBy; }

    /**
     * @return Clave de idempotencia enviada por cliente para evitar duplicados por reintento.
     */
    public String getIdempotencyKey() { return idempotencyKey; }

    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    /**
     * @return Marca temporal del intento de devolución en cliente.
     */
    public LocalDateTime getClientAttemptAt() { return clientAttemptAt; }

    public void setClientAttemptAt(LocalDateTime clientAttemptAt) { this.clientAttemptAt = clientAttemptAt; }

    /**
     * @return Indica si el producto regresa al stock (true) o es considerado desperdicio (false).
     */
    public Boolean getReturnToStock() { return returnToStock; }

    public void setReturnToStock(Boolean returnToStock) { this.returnToStock = returnToStock != null ? returnToStock : true; }
}