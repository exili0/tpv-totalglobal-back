package com.ncortez.TPV_TotalGlobal.dto;

import com.ncortez.TPV_TotalGlobal.entity.enums.PaymentMethod;

import java.math.BigDecimal;

/**
 * DTO para registrar el cobro de una orden.
 */
public class PaymentRequest {
    private Long saleOrderId;
    private PaymentMethod paymentMethod;
    /** Importe del ticket que se va a cobrar, calculado a partir de la orden. */
    private BigDecimal amount;
    /** Importe físico entregado por el cliente; solo tiene sentido en efectivo. */
    private BigDecimal receivedAmount;

    public Long getSaleOrderId() { return saleOrderId; }
    public void setSaleOrderId(Long saleOrderId) { this.saleOrderId = saleOrderId; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
}
