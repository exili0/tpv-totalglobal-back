package com.ncortez.TPV_TotalGlobal.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Representa un producto vendible en el TPV.
 * Compatible con hostelería (sin stock) y retail (con stock y código de barras).
 */
@Entity
@Table(name = "products")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Evita problemas de serialización con relaciones perezosas
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false)
    private String name;

    private String description;

    // Precio base sin IVA
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // Coste unitario base para cálculo de beneficio
    @Column(precision = 10, scale = 2)
    private BigDecimal costPrice;

    // Porcentaje de IVA aplicable (ej. 21, 10, 4)
    @Column(nullable = false)
    private int vatPercent = 21;

    // Código de barras — solo para retail, nullable en hostelería
    @Column(unique = true)
    private String barcode;

    // URL o ruta de imagen del producto
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    // Stock disponible — null si no aplica (hostelería)
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"products", "subcategories", "parentCategory"})
    private Category category;

    public Product() {}

    public Product(String name, String description, BigDecimal price, int vatPercent, Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.vatPercent = vatPercent;
        this.category = category;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public int getVatPercent() { return vatPercent; }
    public void setVatPercent(int vatPercent) { this.vatPercent = vatPercent; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}
