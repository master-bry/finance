package com.master.finance.model.enums;

public enum DebtType {
    OWED_TO_ME,
    I_OWE;

    public boolean isOwedToMe() {
        return this == OWED_TO_ME;
    }

    public boolean isIOwe() {
        return this == I_OWE;
    }
}
