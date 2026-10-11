package com.canteen.app.controller;

import com.canteen.app.model.Order;
import com.canteen.app.model.OrderStatus;
import com.canteen.app.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Endpoint polled by Customer Status Tracker
     */
    @GetMapping("/{orderNumber}/status")
    public ResponseEntity<?> getOrderStatus(@PathVariable("orderNumber") String orderNumber) {
        Optional<Order> orderOpt = orderService.getOrderByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = orderOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("orderNumber", order.getOrderNumber());
        response.put("tokenNumber", order.getTokenNumber());
        response.put("customerName", order.getCustomerName());
        response.put("status", order.getOrderStatus().name());
        response.put("statusDisplay", order.getOrderStatus().getDisplayName());
        response.put("badgeClass", order.getOrderStatus().getBadgeClass());
        response.put("canCancel", order.canBeCancelled());
        response.put("totalAmount", order.getTotalAmount());
        response.put("orderTime", order.getFormattedOrderTime());
        response.put("isReady", order.getOrderStatus() == OrderStatus.READY_TO_COLLECT);
        response.put("isDelivered", order.getOrderStatus() == OrderStatus.DELIVERED);
        response.put("isCancelled", order.getOrderStatus() == OrderStatus.CANCELLED);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint polled by Canteen Staff Dashboard for live updates and notifications
     */
    @GetMapping("/live-queue")
    public ResponseEntity<?> getLiveQueue() {
        List<Order> activeOrders = orderService.getActiveOrdersForCanteenQueue();
        Map<String, Object> stats = orderService.getCanteenDashboardStats();

        List<Map<String, Object>> ordersList = activeOrders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("orderId", order.getId());
            map.put("orderNumber", order.getOrderNumber());
            map.put("tokenNumber", order.getTokenNumber());
            map.put("customerName", order.getCustomerName());
            map.put("rollNo", order.getStudentRollNo());
            map.put("phone", order.getPhone());
            map.put("status", order.getOrderStatus().name());
            map.put("statusDisplay", order.getOrderStatus().getDisplayName());
            map.put("totalAmount", order.getTotalAmount());
            map.put("orderTime", order.getFormattedOrderTime());
            map.put("itemsCount", order.getItems().size());
            return map;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("orders", ordersList);
        response.put("stats", stats);
        response.put("activeCount", ordersList.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Quick status update endpoint
     */
    @PostMapping("/{orderId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable("orderId") Long orderId,
                                          @RequestParam("status") OrderStatus status) {
        try {
            Order updated = orderService.updateOrderStatus(orderId, status);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderId", updated.getId(),
                    "newStatus", updated.getOrderStatus().name(),
                    "tokenNumber", updated.getTokenNumber()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
