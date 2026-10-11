package com.canteen.app.controller;

import com.canteen.app.service.UpiPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DownloadAppController {

    private final UpiPaymentService upiPaymentService;

    public DownloadAppController(UpiPaymentService upiPaymentService) {
        this.upiPaymentService = upiPaymentService;
    }

    /**
     * Android App Landing Page with Download Links & Install Guide
     */
    @GetMapping("/download-app")
    public String showDownloadPage(HttpServletRequest request, Model model) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        String baseUrl = (serverPort == 80 || serverPort == 443)
                ? String.format("%s://%s", scheme, serverName)
                : String.format("%s://%s:%d", scheme, serverName, serverPort);

        String appUrl = baseUrl + "/student/menu";
        String apkReleaseUrl = "https://github.com/prudhvinath2007-coder/vnr-canteen-app/releases/download/v1.0.0-apk/VNR-VJIET-Canteen.apk";
        String qrBase64 = upiPaymentService.generateUpiQrBase64(apkReleaseUrl, 220, 220);

        model.addAttribute("appUrl", appUrl);
        model.addAttribute("downloadUrl", apkReleaseUrl);
        model.addAttribute("qrBase64", qrBase64);
        model.addAttribute("upiId", upiPaymentService.getUpiId());

        return "student/download-app";
    }

    /**
     * Direct APK file download endpoint
     */
    @GetMapping("/download/app")
    public String downloadApk() {
        return "redirect:https://github.com/prudhvinath2007-coder/vnr-canteen-app/releases/download/v1.0.0-apk/VNR-VJIET-Canteen.apk";
    }
}
