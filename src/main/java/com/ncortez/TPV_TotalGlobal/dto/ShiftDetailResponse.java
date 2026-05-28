package com.ncortez.TPV_TotalGlobal.dto;

import com.ncortez.TPV_TotalGlobal.entity.CashRegisterShift;

import java.util.ArrayList;
import java.util.List;

/**
 * Detalle completo de turno con productos vendidos y snapshot de stock al cierre.
 */
public class ShiftDetailResponse {

    private CashRegisterShift shift;
    private List<ShiftProductSaleResponse> soldProducts = new ArrayList<>();

    public CashRegisterShift getShift() { return shift; }
    public void setShift(CashRegisterShift shift) { this.shift = shift; }

    public List<ShiftProductSaleResponse> getSoldProducts() { return soldProducts; }
    public void setSoldProducts(List<ShiftProductSaleResponse> soldProducts) { this.soldProducts = soldProducts; }
}
