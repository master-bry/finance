package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "budgets")
public class Budget {
    @Id
    private String id;
    private String userId;
    private String month; // Format: "2024-01"
    private Double totalIncome;
    private Double totalExpense;
    private Map<String, CategoryBudget> categoryBudgets = new HashMap<>();
    private Double savingsTarget;
    private Double actualSavings;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Soft delete fields
    private boolean deleted = false;
    private LocalDateTime deletedAt;
    
    public static class CategoryBudget {
        private Double planned;
        private Double actual;
        private String notes;
        
        public CategoryBudget() {
            this.planned = 0.0;
            this.actual = 0.0;
        }
        
        public Double getPlanned() { return planned; }
        public void setPlanned(Double planned) { this.planned = planned; }
        
        public Double getActual() { return actual; }
        public void setActual(Double actual) { this.actual = actual; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        
        public Double getVariance() {
            return this.actual - this.planned;
        }
        
        public Double getVariancePercentage() {
            if (this.planned == 0) return 0.0;
            return (this.getVariance() / this.planned) * 100;
        }
        
        public String getStatus() {
            if (getVariance() > 0) return "OVER";
            if (getVariance() < 0) return "UNDER";
            return "ON_TRACK";
        }
    }
    
    public Budget() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.actualSavings = 0.0;
        this.totalIncome = 0.0;
        this.totalExpense = 0.0;
        this.savingsTarget = 0.0;
        this.categoryBudgets = new HashMap<>();
    }
    
    public boolean isOverBudget() {
        return this.totalExpense > this.totalIncome;
    }
    
    public Double getSavingsRate() {
        if (this.totalIncome == 0) return 0.0;
        return (this.actualSavings / this.totalIncome) * 100;
    }
    
    public Double getSavingsGap() {
        return this.savingsTarget - this.actualSavings;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    
    public Double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(Double totalIncome) { this.totalIncome = totalIncome; }
    
    public Double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(Double totalExpense) { this.totalExpense = totalExpense; }
    
    public Map<String, CategoryBudget> getCategoryBudgets() { return categoryBudgets; }
    public void setCategoryBudgets(Map<String, CategoryBudget> categoryBudgets) { this.categoryBudgets = categoryBudgets; }
    
    public Double getSavingsTarget() { return savingsTarget; }
    public void setSavingsTarget(Double savingsTarget) { this.savingsTarget = savingsTarget; }
    
    public Double getActualSavings() { return actualSavings; }
    public void setActualSavings(Double actualSavings) { this.actualSavings = actualSavings; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}