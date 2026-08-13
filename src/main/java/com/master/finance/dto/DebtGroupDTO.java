package com.master.finance.dto;

import java.math.BigDecimal;
import java.util.List;

import com.master.finance.model.Debt;
import com.master.finance.model.enums.DebtType;
import com.master.finance.model.enums.DebtStatus;

public class DebtGroupDTO {
    private String personName;
    private String phoneNumber;
    private BigDecimal totalAmount;
    private BigDecimal totalRemaining;
    private DebtStatus overallStatus;
    private List<Debt> debts;
    private int debtCount;
    private DebtType type;
    
    public DebtGroupDTO() {}
    
    public DebtGroupDTO(String personName, String phoneNumber, BigDecimal totalAmount, 
                        BigDecimal totalRemaining, DebtStatus overallStatus, List<Debt> debts, DebtType type) {
        this.personName = personName;
        this.phoneNumber = phoneNumber;
        this.totalAmount = totalAmount;
        this.totalRemaining = totalRemaining;
        this.overallStatus = overallStatus;
        this.debts = debts;
        this.debtCount = debts != null ? debts.size() : 0;
        this.type = type;
    }
    
    // Getters and Setters
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    
    public BigDecimal getTotalRemaining() { return totalRemaining; }
    public void setTotalRemaining(BigDecimal totalRemaining) { this.totalRemaining = totalRemaining; }
    
    public DebtStatus getOverallStatus() { return overallStatus; }
    public void setOverallStatus(DebtStatus overallStatus) { this.overallStatus = overallStatus; }
    
    public List<Debt> getDebts() { return debts; }
    public void setDebts(List<Debt> debts) { 
        this.debts = debts;
        this.debtCount = debts != null ? debts.size() : 0;
    }
    
    public int getDebtCount() { return debtCount; }
    public void setDebtCount(int debtCount) { this.debtCount = debtCount; }
    
    public DebtType getType() { return type; }
    public void setType(DebtType type) { this.type = type; }
    
    public String getFormattedTotalAmount() {
        return String.format("%,.0f", totalAmount);
    }
    
    public String getFormattedTotalRemaining() {
        return String.format("%,.0f", totalRemaining);
    }
}
