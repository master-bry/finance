package com.master.finance.model.enums;

public enum AuditSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL;

    public boolean isAtLeastWarning() {
        return this != INFO;
    }
}
