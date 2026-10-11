package com.canteen.app.controller;

import com.canteen.app.model.*;
import com.canteen.app.service.MenuService;
import com.canteen.app.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/canteen/admin")
public class AdminCanteenController {

    private final OrderService orderService;
    private final MenuService menuService;
    private final com.canteen.app.service.RazorpayService razorpayService;

    public AdminCanteenController(OrderService orderService,
                                  MenuService menuService,
                                  com.canteen.app.service.RazorpayService razorpayService) {
        this.orderService = orderService;
        this.menuService = menuService;
        this.razorpayService = razorpayService;
    }

    /**
     * Kitchen Live Orders Dashboard (Steps 6-9 in Flowchart)
     */
    @GetMapping({"", "/dashboard"})
    public String dashboard(@RequestParam(value = "statusFilter", required = false) String statusFilter,
                            jakarta.servlet.http.HttpSession session,
                            Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("isStaffAuthenticated"))) {
            return "redirect:/canteen/login";
        }

        List<Order> activeOrders;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                OrderStatus status = OrderStatus.valueOf(statusFilter);
                activeOrders = orderService.getOrdersByStatus(status);
            } catch (Exception e) {
                activeOrders = orderService.getActiveOrdersForCanteenQueue();
            }
        } else {
            activeOrders = orderService.getActiveOrdersForCanteenQueue();
        }

        Map<String, Object> stats = orderService.getCanteenDashboardStats();

        model.addAttribute("activeOrders", activeOrders);
        model.addAttribute("stats", stats);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("allStatuses", OrderStatus.values());
        model.addAttribute("razorpayKeyId", razorpayService.getKeyId());
        model.addAttribute("isRazorpayLive", razorpayService.isLiveMode());

        return "admin/dashboard";
    }

    /**
     * Update Razorpay Live / Test Credentials dynamically
     */
    @PostMapping("/settings/razorpay")
    public String updateRazorpaySettings(@RequestParam("keyId") String keyId,
                                         @RequestParam("keySecret") String keySecret,
                                         jakarta.servlet.http.HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(session.getAttribute("isStaffAuthenticated"))) {
            return "redirect:/canteen/login";
        }

        razorpayService.updateCredentials(keyId, keySecret);
        if (razorpayService.isLiveMode()) {
            redirectAttributes.addFlashAttribute("successMessage", "Razorpay switched to LIVE PRODUCTION mode! Payments will credit real bank account with instant settlement.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Razorpay credentials updated in TEST mode.");
        }
        return "redirect:/canteen/admin/dashboard";
    }

    /**
     * Step 7: Admin Accepts & Prepares Order (status -> PREPARING)
     */
    @PostMapping("/orders/{orderId}/prepare")
    public String prepareOrder(@PathVariable("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        orderService.updateOrderStatus(orderId, OrderStatus.PREPARING);
        redirectAttributes.addFlashAttribute("successMessage", "Order #" + orderId + " is now in PREPARATION.");
        return "redirect:/canteen/admin/dashboard";
    }

    /**
     * Step 8: Updates Status to Ready to Collect (status -> READY_TO_COLLECT)
     */
    @PostMapping("/orders/{orderId}/ready")
    public String readyOrder(@PathVariable("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        Order order = orderService.updateOrderStatus(orderId, OrderStatus.READY_TO_COLLECT);
        redirectAttributes.addFlashAttribute("successMessage", "Order #" + orderId + " (Token #" + order.getTokenNumber() + ") marked READY TO COLLECT!");
        return "redirect:/canteen/admin/dashboard";
    }

    /**
     * Step 9: Marks Order as Delivered & Closes Order (status -> DELIVERED)
     */
    @PostMapping("/orders/{orderId}/deliver")
    public String deliverOrder(@PathVariable("orderId") Long orderId, RedirectAttributes redirectAttributes) {
        orderService.updateOrderStatus(orderId, OrderStatus.DELIVERED);
        redirectAttributes.addFlashAttribute("successMessage", "Order #" + orderId + " marked DELIVERED and closed.");
        return "redirect:/canteen/admin/dashboard";
    }

    /**
     * Menu Management Portal
     */
    @GetMapping("/menu")
    public String manageMenu(jakarta.servlet.http.HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("isStaffAuthenticated"))) {
            return "redirect:/canteen/login";
        }
        List<MenuItem> items = menuService.getAllItems();
        List<Category> categories = menuService.getAllCategories();

        model.addAttribute("items", items);
        model.addAttribute("categories", categories);
        model.addAttribute("newItem", new MenuItem());

        return "admin/menu-management";
    }

    /**
     * Toggle Stock Availability
     */
    @PostMapping("/menu/{itemId}/toggle")
    public String toggleAvailability(@PathVariable("itemId") Long itemId, RedirectAttributes redirectAttributes) {
        boolean available = menuService.toggleAvailability(itemId);
        redirectAttributes.addFlashAttribute("successMessage", "Item availability updated: " + (available ? "IN STOCK" : "OUT OF STOCK"));
        return "redirect:/canteen/admin/menu";
    }

    /**
     * Add New Menu Item
     */
    @PostMapping("/menu/save")
    public String saveMenuItem(@RequestParam("name") String name,
                               @RequestParam("categoryId") Long categoryId,
                               @RequestParam("price") BigDecimal price,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam(value = "imageUrl", required = false) String imageUrl,
                               @RequestParam(value = "isVeg", defaultValue = "true") boolean isVeg,
                               @RequestParam(value = "prepTimeMinutes", defaultValue = "10") int prepTimeMinutes,
                               RedirectAttributes redirectAttributes) {
        Optional<Category> catOpt = menuService.getCategoryById(categoryId);
        if (catOpt.isPresent()) {
            MenuItem item = new MenuItem(catOpt.get(), name, description, price, imageUrl, isVeg, true, prepTimeMinutes);
            menuService.saveMenuItem(item);
            redirectAttributes.addFlashAttribute("successMessage", "Added new item: " + name);
        }
        return "redirect:/canteen/admin/menu";
    }

    /**
     * Delete Menu Item
     */
    @PostMapping("/menu/{itemId}/delete")
    public String deleteMenuItem(@PathVariable("itemId") Long itemId, RedirectAttributes redirectAttributes) {
        menuService.deleteMenuItem(itemId);
        redirectAttributes.addFlashAttribute("successMessage", "Item deleted successfully.");
        return "redirect:/canteen/admin/menu";
    }

    /**
     * Daily Reports and Order History
     */
    @GetMapping("/reports")
    public String reports(jakarta.servlet.http.HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("isStaffAuthenticated"))) {
            return "redirect:/canteen/login";
        }
        List<Order> allOrders = orderService.getAllOrders();
        Map<String, Object> stats = orderService.getCanteenDashboardStats();

        model.addAttribute("orders", allOrders);
        model.addAttribute("stats", stats);

        return "admin/reports";
    }
}
