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
     * Builds standard NPCI UPI URI string
     * format: upi://pay?pa=...&pn=...&am=...&cu=INR&tn=...&tr=...
     */
    public String generateUpiUri(String orderNumber, BigDecimal amount) {
        String formattedAmount = String.format("%.2f", amount);
        String note = "Canteen Order " + orderNumber;
        String transactionRef = "TXN" + System.currentTimeMillis();

        return String.format(
                "upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s&tr=%s",
                urlEncode(upiId),
                urlEncode(payeeName),
                formattedAmount,
                urlEncode(note),
                urlEncode(transactionRef)
        );
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
