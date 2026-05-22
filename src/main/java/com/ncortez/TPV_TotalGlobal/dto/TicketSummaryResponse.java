package com.ncortez.TPV_TotalGlobal.dto;

import com.ncortez.TPV_TotalGlobal.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Resumen de ticket para listados de cobros.
 */
public class TicketSummaryResponse {

    private Long paymentId;
    private Long saleOrderId;
    private Integer tableNumber;
    private String serviceLabel;
    private LocalDateTime paidAt;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private PaymentMethod paymentMethod;
    private String collectedBy;
    private BigDecimal tipAmount;
    private BigDecimal refundedAmount;
    private BigDecimal refundableAmount;

    public Long getPaymentId() { return paymentId; }

    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public Long getSaleOrderId() { return saleOrderId; }

    public void setSaleOrderId(Long saleOrderId) { this.saleOrderId = saleOrderId; }

    public Integer getTableNumber() { return tableNumber; }

    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }

    public String getServiceLabel() { return serviceLabel; }

    public void setServiceLabel(String serviceLabel) { this.serviceLabel = serviceLabel; }

    public LocalDateTime getPaidAt() { return paidAt; }

    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public BigDecimal getTotalAmount() { return totalAmount; }

    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Integer getTotalItems() { return totalItems; }

    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }

    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getCollectedBy() { return collectedBy; }

    public void setCollectedBy(String collectedBy) { this.collectedBy = collectedBy; }

    public BigDecimal getTipAmount() { return tipAmount; }

    public void setTipAmount(BigDecimal tipAmount) { this.tipAmount = tipAmount; }

    public BigDecimal getRefundedAmount() { return refundedAmount; }

    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }

    public BigDecimal getRefundableAmount() { return refundableAmount; }

    public void setRefundableAmount(BigDecimal refundableAmount) { this.refundableAmount = refundableAmount; }
}
