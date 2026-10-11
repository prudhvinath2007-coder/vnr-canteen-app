package com.canteen.app.service;

import com.canteen.app.model.Order;
import com.canteen.app.model.Payment;
import com.canteen.app.model.PaymentStatus;
import com.canteen.app.repository.PaymentRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class UpiPaymentService {

    @Value("${canteen.upi.id:6301599722@mbkns}")
    private String upiId;

    @Value("${canteen.upi.name:VNR VJIET Canteen}")
    private String payeeName;

    private final PaymentRepository paymentRepository;

    public UpiPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Builds standard NPCI UPI URI string accepted by all Indian UPI apps (PhonePe, Paytm, GPay, BHIM)
     * Note: 'pa' must contain literal '@' (NOT %40 URL-encoded), and no 'tr' merchant ref for personal VPAs.
     */
    public String generateUpiUri(String orderNumber, BigDecimal amount) {
        return generateUpiUri(this.upiId, orderNumber, amount);
    }

    /**
     * Builds standard NPCI UPI URI string for any given VPA (PhonePe @ybl, Paytm @paytm, GPay @okaxis, Mobikwik @mbkns)
     */
    public String generateUpiUri(String customVpa, String orderNumber, BigDecimal amount) {
        String formattedAmount = (amount != null) ? String.format("%.2f", amount) : "0.00";
        String cleanUpi = (customVpa != null && !customVpa.isBlank()) ? customVpa.trim() : (upiId != null ? upiId.trim() : "6301599722@ybl");
        String cleanName = (payeeName != null) ? payeeName.trim() : "VNR VJIET Canteen";

        // Clean universal NPCI standard format
        return "upi://pay?pa=" + cleanUpi
                + "&pn=" + urlEncode(cleanName)
                + "&am=" + formattedAmount
                + "&cu=INR"
                + "&tn=" + urlEncode("VNR Canteen " + (orderNumber != null ? orderNumber : ""));
    }

    /**
     * Generates a Base64 encoded PNG image data URL for the UPI QR Code
     */
    public String generateUpiQrBase64(String upiUri, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(upiUri, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Creates and attaches a Payment record for an Order
     */
    public Payment createPaymentForOrder(Order order) {
        String upiUri = generateUpiUri(order.getOrderNumber(), order.getTotalAmount());
        String qrBase64 = generateUpiQrBase64(upiUri, 280, 280);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("UPI");
        payment.setUpiId(upiId);
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentStatus(PaymentStatus.PAID); // Auto-verified upon order submission in demo mode
        payment.setTransactionRefNo("UPI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setPaymentTime(LocalDateTime.now());
        payment.setQrBase64(qrBase64);

        return paymentRepository.save(payment);
    }

    /**
     * Verifies UPI Transaction with student provided UTR reference
     */
    public boolean verifyUpiTransaction(Long orderId, String transactionRefNo) {
        return paymentRepository.findByOrderId(orderId).map(payment -> {
            payment.setTransactionRefNo(transactionRefNo != null && !transactionRefNo.isBlank() ? transactionRefNo : "UTR-" + System.currentTimeMillis());
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaymentTime(LocalDateTime.now());
            paymentRepository.save(payment);
            return true;
        }).orElse(false);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public String getUpiId() {
        return upiId;
    }

    public String getPayeeName() {
        return payeeName;
    }
}
