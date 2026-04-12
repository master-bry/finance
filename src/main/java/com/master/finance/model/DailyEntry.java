package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "daily_entries")
public class DailyEntry {
    @Id
    private String id;
    private String userId;
    private LocalDateTime date;
    private Double totalIncome;
    private Double totalExpense;
    private Double savings;
    private Map<String, Double> expensesByCategory = new HashMap<>();
    private Map<String, Boolean> goalsCompleted = new HashMap<>();
    private List<String> notes = new ArrayList<>();
    private String mood; // "HAPPY", "NEUTRAL", "STRESSED"
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Soft delete fields
    private boolean deleted = false;
    private LocalDateTime deletedAt;
    
    public DailyEntry() {
        this.date = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.completed = false;
        this.totalIncome = 0.0;
        this.totalExpense = 0.0;
        this.savings = 0.0;
        this.expensesByCategory = new HashMap<>();
        this.goalsCompleted = new HashMap<>();
        this.notes = new ArrayList<>();
    }
    
    // Helper method to calculate savings
    public void calculateSavings() {
        this.savings = this.totalIncome - this.totalExpense;
    }
    
    // Helper method to add expense to category
    public void addExpense(String category, Double amount) {
        this.expensesByCategory.merge(category, amount, Double::sum);
        this.totalExpense += amount;
        calculateSavings();
    }
    
    // Helper method to add income
    public void addIncome(Double amount) {
        this.totalIncome += amount;
        calculateSavings();
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public Double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(Double totalIncome) { 
        this.totalIncome = totalIncome;
        calculateSavings();
    }
    
    public Double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(Double totalExpense) { 
        this.totalExpense = totalExpense;
        calculateSavings();
    }
    
    public Double getSavings() { return savings; }
    public void setSavings(Double savings) { this.savings = savings; }
    
    public Map<String, Double> getExpensesByCategory() { return expensesByCategory; }
    public void setExpensesByCategory(Map<String, Double> expensesByCategory) { 
        this.expensesByCategory = expensesByCategory;
        this.totalExpense = expensesByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
        calculateSavings();
    }
    
    public Map<String, Boolean> getGoalsCompleted() { return goalsCompleted; }
    public void setGoalsCompleted(Map<String, Boolean> goalsCompleted) { this.goalsCompleted = goalsCompleted; }
    
    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }
    
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}