package com.canteen.app.dto;

import com.canteen.app.model.OrderStatus;

public class OrderStatusUpdateDto {
    private Long orderId;
    private OrderStatus newStatus;

    public OrderStatusUpdateDto() {}

    public OrderStatusUpdateDto(Long orderId, OrderStatus newStatus) {
        this.orderId = orderId;
        this.newStatus = newStatus;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(OrderStatus newStatus) {
        this.newStatus = newStatus;
    }
}
