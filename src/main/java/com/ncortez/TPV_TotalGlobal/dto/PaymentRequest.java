package com.ncortez.TPV_TotalGlobal.dto;

import com.ncortez.TPV_TotalGlobal.entity.enums.PaymentMethod;

import java.math.BigDecimal;

/**
 * DTO para registrar el cobro de una orden.
 */
public class PaymentRequest {
    private Long saleOrderId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;

    public Long getSaleOrderId() { return saleOrderId; }
    public void setSaleOrderId(Long saleOrderId) { this.saleOrderId = saleOrderId; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
