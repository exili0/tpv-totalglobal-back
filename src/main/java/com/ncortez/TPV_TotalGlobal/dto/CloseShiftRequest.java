package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO para cerrar el turno de caja activo.
 */
public class CloseShiftRequest {
    private String closedBy;

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
}
