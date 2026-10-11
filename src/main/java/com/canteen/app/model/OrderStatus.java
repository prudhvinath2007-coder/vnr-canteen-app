package com.canteen.app.model;

public enum OrderStatus {
    ORDERED("Order Placed", "badge-primary", "bi-receipt"),
    PREPARING("Preparing Food", "badge-warning", "bi-fire"),
    READY_TO_COLLECT("Ready to Collect", "badge-success", "bi-bell-fill"),
    DELIVERED("Delivered", "badge-secondary", "bi-check-circle-fill"),
    CANCELLED("Cancelled", "badge-danger", "bi-x-circle-fill");

    private final String displayName;
    private final String badgeClass;
    private final String icon;

    OrderStatus(String displayName, String badgeClass, String icon) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIcon() {
        return icon;
    }
}
