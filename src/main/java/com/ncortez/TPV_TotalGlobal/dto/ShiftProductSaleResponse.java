package com.ncortez.TPV_TotalGlobal.dto;

import java.math.BigDecimal;

/**
 * Resumen de ventas por producto dentro de un turno de caja.
 */
public class ShiftProductSaleResponse {

    private Long productId;
    private String productName;
    private Integer quantitySold;
    private BigDecimal totalSales;
    private BigDecimal totalProfit;
    private Integer stockAtOpen;
    private Integer stockAtClose;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantitySold() { return quantitySold; }
    public void setQuantitySold(Integer quantitySold) { this.quantitySold = quantitySold; }

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }

    public BigDecimal getTotalProfit() { return totalProfit; }
    public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }

    public Integer getStockAtOpen() { return stockAtOpen; }
    public void setStockAtOpen(Integer stockAtOpen) { this.stockAtOpen = stockAtOpen; }

    public Integer getStockAtClose() { return stockAtClose; }
    public void setStockAtClose(Integer stockAtClose) { this.stockAtClose = stockAtClose; }
}
