package com.canteen.app.controller;

import com.canteen.app.model.Role;
import com.canteen.app.model.User;
import com.canteen.app.repository.UserRepository;
import com.canteen.app.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/student")
public class StudentAuthController {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public StudentAuthController(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model) {
        if (session.getAttribute("loggedInStudent") != null) {
            return "redirect:/student/menu";
        }
        return "student/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("identifier") String identifier,
                               @RequestParam("password") String password,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        String cleanIdentifier = identifier != null ? identifier.trim() : "";
        String cleanPassword = password != null ? password.trim() : "";

        if (cleanIdentifier.isEmpty() || cleanPassword.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide both Roll Number/Email and Password.");
            return "redirect:/student/login";
        }

        Optional<User> userOpt;
        if (cleanIdentifier.contains("@")) {
            userOpt = userRepository.findByEmail(cleanIdentifier.toLowerCase());
        } else {
            userOpt = userRepository.findByRollNumber(cleanIdentifier.toUpperCase());
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (cleanPassword.equals(user.getPassword())) {
                // Set student session
                session.setAttribute("loggedInStudent", user);
                session.setAttribute("studentRollNo", user.getRollNumber());
                session.setAttribute("studentName", user.getFullName());
                session.setAttribute("studentEmail", user.getEmail());
                session.setAttribute("studentPhone", user.getPhone());
                session.setAttribute("studentDept", user.getDepartment());

                redirectAttributes.addFlashAttribute("successMessage", "Welcome back, " + user.getFullName() + "!");
                return "redirect:/student/menu";
            }
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Invalid Roll Number / Email or Password. Please try again.");
        return "redirect:/student/login";
    }

    @GetMapping("/register")
    public String showRegisterPage(HttpSession session, Model model) {
        if (session.getAttribute("loggedInStudent") != null) {
            return "redirect:/student/menu";
        }
        return "student/register";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam("fullName") String fullName,
                                  @RequestParam("rollNumber") String rollNumber,
                                  @RequestParam("department") String department,
                                  @RequestParam("email") String email,
                                  @RequestParam("phone") String phone,
                                  @RequestParam("password") String password,
                                  @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {

        String cleanName = fullName != null ? fullName.trim() : "";
        String cleanRoll = rollNumber != null ? rollNumber.trim().toUpperCase() : "";
        String cleanDept = department != null ? department.trim() : "General";
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String cleanPhone = phone != null ? phone.trim() : "";
        String cleanPass = password != null ? password.trim() : "";

        if (cleanName.isEmpty() || cleanRoll.isEmpty() || cleanEmail.isEmpty() || cleanPass.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "All required fields must be filled.");
            return "redirect:/student/register";
        }

        if (confirmPassword != null && !confirmPassword.trim().isEmpty() && !cleanPass.equals(confirmPassword.trim())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match. Please verify.");
            return "redirect:/student/register";
        }

        if (userRepository.findByRollNumber(cleanRoll).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Roll Number " + cleanRoll + " is already registered! Please sign in.");
            return "redirect:/student/login";
        }

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email " + cleanEmail + " is already registered! Please sign in.");
            return "redirect:/student/login";
        }

        User newUser = new User(cleanName, cleanEmail, cleanPhone, cleanRoll, cleanDept, cleanPass, Role.STUDENT);
        userRepository.save(newUser);

        // Auto-login newly registered student
        session.setAttribute("loggedInStudent", newUser);
        session.setAttribute("studentRollNo", newUser.getRollNumber());
        session.setAttribute("studentName", newUser.getFullName());
        session.setAttribute("studentEmail", newUser.getEmail());
        session.setAttribute("studentPhone", newUser.getPhone());
        session.setAttribute("studentDept", newUser.getDepartment());

        // Send friendly welcome notification
        try {
            notificationService.sendNotification(
                    cleanRoll,
                    "Welcome to VNR VJIET Canteen!",
                    "Your student profile is active. Order your meals anytime to skip the queues!",
                    "INFO",
                    null,
                    null
            );
        } catch (Exception ignored) {}

        redirectAttributes.addFlashAttribute("successMessage", "Welcome to VNR VJIET Canteen, " + cleanName + "! Registration complete.");
        return "redirect:/student/menu";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.removeAttribute("loggedInStudent");
        session.removeAttribute("studentRollNo");
        session.removeAttribute("studentName");
        session.removeAttribute("studentEmail");
        session.removeAttribute("studentPhone");
        session.removeAttribute("studentDept");
        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out successfully.");
        return "redirect:/student/menu";
    }
}
