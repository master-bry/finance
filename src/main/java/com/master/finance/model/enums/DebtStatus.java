package com.master.finance.model.enums;

public enum DebtStatus {
    PENDING,
    PARTIAL,
    SETTLED,
    ACTIVE;

    public boolean isSettled() {
        return this == SETTLED;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isPartial() {
        return this == PARTIAL;
    }
}
