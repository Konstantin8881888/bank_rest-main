package com.example.bankcards.entity;

public enum BankCardStatus {
    ACTIVE("Активна"),
    BLOCKED("Заблокирована"),
    EXPIRED("Истек срок");

    private final String displayName;

    BankCardStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BankCardStatus fromString(String text) {
        for (BankCardStatus status : BankCardStatus.values()) {
            if (status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No status with text " + text + " found");
    }
}