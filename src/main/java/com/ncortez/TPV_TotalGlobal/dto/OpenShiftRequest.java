package com.ncortez.TPV_TotalGlobal.dto;

import java.math.BigDecimal;

/**
 * DTO para abrir un turno de caja.
 */
public class OpenShiftRequest {
    private BigDecimal openingFloat;
    private String openedBy;

    public BigDecimal getOpeningFloat() { return openingFloat; }
    public void setOpeningFloat(BigDecimal openingFloat) { this.openingFloat = openingFloat; }

    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
}
