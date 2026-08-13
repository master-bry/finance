package com.master.finance.model.enums;

public enum BillStatus {
    PENDING,
    PARTIAL,
    PAID;

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean isPartial() {
        return this == PARTIAL;
    }

    public boolean isPending() {
        return this == PENDING;
    }
}
