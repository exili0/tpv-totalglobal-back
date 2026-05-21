package com.ncortez.TPV_TotalGlobal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ncortez.TPV_TotalGlobal.entity.enums.PaymentMethod;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de cobro asociado a una orden de venta.
 */
@Entity
@Table(name = "payments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id", nullable = false)
    @JsonIgnoreProperties({"orderLines"})
    private SaleOrder saleOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    /** Total del ticket cobrado; se usa para cierres de caja y para generar tickets. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Importe realmente recibido, necesario para registrar cambio en cobros en efectivo. */
    @Column(precision = 12, scale = 2)
    private BigDecimal receivedAmount;

    @Column(nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SaleOrder getSaleOrder() { return saleOrder; }
    public void setSaleOrder(SaleOrder saleOrder) { this.saleOrder = saleOrder; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
