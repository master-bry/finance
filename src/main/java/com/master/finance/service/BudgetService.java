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
    
    // Create or update budget for a month
    public Budget saveBudget(Budget budget, String userId) {
        budget.setUserId(userId);
        budget.setUpdatedAt(LocalDateTime.now());
        
        // Calculate actual expenses from transactions
        String month = budget.getMonth();
        List<Transaction> transactions = transactionRepository.findByUserIdAndMonth(userId, month);
        
        double totalExpense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        budget.setTotalExpense(totalExpense);
        
        // Calculate actual per category
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
        
        // Check for budget alerts
        checkBudgetAlerts(budget, userId);
        
        return budgetRepository.save(budget);
    }
    
    // Check if any category is over budget
    private void checkBudgetAlerts(Budget budget, String userId) {
        List<String> alerts = new ArrayList<>();
        
        if (budget.getCategoryBudgets() != null) {
            for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                String category = entry.getKey();
                Budget.CategoryBudget catBudget = entry.getValue();
                
                if (catBudget.getActual() > catBudget.getPlanned()) {
                    double overAmount = catBudget.getActual() - catBudget.getPlanned();
                    alerts.add(String.format("⚠️ You've exceeded your %s budget by %.2f TZS!", 
                             category, overAmount));
                }
            }
        }
        
        // Check total budget
        if (budget.getTotalExpense() > budget.getTotalIncome()) {
            alerts.add("⚠️ Your expenses exceed your income for this month!");
        }
        
        // Save alerts to user
        if (!alerts.isEmpty()) {
            userService.addNotifications(userId, alerts);
        }
    }
    
    // Get current month budget
    public Budget getCurrentMonthBudget(String userId) {
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return budgetRepository.findByUserIdAndMonth(userId, currentMonth)
                .orElseGet(() -> createDefaultBudget(userId, currentMonth));
    }
    
    // Create default budget
    private Budget createDefaultBudget(String userId, String month) {
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setMonth(month);
        budget.setTotalIncome(0.0);
        budget.setTotalExpense(0.0);
        budget.setSavingsTarget(0.0);
        
        // Default category budgets
        Map<String, Budget.CategoryBudget> defaults = new HashMap<>();
        String[] categories = {"Food", "Transport", "Rent", "Utilities", "Entertainment", "Shopping", "Healthcare"};
        
        for (String category : categories) {
            Budget.CategoryBudget catBudget = new Budget.CategoryBudget();
            catBudget.setPlanned(0.0);
            catBudget.setActual(0.0);
            defaults.put(category, catBudget);
        }
        
        budget.setCategoryBudgets(defaults);
        return budget;
    }
    
    // Generate budget vs actual report
    public Map<String, Object> getBudgetVsActual(String userId, String month) {
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, month)
                .orElse(null);
        
        Map<String, Object> report = new HashMap<>();
        
        if (budget != null && budget.getCategoryBudgets() != null) {
            List<Map<String, Object>> categoryComparisons = new ArrayList<>();
            
            for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                Map<String, Object> comparison = new HashMap<>();
                comparison.put("category", entry.getKey());
                comparison.put("planned", entry.getValue().getPlanned());
                comparison.put("actual", entry.getValue().getActual());
                comparison.put("variance", entry.getValue().getVariance());
                comparison.put("variancePercentage", entry.getValue().getVariancePercentage());
                comparison.put("status", entry.getValue().getVariance() > 0 ? "OVER" : "UNDER");
                categoryComparisons.add(comparison);
            }
            
            report.put("comparisons", categoryComparisons);
            report.put("totalPlanned", budget.getTotalIncome());
            report.put("totalActual", budget.getTotalExpense());
            report.put("savingsTarget", budget.getSavingsTarget());
            report.put("actualSavings", budget.getActualSavings());
        }
        
        return report;
    }
}