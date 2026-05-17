package com.ncortez.TPV_TotalGlobal.dto;

import java.math.BigDecimal;

/**
 * DTO para crear o actualizar productos en el TPV.
 * Compatible con hostelaría (sin stock) y retail (con stock y código de barras).
 */
public class ProductRequest {

    /** Nombre del producto */
    private String name;
    /** Descripción detallada del producto */
    private String description;
    /** Precio base sin IVA */
    private BigDecimal price;
    /** Coste unitario base para cálculo de beneficio */
    private BigDecimal costPrice;
    /** Porcentaje de IVA aplicable (por defecto 21) */
    private int vatPercent = 21;
    /** Código de barras único del producto (nullable para hostelía) */
    private String barcode;
    /** URL o ruta de la imagen del producto */
    private String imageUrl;
    /** Stock disponible (nullable si no aplica) */
    private Integer stock;
    /** ID de la categoría a la que pertenece el producto */
    private Long categoryId;

    /**
     * Obtiene el nombre del producto.
     * @return Nombre del producto
     */
    public String getName() { return name; }
    /**
     * Establece el nombre del producto.
     * @param name Nombre del producto
     */
    public void setName(String name) { this.name = name; }

    /**
     * Obtiene la descripción del producto.
     * @return Descripción
     */
    public String getDescription() { return description; }
    /**
     * Establece la descripción del producto.
     * @param description Descripción
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Obtiene el precio base sin IVA.
     * @return Precio
     */
    public BigDecimal getPrice() { return price; }
    /**
     * Establece el precio base sin IVA.
     * @param price Precio
     */
    public void setPrice(BigDecimal price) { this.price = price; }

    /**
     * Obtiene el coste unitario base.
     * @return Coste unitario
     */
    public BigDecimal getCostPrice() { return costPrice; }
    /**
     * Establece el coste unitario base.
     * @param costPrice Coste unitario
     */
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    /**
     * Obtiene el porcentaje de IVA aplicable.
     * @return Porcentaje de IVA
     */
    public int getVatPercent() { return vatPercent; }
    /**
     * Establece el porcentaje de IVA aplicable.
     * @param vatPercent Porcentaje de IVA
     */
    public void setVatPercent(int vatPercent) { this.vatPercent = vatPercent; }

    /**
     * Obtiene el código de barras del producto.
     * @return Código de barras
     */
    public String getBarcode() { return barcode; }
    /**
     * Establece el código de barras del producto.
     * @param barcode Código de barras
     */
    public void setBarcode(String barcode) { this.barcode = barcode; }

    /**
     * Obtiene la URL de la imagen del producto.
     * @return URL de la imagen
     */
    public String getImageUrl() { return imageUrl; }
    /**
     * Establece la URL de la imagen del producto.
     * @param imageUrl URL de la imagen
     */
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /**
     * Obtiene el stock disponible del producto.
     * @return Stock
     */
    public Integer getStock() { return stock; }
    /**
     * Establece el stock disponible del producto.
     * @param stock Stock
     */
    public void setStock(Integer stock) { this.stock = stock; }

    /**
     * Obtiene el ID de la categoría del producto.
     * @return ID de la categoría
     */
    public Long getCategoryId() { return categoryId; }
    /**
     * Establece el ID de la categoría del producto.
     * @param categoryId ID de la categoría
     */
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
