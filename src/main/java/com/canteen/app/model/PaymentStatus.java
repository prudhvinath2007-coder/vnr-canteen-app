package com.canteen.app.model;

public enum PaymentStatus {
    PENDING("Payment Pending", "badge-warning"),
    PAID("UPI Payment Verified", "badge-success"),
    FAILED("Payment Failed", "badge-danger"),
    REFUNDED("Refunded", "badge-info");

    private final String displayName;
    private final String badgeClass;

    PaymentStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
