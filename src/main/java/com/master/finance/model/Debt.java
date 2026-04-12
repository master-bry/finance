package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "debts")
public class Debt {
    @Id
    private String id;
    private String userId;
    private String personName;
    private String type; // "OWED_TO_ME" or "I_OWE"
    private Double amount;
    private Double remainingAmount;
    private String status; // "PENDING", "PARTIAL", "SETTLED"
    private String description;
    private LocalDateTime dueDate;
    private LocalDateTime dateGiven;
    private LocalDateTime lastUpdated;
    private String phoneNumber;
    private String notes;
    private List<PaymentRecord> paymentHistory = new ArrayList<>();
    
    // Soft delete fields
    private boolean deleted = false;
    private LocalDateTime deletedAt;
    
    public static class PaymentRecord {
        private LocalDateTime paymentDate;
        private Double amountPaid;
        private String notes;
        
        public PaymentRecord() {
            this.paymentDate = LocalDateTime.now();
        }
        
        public LocalDateTime getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
        public Double getAmountPaid() { return amountPaid; }
        public void setAmountPaid(Double amountPaid) { this.amountPaid = amountPaid; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    public Debt() {
        this.dateGiven = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.status = "PENDING";
        this.remainingAmount = 0.0;
        this.paymentHistory = new ArrayList<>();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { 
        this.amount = amount;
        if (this.remainingAmount == null || this.remainingAmount == 0) {
            this.remainingAmount = amount;
        }
    }
    
    public Double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Double remainingAmount) { this.remainingAmount = remainingAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    
    public LocalDateTime getDateGiven() { return dateGiven; }
    public void setDateGiven(LocalDateTime dateGiven) { this.dateGiven = dateGiven; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public List<PaymentRecord> getPaymentHistory() { return paymentHistory; }
    public void setPaymentHistory(List<PaymentRecord> paymentHistory) { this.paymentHistory = paymentHistory; }
    
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}