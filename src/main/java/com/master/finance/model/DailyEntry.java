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
    
    public DailyEntry() {
        this.date = LocalDateTime.now();
        this.completed = false;
        this.totalIncome = 0.0;
        this.totalExpense = 0.0;
        this.savings = 0.0;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public Double getTotalIncome() { return totalIncome; }
    public void setTotalIncome(Double totalIncome) { this.totalIncome = totalIncome; }
    
    public Double getTotalExpense() { return totalExpense; }
    public void setTotalExpense(Double totalExpense) { this.totalExpense = totalExpense; }
    
    public Double getSavings() { return savings; }
    public void setSavings(Double savings) { this.savings = savings; }
    
    public Map<String, Double> getExpensesByCategory() { return expensesByCategory; }
    public void setExpensesByCategory(Map<String, Double> expensesByCategory) { this.expensesByCategory = expensesByCategory; }
    
    public Map<String, Boolean> getGoalsCompleted() { return goalsCompleted; }
    public void setGoalsCompleted(Map<String, Boolean> goalsCompleted) { this.goalsCompleted = goalsCompleted; }
    
    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }
    
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}