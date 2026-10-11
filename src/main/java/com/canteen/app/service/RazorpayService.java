package com.canteen.app.service;

import com.canteen.app.dto.OrderRequestDto;
import com.canteen.app.model.Order;
import com.canteen.app.model.Payment;
import com.canteen.app.model.PaymentStatus;
import com.canteen.app.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id:rzp_test_1DP5mmOlF5G5ag}")
    private String keyId;

    @Value("${razorpay.key.secret:s9mQw9v0QZgK9XbZ8a8qZ9mQ}")
    private String keySecret;

    @Value("${razorpay.company.name:VNR VJIET Canteen}")
    private String companyName;

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    public RazorpayService(PaymentRepository paymentRepository, RestTemplateBuilder restTemplateBuilder) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Create a Razorpay Order ID for checkout.
     * Communicates with Razorpay API https://api.razorpay.com/v1/orders
     * Falls back to a deterministic test order reference if API cannot be contacted.
     */
    public Map<String, Object> createOrder(BigDecimal amount, String receipt) {
        long amountInPaise = (amount != null)
                ? amount.multiply(BigDecimal.valueOf(100)).longValue()
                : 0L;

        Map<String, Object> response = new HashMap<>();
        response.put("keyId", keyId);
        response.put("amount", amountInPaise);
        response.put("currency", "INR");
        response.put("companyName", companyName);

        try {
            String url = "https://api.razorpay.com/v1/orders";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(keyId, keySecret);

            Map<String, Object> orderPayload = new HashMap<>();
            orderPayload.put("amount", amountInPaise);
            orderPayload.put("currency", "INR");
            orderPayload.put("receipt", receipt != null ? receipt : "rcpt_" + System.currentTimeMillis());
            orderPayload.put("payment_capture", 1);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(orderPayload, headers);
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (apiResponse.getStatusCode().is2xxSuccessful() && apiResponse.getBody() != null) {
                String razorpayOrderId = (String) apiResponse.getBody().get("id");
                response.put("orderId", razorpayOrderId);
                log.info("Created real Razorpay Order: {}", razorpayOrderId);
                return response;
            }
        } catch (Exception e) {
            log.warn("Razorpay API live order creation fallback active: {}", e.getMessage());
        }

        // Test/Demo fallback order ID
        String fallbackOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        response.put("orderId", fallbackOrderId);
        return response;
    }

    /**
     * Cryptographically verifies Razorpay payment signature using HMAC SHA-256.
     * signature = HMAC_SHA256(order_id + "|" + payment_id, secret)
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        if (paymentId == null || paymentId.isBlank()) {
            return false;
        }

        // Test / demo mode signature bypass ONLY allowed when NOT in Live mode
        if (!isLiveMode()) {
            if ("demo_verified".equalsIgnoreCase(signature) || paymentId.startsWith("pay_demo_") || paymentId.startsWith("pay_test_")) {
                log.info("Payment verified in demo/test mode: {}", paymentId);
                return true;
            }
        }

        if (orderId == null || signature == null) {
            return false;
        }

        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String expectedSignature = hexString.toString();
            boolean isValid = expectedSignature.equalsIgnoreCase(signature.trim());
            
            if (isValid) {
                log.info("Razorpay cryptographic HMAC-SHA256 signature verified for payment: {}", paymentId);
            } else {
                log.warn("Razorpay signature mismatch: expected {}, received {}", expectedSignature, signature);
            }
            return isValid;
        } catch (Exception e) {
            log.error("Error verifying Razorpay signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if Razorpay is configured with a live production key
     */
    public boolean isLiveMode() {
        return keyId != null && keyId.trim().startsWith("rzp_live_");
    }

    /**
     * Dynamically updates Razorpay API credentials
     */
    public synchronized void updateCredentials(String newKeyId, String newKeySecret) {
        if (newKeyId != null && !newKeyId.isBlank()) {
            this.keyId = newKeyId.trim();
        }
        if (newKeySecret != null && !newKeySecret.isBlank()) {
            this.keySecret = newKeySecret.trim();
        }
        log.info("Razorpay credentials updated. Live mode: {}", isLiveMode());
    }

    /**
     * Records Razorpay Payment details for an Order in the database
     */
    public Payment recordRazorpayPayment(Order order, OrderRequestDto orderRequest) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod("RAZORPAY");
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentStatus(PaymentStatus.PAID);
        
        String paymentId = orderRequest.getRazorpayPaymentId();
        if (paymentId == null || paymentId.isBlank()) {
            paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        }
        payment.setTransactionRefNo(paymentId);
        payment.setRazorpayPaymentId(paymentId);
        payment.setRazorpayOrderId(orderRequest.getRazorpayOrderId());
        payment.setRazorpaySignature(orderRequest.getRazorpaySignature());
        payment.setPaymentTime(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public String getKeyId() {
        return keyId;
    }

    public String getCompanyName() {
        return companyName;
    }
}
