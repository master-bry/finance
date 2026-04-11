package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "investments")
public class Investment {
    @Id
    private String id;
    private String userId;
    private String name;
    private String type; // "STOCKS", "REAL_ESTATE", "BUSINESS", "SAVINGS", "CRYPTO", "OTHER"
    private Double amountInvested;
    private Double currentValue;
    private Double expectedReturn;
    private String riskLevel; // "LOW", "MEDIUM", "HIGH"
    private LocalDateTime startDate;
    private LocalDateTime maturityDate;
    private String status; // "ACTIVE", "MATURED", "SOLD", "LOSS"
    private String provider;
    private String notes;
    private List<InvestmentTransaction> transactions = new ArrayList<>();
    
    public static class InvestmentTransaction {
        private LocalDateTime date;
        private String type; // "DEPOSIT", "WITHDRAWAL", "INTEREST", "DIVIDEND"
        private Double amount;
        private String description;
        
        public InvestmentTransaction() {
            this.date = LocalDateTime.now();
        }
        
        // Getters and Setters
        public LocalDateTime getDate() { return date; }
        public void setDate(LocalDateTime date) { this.date = date; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public Investment() {
        this.startDate = LocalDateTime.now();
        this.status = "ACTIVE";
        this.currentValue = 0.0;
        this.amountInvested = 0.0;
        this.transactions = new ArrayList<>();
    }
    
    public Double getProfitLoss() {
        return this.currentValue - this.amountInvested;
    }
    
    public Double getProfitLossPercentage() {
        if (this.amountInvested == 0) return 0.0;
        return (this.getProfitLoss() / this.amountInvested) * 100;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Double getAmountInvested() { return amountInvested; }
    public void setAmountInvested(Double amountInvested) { this.amountInvested = amountInvested; }
    
    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }
    
    public Double getExpectedReturn() { return expectedReturn; }
    public void setExpectedReturn(Double expectedReturn) { this.expectedReturn = expectedReturn; }
    
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDateTime maturityDate) { this.maturityDate = maturityDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public List<InvestmentTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<InvestmentTransaction> transactions) { this.transactions = transactions; }
}