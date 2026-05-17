package com.ncortez.TPV_TotalGlobal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Respuesta del reporte Z diario con ventas y beneficio.
 */
public class DailyZReportResponse {
    private LocalDate date;
    private int ticketsCount;
    private BigDecimal totalSales;
    private BigDecimal totalVat;
    private BigDecimal totalCost;
    private BigDecimal totalProfit;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private BigDecimal otherSales;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public int getTicketsCount() { return ticketsCount; }
    public void setTicketsCount(int ticketsCount) { this.ticketsCount = ticketsCount; }

    public BigDecimal getTotalSales() { return totalSales; }
    public void setTotalSales(BigDecimal totalSales) { this.totalSales = totalSales; }

    public BigDecimal getTotalVat() { return totalVat; }
    public void setTotalVat(BigDecimal totalVat) { this.totalVat = totalVat; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public BigDecimal getTotalProfit() { return totalProfit; }
    public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }

    public BigDecimal getCashSales() { return cashSales; }
    public void setCashSales(BigDecimal cashSales) { this.cashSales = cashSales; }

    public BigDecimal getCardSales() { return cardSales; }
    public void setCardSales(BigDecimal cardSales) { this.cardSales = cardSales; }

    public BigDecimal getOtherSales() { return otherSales; }
    public void setOtherSales(BigDecimal otherSales) { this.otherSales = otherSales; }
}
