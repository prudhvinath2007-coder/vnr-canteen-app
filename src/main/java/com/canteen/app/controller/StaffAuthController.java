package com.canteen.app.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StaffAuthController {

    // Default canteen staff PIN / Password
    private static final String STAFF_PIN = "vnr@canteen";
    private static final String BACKUP_PIN = "admin123";

    @GetMapping("/canteen/login")
    public String staffLoginPage(HttpSession session) {
        if (Boolean.TRUE.equals(session.getAttribute("isStaffAuthenticated"))) {
            return "redirect:/canteen/admin/dashboard";
        }
        return "admin/staff-login";
    }

    @PostMapping("/canteen/login")
    public String authenticateStaff(@RequestParam("passcode") String passcode,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (STAFF_PIN.equalsIgnoreCase(passcode.trim()) || BACKUP_PIN.equals(passcode.trim())) {
            session.setAttribute("isStaffAuthenticated", true);
            redirectAttributes.addFlashAttribute("successMessage", "Welcome, VNR VJIET Canteen Staff!");
            return "redirect:/canteen/admin/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid Staff Passcode. Access restricted to canteen staff only.");
            return "redirect:/canteen/login";
        }
    }

    @GetMapping("/canteen/logout")
    public String staffLogout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("isStaffAuthenticated");
        redirectAttributes.addFlashAttribute("successMessage", "Logged out of Kitchen Portal successfully.");
        return "redirect:/student";
    }
}
