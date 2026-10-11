package com.canteen.app.controller;

import com.canteen.app.model.Notification;
import com.canteen.app.model.User;
import com.canteen.app.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationService notificationService;

    public NotificationApiController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    private String resolveRollNo(String rollNo, HttpSession session) {
        if (rollNo != null && !rollNo.trim().isEmpty()) {
            return rollNo.trim().toUpperCase();
        }
        User user = (User) session.getAttribute("loggedInStudent");
        if (user != null && user.getRollNumber() != null) {
            return user.getRollNumber().trim().toUpperCase();
        }
        String sessionRoll = (String) session.getAttribute("studentRollNo");
        if (sessionRoll != null && !sessionRoll.trim().isEmpty()) {
            return sessionRoll.trim().toUpperCase();
        }
        return null;
    }

    /**
     * Poll endpoint for student notification bell and live popups
     */
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestNotifications(@RequestParam(value = "rollNo", required = false) String rollNo,
                                                    HttpSession session) {
        String effectiveRoll = resolveRollNo(rollNo, session);
        if (effectiveRoll == null) {
            return ResponseEntity.ok(Map.of(
                    "unreadCount", 0,
                    "notifications", List.of()
            ));
        }

        long unreadCount = notificationService.getUnreadCount(effectiveRoll);
        List<Notification> list = notificationService.getRecentNotifications(effectiveRoll);

        List<Map<String, Object>> mapped = list.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("type", n.getType());
            m.put("orderNumber", n.getOrderNumber());
            m.put("tokenNumber", n.getTokenNumber());
            m.put("isRead", n.getIsRead());
            m.put("time", n.getFormattedTime());
            return m;
        }).toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("unreadCount", unreadCount);
        resp.put("notifications", mapped);
        resp.put("rollNo", effectiveRoll);

        return ResponseEntity.ok(resp);
    }

    /**
     * Mark all notifications as read for this roll number
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllRead(@RequestParam(value = "rollNo", required = false) String rollNo,
                                         HttpSession session) {
        String effectiveRoll = resolveRollNo(rollNo, session);
        if (effectiveRoll != null) {
            notificationService.markAllAsRead(effectiveRoll);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * Mark single notification as read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markSingleRead(@PathVariable("id") Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
