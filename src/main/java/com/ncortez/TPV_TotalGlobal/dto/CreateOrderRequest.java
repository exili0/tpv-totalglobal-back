package com.ncortez.TPV_TotalGlobal.dto;

import java.util.List;

/**
 * DTO para abrir o actualizar una orden de venta asociada a una mesa.
 */
public class CreateOrderRequest {
    private Integer tableNumber;
    private List<OrderItemRequest> items;
    private String notes;
    private String operatorUsername;
    private String operatorSessionToken;

    public Integer getTableNumber() { return tableNumber; }
    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getOperatorUsername() { return operatorUsername; }
    public void setOperatorUsername(String operatorUsername) { this.operatorUsername = operatorUsername; }

    public String getOperatorSessionToken() { return operatorSessionToken; }
    public void setOperatorSessionToken(String operatorSessionToken) { this.operatorSessionToken = operatorSessionToken; }
}
