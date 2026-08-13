package com.master.finance.model.enums;

public enum InvestmentStatus {
    ACTIVE,
    CLOSED;

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isClosed() {
        return this == CLOSED;
    }
}
