package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "daily_entries")
public class DailyEntry {
    @Id
    private String id;
    private String userId;
    private LocalDateTime date;
    private Double openingBalance;
    private Double totalIncome;
    private Double totalExpense;
    private Double closingBalance;
    private List<ExpenseItem> expenses = new ArrayList<>();
    private List<IncomeItem> incomes = new ArrayList<>();
    private String notes;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted = false;
    
    public static class ExpenseItem {
        private String description;
        private Double amount;
        private String category;
        private LocalDateTime time;
        
        public ExpenseItem() {
            this.time = LocalDateTime.now();
        }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public LocalDateTime getTime() { return time; }
        public void setTime(LocalDateTime time) { this.time = time; }
    }
    
    public static class IncomeItem {
        private String description;
        private Double amount;
        private String source;
        private LocalDateTime time;
        
        public IncomeItem() {
            this.time = LocalDateTime.now();
        }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public LocalDateTime getTime() { return time; }
        public void setTime(LocalDateTime time) { this.time = time; }
    }
    
    public DailyEntry() {
        this.date = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.completed = false;
        this.totalIncome = 0.0;
        this.totalExpense = 0.0;
        this.closingBalance = 0.0;
        this.openingBalance = 0.0;
        this.expenses = new ArrayList<>();
        this.incomes = new ArrayList<>();
    }
    
    public void calculateTotals() {
        this.totalIncome = incomes.stream().mapToDouble(IncomeItem::getAmount).sum();
        this.totalExpense = expenses.stream().mapToDouble(ExpenseItem::getAmount).sum();
        this.closingBalance = this.openingBalance + this.totalIncome - this.totalExpense;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public Double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(Double openingBalance) { this.openingBalance = openingBalance; calculateTotals(); }
    public Double getTotalIncome() { return totalIncome; }
    public Double getTotalExpense() { return totalExpense; }
    public Double getClosingBalance() { return closingBalance; }
    public List<ExpenseItem> getExpenses() { return expenses; }
    public void setExpenses(List<ExpenseItem> expenses) { this.expenses = expenses; calculateTotals(); }
    public List<IncomeItem> getIncomes() { return incomes; }
    public void setIncomes(List<IncomeItem> incomes) { this.incomes = incomes; calculateTotals(); }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}