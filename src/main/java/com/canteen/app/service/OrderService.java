package com.canteen.app.service;

import com.canteen.app.dto.CartItemDto;
import com.canteen.app.dto.OrderRequestDto;
import com.canteen.app.model.*;
import com.canteen.app.repository.MenuItemRepository;
import com.canteen.app.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final UpiPaymentService upiPaymentService;
    private final NotificationService notificationService;

    // Daily token generator counter
    private final AtomicInteger tokenSequence = new AtomicInteger(100);

    public OrderService(OrderRepository orderRepository,
                        MenuItemRepository menuItemRepository,
                        UpiPaymentService upiPaymentService,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.upiPaymentService = upiPaymentService;
        this.notificationService = notificationService;

        // Initialize token counter from existing records
        Optional<Order> latestOrder = orderRepository.findTopByOrderByTokenNumberDesc();
        latestOrder.ifPresent(order -> {
            if (order.getTokenNumber() != null && order.getTokenNumber() >= 100) {
                tokenSequence.set(order.getTokenNumber());
            }
        });
    }

    /**
     * Place order from student shopping cart
     */
    @Transactional
    public synchronized Order placeOrder(List<CartItemDto> cartItems, OrderRequestDto orderRequest) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty");
        }

        // Ensure token sequence is higher than any existing order in the database
        Optional<Order> highestTokenOrder = orderRepository.findTopByOrderByTokenNumberDesc();
        int currentMax = highestTokenOrder.map(Order::getTokenNumber).orElse(100);
        int nextToken = Math.max(tokenSequence.get(), currentMax) + 1;
        tokenSequence.set(nextToken);

        Order order = new Order();
        order.setTokenNumber(nextToken);

        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        order.setOrderNumber("ORD-" + datePrefix + "-" + String.format("%03d", nextToken));

        order.setCustomerName(orderRequest.getCustomerName().trim());
        order.setStudentRollNo(orderRequest.getRollNumber().trim().toUpperCase());
        order.setPhone(orderRequest.getPhone().trim());
        order.setSpecialInstructions(orderRequest.getSpecialInstructions());
        order.setOrderStatus(OrderStatus.ORDERED);
        order.setOrderTime(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;

        for (CartItemDto cartItem : cartItems) {
            MenuItem menuItem = menuItemRepository.findById(cartItem.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + cartItem.getItemId()));

            OrderItem orderItem = new OrderItem(menuItem, cartItem.getQuantity());
            order.addItem(orderItem);
            total = total.add(orderItem.getSubtotal());
        }

        order.setTotalAmount(total);
        order.setPaymentStatus(PaymentStatus.PAID);

        Order savedOrder = orderRepository.save(order);

        // Generate and record UPI payment details
        upiPaymentService.createPaymentForOrder(savedOrder);

        // Dispatch notification to student
        notificationService.sendNotification(
                savedOrder.getStudentRollNo(),
                "Order #" + savedOrder.getTokenNumber() + " Placed",
                "Your order has been sent to the kitchen. Token #" + savedOrder.getTokenNumber(),
                "ORDERED",
                savedOrder.getOrderNumber(),
                savedOrder.getTokenNumber()
        );

        return savedOrder;
    }

    /**
     * Customer cancels order before preparation begins
     */
    @Transactional
    public boolean cancelOrder(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            if (order.canBeCancelled()) {
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                orderRepository.save(order);

                // Notification for cancellation
                notificationService.sendNotification(
                        order.getStudentRollNo(),
                        "Order #" + order.getTokenNumber() + " Cancelled",
                        "Your order has been cancelled and payment marked refunded.",
                        "CANCELLED",
                        order.getOrderNumber(),
                        order.getTokenNumber()
                );

                return true;
            }
        }
        return false;
    }

    /**
     * Admin updates status: ORDERED -> PREPARING -> READY_TO_COLLECT -> DELIVERED
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        order.setOrderStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();

        if (newStatus == OrderStatus.PREPARING) {
            order.setPreparationStartTime(now);
            notificationService.sendNotification(
                    order.getStudentRollNo(),
                    "Token #" + order.getTokenNumber() + " in Preparation",
                    "Chef has started cooking your order in the kitchen!",
                    "PREPARING",
                    order.getOrderNumber(),
                    order.getTokenNumber()
            );
        } else if (newStatus == OrderStatus.READY_TO_COLLECT) {
            order.setReadyTime(now);
            notificationService.sendNotification(
                    order.getStudentRollNo(),
                    "🔔 Token #" + order.getTokenNumber() + " Ready to Collect!",
                    "Your food is hot & ready! Please collect from Counter #1 with Token #" + order.getTokenNumber(),
                    "READY",
                    order.getOrderNumber(),
                    order.getTokenNumber()
            );
        } else if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveryTime(now);
            notificationService.sendNotification(
                    order.getStudentRollNo(),
                    "Order #" + order.getTokenNumber() + " Delivered",
                    "Order completed. Thank you for dining at VNR VJIET Canteen!",
                    "DELIVERED",
                    order.getOrderNumber(),
                    order.getTokenNumber()
            );
        }

        return orderRepository.save(order);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Optional<Order> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<Order> getActiveOrdersForCanteenQueue() {
        return orderRepository.findByOrderStatusInOrderByOrderTimeAsc(
                List.of(OrderStatus.ORDERED, OrderStatus.PREPARING, OrderStatus.READY_TO_COLLECT)
        );
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderTimeDesc();
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByOrderStatus(status);
    }

    public List<Order> getOrdersByStudentRollNo(String rollNo) {
        return orderRepository.findByStudentRollNoOrderByOrderTimeDesc(rollNo.toUpperCase());
    }

    public Map<String, Object> getCanteenDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("orderedCount", orderRepository.countByOrderStatus(OrderStatus.ORDERED));
        stats.put("preparingCount", orderRepository.countByOrderStatus(OrderStatus.PREPARING));
        stats.put("readyCount", orderRepository.countByOrderStatus(OrderStatus.READY_TO_COLLECT));
        stats.put("deliveredCount", orderRepository.countByOrderStatus(OrderStatus.DELIVERED));
        stats.put("totalRevenue", orderRepository.calculateTotalRevenue());

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        stats.put("todayOrdersCount", orderRepository.countOrdersSince(startOfToday));
        stats.put("todayRevenue", orderRepository.calculateRevenueSince(startOfToday));

        return stats;
    }
}
