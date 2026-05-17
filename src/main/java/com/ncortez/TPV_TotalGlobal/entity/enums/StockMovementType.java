package com.ncortez.TPV_TotalGlobal.entity.enums;

/**
 * Tipo de movimiento de stock registrado.
 */
public enum StockMovementType {
    SALE,      // Venta: se descuenta stock
    RETURN,    // Devolución: se suma stock
    ADJUSTMENT // Ajuste manual: entrada o salida
}
