package com.ncortez.TPV_TotalGlobal.dto;

import java.math.BigDecimal;

/**
 * Línea de detalle para la visualización de un ticket.
 */
public class TicketLineResponse {

    private Long lineId;
    private String productName;
    private Integer quantity;
    private Integer refundedQuantity;
    private Integer refundableQuantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public Long getLineId() { return lineId; }

    public void setLineId(Long lineId) { this.lineId = lineId; }

    public String getProductName() { return productName; }

    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getRefundedQuantity() { return refundedQuantity; }

    public void setRefundedQuantity(Integer refundedQuantity) { this.refundedQuantity = refundedQuantity; }

    public Integer getRefundableQuantity() { return refundableQuantity; }

    public void setRefundableQuantity(Integer refundableQuantity) { this.refundableQuantity = refundableQuantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }

    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getLineTotal() { return lineTotal; }

    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
