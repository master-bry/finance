package com.master.finance.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "daily_entries")
public class DailyEntry {
    @Id
    private String id;
    private String userId;
    private LocalDateTime date;
    private Double openingBalance;
    private Double totalIncome;
    private Double totalExpense;          // cash expenses only
    private Double totalPrepaidExpense;   // new: prepaid expenses only
    private Double savings;               // cash savings (totalIncome - totalExpense)
    private Double closingBalance;
    private List<ExpenseItem> expenses = new ArrayList<>();        // cash expenses
    private List<ExpenseItem> prepaidExpenses = new ArrayList<>(); // prepaid expenses
    private List<IncomeItem> incomes = new ArrayList<>();
    private Map<String, Double> expensesByCategory = new HashMap<>();
    private Map<String, Boolean> goalsCompleted = new HashMap<>();
    private String notes;
    private String mood;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted = false;
    private LocalDateTime deletedAt;

    @Document
    public static class ExpenseItem {
        private String description;
        private Double amount;
        private String category;
        private LocalDateTime time;
        private String paymentMethod; // "CASH" or "BILL"
        private String billId;

        public ExpenseItem() {
            this.time = LocalDateTime.now();
            this.paymentMethod = "CASH";
        }

        // getters and setters
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public LocalDateTime getTime() { return time; }
        public void setTime(LocalDateTime time) { this.time = time; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getBillId() { return billId; }
        public void setBillId(String billId) { this.billId = billId; }
    }

    @Document
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
        this.totalPrepaidExpense = 0.0;
        this.savings = 0.0;
        this.closingBalance = 0.0;
        this.openingBalance = 0.0;
        this.mood = "NEUTRAL";
        this.expenses = new ArrayList<>();
        this.prepaidExpenses = new ArrayList<>();
        this.incomes = new ArrayList<>();
        this.expensesByCategory = new HashMap<>();
        this.goalsCompleted = new HashMap<>();
    }

    public void calculateTotals() {
        this.totalIncome = incomes.stream().mapToDouble(IncomeItem::getAmount).sum();
        this.totalExpense = expenses.stream().mapToDouble(ExpenseItem::getAmount).sum();
        this.totalPrepaidExpense = prepaidExpenses.stream().mapToDouble(ExpenseItem::getAmount).sum();
        this.savings = this.totalIncome - this.totalExpense;   // cash savings
        this.closingBalance = this.openingBalance + this.totalIncome - this.totalExpense;
    }

    // Getters and Setters (including new ones)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public Double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(Double openingBalance) { this.openingBalance = openingBalance; calculateTotals(); }
    public Double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(Double totalIncome) { this.totalIncome = totalIncome; calculateTotals(); }
    public Double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(Double totalExpense) { this.totalExpense = totalExpense; calculateTotals(); }
    public Double getTotalPrepaidExpense() { return totalPrepaidExpense; }
    public void setTotalPrepaidExpense(Double totalPrepaidExpense) { this.totalPrepaidExpense = totalPrepaidExpense; calculateTotals(); }
    public Double getSavings() { return savings; }
    public void setSavings(Double savings) { this.savings = savings; }
    public Double getClosingBalance() { return closingBalance; }
    public void setClosingBalance(Double closingBalance) { this.closingBalance = closingBalance; }
    public List<ExpenseItem> getExpenses() { return expenses; }
    public void setExpenses(List<ExpenseItem> expenses) { this.expenses = expenses; calculateTotals(); }
    public List<ExpenseItem> getPrepaidExpenses() { return prepaidExpenses; }
    public void setPrepaidExpenses(List<ExpenseItem> prepaidExpenses) { this.prepaidExpenses = prepaidExpenses; calculateTotals(); }
    public List<IncomeItem> getIncomes() { return incomes; }
    public void setIncomes(List<IncomeItem> incomes) { this.incomes = incomes; calculateTotals(); }
    public Map<String, Double> getExpensesByCategory() { return expensesByCategory; }
    public void setExpensesByCategory(Map<String, Double> expensesByCategory) { this.expensesByCategory = expensesByCategory; }
    public Map<String, Boolean> getGoalsCompleted() { return goalsCompleted; }
    public void setGoalsCompleted(Map<String, Boolean> goalsCompleted) { this.goalsCompleted = goalsCompleted; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
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