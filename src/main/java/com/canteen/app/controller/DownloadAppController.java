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
        String downloadUrl = baseUrl + "/download/app";
        String qrBase64 = upiPaymentService.generateUpiQrBase64(downloadUrl, 220, 220);

        model.addAttribute("appUrl", appUrl);
        model.addAttribute("downloadUrl", downloadUrl);
        model.addAttribute("qrBase64", qrBase64);
        model.addAttribute("upiId", upiPaymentService.getUpiId());

        return "student/download-app";
    }

    /**
     * Direct APK file download endpoint served directly by web app
     */
    @GetMapping("/download/app")
    public ResponseEntity<Resource> downloadApk() {
        Resource apkResource = new ClassPathResource("static/downloads/VNR-VJIET-Canteen.apk");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"VNR-VJIET-Canteen.apk\"")
                .body(apkResource);
    }
}
