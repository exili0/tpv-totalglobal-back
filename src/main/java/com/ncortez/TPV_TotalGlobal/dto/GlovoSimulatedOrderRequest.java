package com.ncortez.TPV_TotalGlobal.dto;

import java.util.List;

/**
 * DTO de entrada para simular el webhook de pedido despachado de Glovo.
 */
public class GlovoSimulatedOrderRequest {
    private String glovoOrderId;
    private String orderCode;
    private String storeId;
    private String customerName;
    private String paymentMethod;
    private String specialRequirements;
    private String operatorUsername;
    private List<GlovoSimulatedOrderItemRequest> items;

    public String getGlovoOrderId() {
        return glovoOrderId;
    }

    public void setGlovoOrderId(String glovoOrderId) {
        this.glovoOrderId = glovoOrderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSpecialRequirements() {
        return specialRequirements;
    }

    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    public List<GlovoSimulatedOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<GlovoSimulatedOrderItemRequest> items) {
        this.items = items;
    }
}
