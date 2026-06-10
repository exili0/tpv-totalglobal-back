package com.ncortez.TPV_TotalGlobal.dto;

/**
 * DTO para mover una comanda abierta desde una mesa origen hacia una mesa destino.
 */
public class MoveTableRequest {
    private Integer fromTableNumber;
    private Integer toTableNumber;
    private String sessionToken;

    public Integer getFromTableNumber() {
        return fromTableNumber;
    }

    public void setFromTableNumber(Integer fromTableNumber) {
        this.fromTableNumber = fromTableNumber;
    }

    public Integer getToTableNumber() {
        return toTableNumber;
    }

    public void setToTableNumber(Integer toTableNumber) {
        this.toTableNumber = toTableNumber;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
}
