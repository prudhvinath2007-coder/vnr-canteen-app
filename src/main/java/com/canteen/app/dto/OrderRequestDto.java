package com.canteen.app.dto;

import jakarta.validation.constraints.NotBlank;

public class OrderRequestDto {

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Student roll number is required")
    private String rollNumber;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String specialInstructions;

    private String upiTransactionId; // UTR entered by user during payment confirmation

    private String paymentMethod = "UPI"; // "UPI" or "CASH_AT_COUNTER"

    private String selectedVpa; // The specific UPI VPA used for payment

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    public OrderRequestDto() {}

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public String getUpiTransactionId() {
        return upiTransactionId;
    }

    public void setUpiTransactionId(String upiTransactionId) {
        this.upiTransactionId = upiTransactionId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSelectedVpa() {
        return selectedVpa;
    }

    public void setSelectedVpa(String selectedVpa) {
        this.selectedVpa = selectedVpa;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpaySignature() {
        return razorpaySignature;
    }

    public void setRazorpaySignature(String razorpaySignature) {
        this.razorpaySignature = razorpaySignature;
    }
}
