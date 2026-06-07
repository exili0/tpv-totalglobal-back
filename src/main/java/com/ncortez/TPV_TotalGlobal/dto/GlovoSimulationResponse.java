package com.ncortez.TPV_TotalGlobal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta de una simulación Glovo con ticket generado en TPV.
 */
public class GlovoSimulationResponse {
    private String glovoOrderId;
    private String orderCode;
    private Integer tableNumber;
    private String serviceLabel;
    private Long saleOrderId;
    private Long paymentId;
    private BigDecimal totalAmount;
    private String tpvPaymentMethod;
    private LocalDateTime paidAt;
    private boolean pendingCashPayment;
    private String message;

    public String getGlovoOrderId() {
        return glovoOrderId;
    }

    public void setGlovoOrderId(String glovoOrderId) {
        this.glovoOrderId = glovoOrderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getServiceLabel() {
        return serviceLabel;
    }

    public void setServiceLabel(String serviceLabel) {
        this.serviceLabel = serviceLabel;
    }

    public Long getSaleOrderId() {
        return saleOrderId;
    }

    public void setSaleOrderId(Long saleOrderId) {
        this.saleOrderId = saleOrderId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getTpvPaymentMethod() {
        return tpvPaymentMethod;
    }

    public void setTpvPaymentMethod(String tpvPaymentMethod) {
        this.tpvPaymentMethod = tpvPaymentMethod;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public boolean isPendingCashPayment() {
        return pendingCashPayment;
    }

    public void setPendingCashPayment(boolean pendingCashPayment) {
        this.pendingCashPayment = pendingCashPayment;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
