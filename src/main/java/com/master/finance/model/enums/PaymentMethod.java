package com.master.finance.model.enums;

public enum PaymentMethod {
    CASH,
    BILL;

    public boolean isCash() {
        return this == CASH;
    }

    public boolean isBill() {
        return this == BILL;
    }
}
