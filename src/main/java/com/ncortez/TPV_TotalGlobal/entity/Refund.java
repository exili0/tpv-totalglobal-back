package com.ncortez.TPV_TotalGlobal.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de una devolución realizada sobre un cobro ya registrado.
 */
@Entity
@Table(name = "refunds")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_id", nullable = false)
    @JsonIgnoreProperties({"orderLines"})
    private SaleOrder saleOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    @JsonIgnoreProperties({"saleOrder"})
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_order_line_id")
    @JsonIgnoreProperties({"saleOrder", "product"})
    private SaleOrderLine saleOrderLine;

    @Column(name = "refunded_quantity")
    private Integer refundedQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 250)
    private String reason;

    @Column(nullable = false, length = 120)
    private String refundedBy;

    @Column(nullable = false)
    private LocalDateTime refundedAt = LocalDateTime.now();

    // Indica si el producto devuelto regresa al stock (true) o es considerado desperdicio (false)
    @Column(nullable = false)
    private boolean returnToStock = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SaleOrder getSaleOrder() { return saleOrder; }
    public void setSaleOrder(SaleOrder saleOrder) { this.saleOrder = saleOrder; }

    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }

    public SaleOrderLine getSaleOrderLine() { return saleOrderLine; }
    public void setSaleOrderLine(SaleOrderLine saleOrderLine) { this.saleOrderLine = saleOrderLine; }

    public Integer getRefundedQuantity() { return refundedQuantity; }
    public void setRefundedQuantity(Integer refundedQuantity) { this.refundedQuantity = refundedQuantity; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRefundedBy() { return refundedBy; }
    public void setRefundedBy(String refundedBy) { this.refundedBy = refundedBy; }

    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }

    public boolean isReturnToStock() { return returnToStock; }
    public void setReturnToStock(boolean returnToStock) { this.returnToStock = returnToStock; }
}