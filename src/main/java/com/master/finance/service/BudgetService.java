package com.master.finance.service;

import com.master.finance.model.Budget;
import com.master.finance.model.Transaction;
import com.master.finance.repository.BudgetRepository;
import com.master.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BudgetService {
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserService userService;
    
    // Save or update budget
    public Budget saveBudget(Budget budget, String userId) {
        budget.setUserId(userId);
        budget.setUpdatedAt(LocalDateTime.now());
        
        String month = budget.getMonth();
        LocalDateTime startDate = LocalDateTime.parse(month + "-01T00:00:00");
        LocalDateTime endDate = startDate.plusMonths(1);
        
        // Get actual transactions for the month
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, startDate, endDate);
        
        // Calculate actual totals
        double totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        double totalExpense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        budget.setTotalIncome(totalIncome);
        budget.setTotalExpense(totalExpense);
        
        // Calculate actual expenses by category
        Map<String, Double> actualByCategory = new HashMap<>();
        for (Transaction t : transactions) {
            if ("EXPENSE".equals(t.getType())) {
                actualByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }
        
        // Update category budgets with actual amounts
        if (budget.getCategoryBudgets() != null) {
            for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                String category = entry.getKey();
                Budget.CategoryBudget catBudget = entry.getValue();
                catBudget.setActual(actualByCategory.getOrDefault(category, 0.0));
            }
        }
        
        // Calculate actual savings
        double actualSavings = totalIncome - totalExpense;
        budget.setActualSavings(actualSavings);
        
        // Check for budget alerts
        checkBudgetAlerts(budget, userId);
        
        return budgetRepository.save(budget);
    }
    
    // Check if any category is over budget and send alerts
    private void checkBudgetAlerts(Budget budget, String userId) {
        List<String> alerts = new ArrayList<>();
        
        if (budget.getCategoryBudgets() != null) {
            for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                String category = entry.getKey();
                Budget.CategoryBudget catBudget = entry.getValue();
                
                if (catBudget.getPlanned() > 0 && catBudget.getActual() > catBudget.getPlanned()) {
                    double overAmount = catBudget.getActual() - catBudget.getPlanned();
                    double percentage = (overAmount / catBudget.getPlanned()) * 100;
                    alerts.add(String.format("⚠️ You've exceeded your %s budget by %.2f TZS (%.1f%%)!", 
                             category, overAmount, percentage));
                }
                
                // Critical alert for 20% over budget
                if (catBudget.getPlanned() > 0 && catBudget.getActual() > catBudget.getPlanned() * 1.2) {
                    alerts.add(String.format("🔴 CRITICAL: %s budget is 20%% over! Review your spending.", category));
                }
            }
        }
        
        // Check total budget vs income
        if (budget.getTotalExpense() > budget.getTotalIncome() && budget.getTotalIncome() > 0) {
            double deficit = budget.getTotalExpense() - budget.getTotalIncome();
            alerts.add(String.format("⚠️ Your expenses exceed your income by %.2f TZS this month!", deficit));
        }
        
        // Check savings target
        if (budget.getSavingsTarget() > 0 && budget.getActualSavings() < budget.getSavingsTarget()) {
            double shortfall = budget.getSavingsTarget() - budget.getActualSavings();
            alerts.add(String.format("💰 You're %.2f TZS short of your savings target this month!", shortfall));
        }
        
        // Add alerts to user notifications
        if (!alerts.isEmpty()) {
            userService.addNotifications(userId, alerts);
        }
    }
    
    // Get budget for specific month
    public Optional<Budget> getBudget(String userId, String month) {
        return budgetRepository.findByUserIdAndMonth(userId, month);
    }
    
    // Get current month budget
    public Budget getCurrentMonthBudget(String userId) {
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return budgetRepository.findByUserIdAndMonth(userId, currentMonth)
                .orElseGet(() -> createDefaultBudget(userId, currentMonth));
    }
    
    // Get previous month budget
    public Budget getPreviousMonthBudget(String userId) {
        String previousMonth = LocalDateTime.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return budgetRepository.findByUserIdAndMonth(userId, previousMonth)
                .orElse(null);
    }
    
    // Create default budget for a user
    private Budget createDefaultBudget(String userId, String month) {
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setMonth(month);
        budget.setTotalIncome(0.0);
        budget.setTotalExpense(0.0);
        budget.setSavingsTarget(0.0);
        budget.setActualSavings(0.0);
        budget.setDeleted(false);
        
        // Default categories
        Map<String, Budget.CategoryBudget> defaults = new LinkedHashMap<>();
        String[] categories = {"Food", "Transport", "Rent", "Utilities", "Entertainment", 
                               "Shopping", "Healthcare", "Education", "Savings", "Other"};
        
        for (String category : categories) {
            Budget.CategoryBudget catBudget = new Budget.CategoryBudget();
            catBudget.setPlanned(0.0);
            catBudget.setActual(0.0);
            defaults.put(category, catBudget);
        }
        
        budget.setCategoryBudgets(defaults);
        return budgetRepository.save(budget);
    }
    
    // Get budget vs actual comparison report
    public Map<String, Object> getBudgetVsActual(String userId, String month) {
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonth(userId, month);
        
        Map<String, Object> report = new HashMap<>();
        
        if (budgetOpt.isPresent()) {
            Budget budget = budgetOpt.get();
            List<Map<String, Object>> categoryComparisons = new ArrayList<>();
            
            if (budget.getCategoryBudgets() != null) {
                for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                    Map<String, Object> comparison = new HashMap<>();
                    comparison.put("category", entry.getKey());
                    comparison.put("planned", entry.getValue().getPlanned());
                    comparison.put("actual", entry.getValue().getActual());
                    comparison.put("variance", entry.getValue().getVariance());
                    comparison.put("variancePercentage", entry.getValue().getVariancePercentage());
                    comparison.put("status", entry.getValue().getVariance() > 0 ? "OVER" : 
                                     entry.getValue().getVariance() < 0 ? "UNDER" : "ON_TRACK");
                    categoryComparisons.add(comparison);
                }
            }
            
            report.put("comparisons", categoryComparisons);
            report.put("totalIncomePlanned", budget.getTotalIncome());
            report.put("totalExpensePlanned", budget.getTotalExpense());
            report.put("totalIncomeActual", budget.getTotalIncome());
            report.put("totalExpenseActual", budget.getTotalExpense());
            report.put("savingsTarget", budget.getSavingsTarget());
            report.put("actualSavings", budget.getActualSavings());
            report.put("month", month);
            report.put("budget", budget);
        } else {
            report.put("message", "No budget found for " + month);
        }
        
        return report;
    }
    
    // Get budget history for last N months
    public List<Budget> getBudgetHistory(String userId, int months) {
        List<Budget> allBudgets = budgetRepository.findByUserIdOrderByMonthDesc(userId);
        return allBudgets.stream().limit(months).toList();
    }
    
    // Get yearly budget summary
    public Map<String, Object> getYearlyBudgetSummary(String userId, int year) {
        Map<String, Object> yearlySummary = new HashMap<>();
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        
        double totalIncomePlanned = 0;
        double totalExpensePlanned = 0;
        double totalSavingsTarget = 0;
        double totalActualIncome = 0;
        double totalActualExpense = 0;
        
        for (int month = 1; month <= 12; month++) {
            String monthStr = String.format("%d-%02d", year, month);
            Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonth(userId, monthStr);
            
            if (budgetOpt.isPresent()) {
                Budget budget = budgetOpt.get();
                totalIncomePlanned += budget.getTotalIncome();
                totalExpensePlanned += budget.getTotalExpense();
                totalSavingsTarget += budget.getSavingsTarget();
                totalActualIncome += budget.getTotalIncome();
                totalActualExpense += budget.getTotalExpense();
                
                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", month);
                monthData.put("monthName", getMonthName(month));
                monthData.put("income", budget.getTotalIncome());
                monthData.put("expense", budget.getTotalExpense());
                monthData.put("savings", budget.getActualSavings());
                monthData.put("savingsTarget", budget.getSavingsTarget());
                monthlyData.add(monthData);
            }
        }
        
        yearlySummary.put("year", year);
        yearlySummary.put("totalIncomePlanned", totalIncomePlanned);
        yearlySummary.put("totalExpensePlanned", totalExpensePlanned);
        yearlySummary.put("totalSavingsTarget", totalSavingsTarget);
        yearlySummary.put("totalActualIncome", totalActualIncome);
        yearlySummary.put("totalActualExpense", totalActualExpense);
        yearlySummary.put("totalActualSavings", totalActualIncome - totalActualExpense);
        yearlySummary.put("monthlyData", monthlyData);
        
        return yearlySummary;
    }
    
    // Soft delete budget
    public void softDeleteBudget(String budgetId) {
        budgetRepository.findById(budgetId).ifPresent(budget -> {
            budget.setDeleted(true);
            budgetRepository.save(budget);
        });
    }
    
    // Helper method to get month name
    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June", 
                           "July", "August", "September", "October", "November", "December"};
        return months[month - 1];
    }
}