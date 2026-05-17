package com.ncortez.TPV_TotalGlobal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro de pérdidas de stock (desechos por devolución).
 * Cuando un producto se devuelve y NO regresa al stock, se registra como desperdicio.
 * Esto genera estadísticas valiosas sobre pérdidas reales.
 */
@Entity
@Table(name = "stock_waste")
public class StockWaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id")
    private Refund refund;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 250)
    private String reason; // Ejemplo: "Producto defectuoso", "Cambio de menú", etc.

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StockWaste() {}

    public StockWaste(Product product, Integer quantity, String reason) {
        this.product = product;
        this.quantity = quantity;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Refund getRefund() { return refund; }
    public void setRefund(Refund refund) { this.refund = refund; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
