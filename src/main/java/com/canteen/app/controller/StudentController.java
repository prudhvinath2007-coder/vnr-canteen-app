package com.canteen.app.controller;

import com.canteen.app.dto.CartItemDto;
import com.canteen.app.dto.OrderRequestDto;
import com.canteen.app.model.*;
import com.canteen.app.service.MenuService;
import com.canteen.app.service.OrderService;
import com.canteen.app.service.UpiPaymentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final MenuService menuService;
    private final OrderService orderService;
    private final UpiPaymentService upiPaymentService;

    public StudentController(MenuService menuService,
                             OrderService orderService,
                             UpiPaymentService upiPaymentService) {
        this.menuService = menuService;
        this.orderService = orderService;
        this.upiPaymentService = upiPaymentService;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, CartItemDto> getCart(HttpSession session) {
        Map<Long, CartItemDto> cart = (Map<Long, CartItemDto>) session.getAttribute("cart");
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    private BigDecimal getCartTotal(Map<Long, CartItemDto> cart) {
        return cart.values().stream()
                .map(CartItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int getCartItemCount(Map<Long, CartItemDto> cart) {
        return cart.values().stream().mapToInt(CartItemDto::getQuantity).sum();
    }

    /**
     * Menu Browsing Page (Bootstrap UI)
     */
    @GetMapping({"", "/menu"})
    public String viewMenu(@RequestParam(value = "category", required = false) Long categoryId,
                           @RequestParam(value = "search", required = false) String search,
                           @RequestParam(value = "vegOnly", required = false, defaultValue = "false") boolean vegOnly,
                           Model model,
                           HttpSession session) {
        List<Category> categories = menuService.getAllCategories();
        List<MenuItem> items;

        if (search != null && !search.trim().isEmpty()) {
            items = menuService.searchItems(search);
        } else if (categoryId != null) {
            items = menuService.getItemsByCategory(categoryId);
        } else {
            items = menuService.getAllItems();
        }

        if (vegOnly) {
            items = items.stream().filter(item -> Boolean.TRUE.equals(item.getIsVeg())).toList();
        }

        Map<Long, CartItemDto> cart = getCart(session);

        model.addAttribute("categories", categories);
        model.addAttribute("items", items);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("search", search);
        model.addAttribute("vegOnly", vegOnly);
        model.addAttribute("cartItemCount", getCartItemCount(cart));
        model.addAttribute("cartTotal", getCartTotal(cart));

        return "student/menu";
    }

    /**
     * Add Item to Cart
     */
    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("itemId") Long itemId,
                            @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Optional<MenuItem> itemOpt = menuService.getItemById(itemId);
        if (itemOpt.isPresent()) {
            MenuItem item = itemOpt.get();
            if (Boolean.TRUE.equals(item.getIsAvailable())) {
                Map<Long, CartItemDto> cart = getCart(session);
                CartItemDto cartItem = cart.get(itemId);
                if (cartItem == null) {
                    cartItem = new CartItemDto(item.getId(), item.getName(), item.getPrice(), quantity, item.getImageUrl(), item.getIsVeg());
                    cart.put(itemId, cartItem);
                } else {
                    cartItem.setQuantity(cartItem.getQuantity() + quantity);
                }
                redirectAttributes.addFlashAttribute("successMessage", item.getName() + " added to cart!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", item.getName() + " is currently out of stock.");
            }
        }
        return "redirect:/student/menu";
    }

    /**
     * View Cart Details
     */
    @GetMapping("/cart")
    public String viewCart(Model model, HttpSession session) {
        Map<Long, CartItemDto> cart = getCart(session);
        model.addAttribute("cartItems", new ArrayList<>(cart.values()));
        model.addAttribute("cartTotal", getCartTotal(cart));
        model.addAttribute("cartItemCount", getCartItemCount(cart));
        return "student/cart";
    }

    /**
     * Update Quantity in Cart
     */
    @PostMapping("/cart/update")
    public String updateCart(@RequestParam("itemId") Long itemId,
                             @RequestParam("action") String action,
                             HttpSession session) {
        Map<Long, CartItemDto> cart = getCart(session);
        CartItemDto cartItem = cart.get(itemId);

        if (cartItem != null) {
            if ("increase".equals(action)) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
            } else if ("decrease".equals(action)) {
                if (cartItem.getQuantity() > 1) {
                    cartItem.setQuantity(cartItem.getQuantity() - 1);
                } else {
                    cart.remove(itemId);
                }
            } else if ("remove".equals(action)) {
                cart.remove(itemId);
            }
        }
        return "redirect:/student/cart";
    }

    /**
     * Checkout & UPI Payment Step
     */
    @GetMapping("/checkout")
    public String checkout(Model model, HttpSession session) {
        Map<Long, CartItemDto> cart = getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/student/menu";
        }

        BigDecimal total = getCartTotal(cart);
        String dummyOrderNum = "TEMP-" + System.currentTimeMillis() % 10000;
        String upiUri = upiPaymentService.generateUpiUri(dummyOrderNum, total);
        String qrBase64 = upiPaymentService.generateUpiQrBase64(upiUri, 260, 260);

        OrderRequestDto orderRequest = new OrderRequestDto();
        User loggedStudent = (User) session.getAttribute("loggedInStudent");
        if (loggedStudent != null) {
            orderRequest.setCustomerName(loggedStudent.getFullName());
            orderRequest.setRollNumber(loggedStudent.getRollNumber());
            orderRequest.setPhone(loggedStudent.getPhone());
        } else {
            String sessRoll = (String) session.getAttribute("studentRollNo");
            if (sessRoll != null) orderRequest.setRollNumber(sessRoll);
        }

        model.addAttribute("cartItems", new ArrayList<>(cart.values()));
        model.addAttribute("cartTotal", total);
        model.addAttribute("cartItemCount", getCartItemCount(cart));
        model.addAttribute("orderRequest", orderRequest);
        model.addAttribute("upiId", upiPaymentService.getUpiId());
        model.addAttribute("payeeName", upiPaymentService.getPayeeName());
        model.addAttribute("upiQrCode", qrBase64);
        model.addAttribute("upiUri", upiUri);

        return "student/checkout";
    }

    /**
     * Place Order and Confirm Payment
     */
    @PostMapping("/order/place")
    public String placeOrder(@ModelAttribute("orderRequest") OrderRequestDto orderRequest,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Map<Long, CartItemDto> cart = getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty.");
            return "redirect:/student/menu";
        }

        try {
            User loggedStudent = (User) session.getAttribute("loggedInStudent");
            if (loggedStudent != null && (orderRequest.getRollNumber() == null || orderRequest.getRollNumber().isBlank())) {
                orderRequest.setRollNumber(loggedStudent.getRollNumber());
            }
            if (loggedStudent != null && (orderRequest.getCustomerName() == null || orderRequest.getCustomerName().isBlank())) {
                orderRequest.setCustomerName(loggedStudent.getFullName());
            }

            Order order = orderService.placeOrder(new ArrayList<>(cart.values()), orderRequest);
            cart.clear(); // Clear cart after placing order

            // Remember student roll number in session for tracking and notification alerts
            if (order.getStudentRollNo() != null) {
                session.setAttribute("studentRollNo", order.getStudentRollNo().toUpperCase());
            }

            redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully! Token #" + order.getTokenNumber());
            return "redirect:/student/track/" + order.getOrderNumber();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to place order: " + e.getMessage());
            return "redirect:/student/checkout";
        }
    }

    /**
     * Real-time Customer Status Tracker
     */
    @GetMapping("/track/{orderNumber}")
    public String trackOrder(@PathVariable("orderNumber") String orderNumber, Model model) {
        Optional<Order> orderOpt = orderService.getOrderByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) {
            return "redirect:/student/menu";
        }

        Order order = orderOpt.get();
        model.addAttribute("order", order);
        model.addAttribute("upiId", upiPaymentService.getUpiId());
        if (order.getPayment() != null && order.getPayment().getQrBase64() != null) {
            model.addAttribute("qrCodeBase64", order.getPayment().getQrBase64());
        }

        return "student/track";
    }

    /**
     * Cancel Order before food preparation begins
     */
    @PostMapping("/order/{orderId}/cancel")
    public String cancelOrder(@PathVariable("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        boolean cancelled = orderService.cancelOrder(orderId);
        Optional<Order> orderOpt = orderService.getOrderById(orderId);

        if (cancelled) {
            redirectAttributes.addFlashAttribute("successMessage", "Order has been cancelled successfully. Refund initiated.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Order cannot be cancelled once food preparation has started.");
        }

        return orderOpt.map(order -> "redirect:/student/track/" + order.getOrderNumber())
                .orElse("redirect:/student/menu");
    }

    /**
     * My Orders list by student roll number
     */
    @GetMapping("/my-orders")
    public String myOrders(@RequestParam(value = "rollNo", required = false) String rollNo,
                           HttpSession session,
                           Model model) {
        String effectiveRoll = (rollNo != null && !rollNo.trim().isEmpty())
                ? rollNo.trim().toUpperCase()
                : (String) session.getAttribute("studentRollNo");

        if (effectiveRoll != null && !effectiveRoll.trim().isEmpty()) {
            List<Order> orders = orderService.getOrdersByStudentRollNo(effectiveRoll);
            model.addAttribute("orders", orders);
            model.addAttribute("rollNo", effectiveRoll);
        }
        return "student/my-orders";
    }
}
