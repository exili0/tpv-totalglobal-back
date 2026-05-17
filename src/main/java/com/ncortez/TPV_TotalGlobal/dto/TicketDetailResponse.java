package com.ncortez.TPV_TotalGlobal.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Detalle completo de ticket para mostrar en modal.
 */
public class TicketDetailResponse extends TicketSummaryResponse {

    private String notes;
    private List<TicketLineResponse> lines = new ArrayList<>();

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public List<TicketLineResponse> getLines() { return lines; }

    public void setLines(List<TicketLineResponse> lines) { this.lines = lines; }
}
