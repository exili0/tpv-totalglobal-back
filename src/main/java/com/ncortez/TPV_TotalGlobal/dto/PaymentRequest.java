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
    /** Usuario de caja que registra el cobro. */
    private String cashierUsername;
    /** Propina opcional del cliente. */
    private BigDecimal tipAmount;

    public Long getSaleOrderId() { return saleOrderId; }
    public void setSaleOrderId(Long saleOrderId) { this.saleOrderId = saleOrderId; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }

    public String getCashierUsername() { return cashierUsername; }
    public void setCashierUsername(String cashierUsername) { this.cashierUsername = cashierUsername; }

    public BigDecimal getTipAmount() { return tipAmount; }
    public void setTipAmount(BigDecimal tipAmount) { this.tipAmount = tipAmount; }
}
