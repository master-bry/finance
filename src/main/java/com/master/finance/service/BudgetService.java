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
    
    public Budget saveBudget(Budget budget, String userId) {
        budget.setUserId(userId);
        budget.setUpdatedAt(LocalDateTime.now());
        
        String month = budget.getMonth();
        LocalDateTime startDate = LocalDateTime.parse(month + "-01T00:00:00");
        LocalDateTime endDate = startDate.plusMonths(1);
        
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, startDate, endDate);
        
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
        
        Map<String, Double> actualByCategory = new HashMap<>();
        for (Transaction t : transactions) {
            if ("EXPENSE".equals(t.getType())) {
                actualByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }
        
        double actualSavings = totalIncome - totalExpense;
        budget.setActualSavings(actualSavings);
        
        if (budget.getCategoryBudgets() != null) {
            for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                String category = entry.getKey();
                Budget.CategoryBudget catBudget = entry.getValue();
                catBudget.setActual(actualByCategory.getOrDefault(category, 0.0));
            }
        }
        
        checkBudgetAlerts(budget, userId);
        
        return budgetRepository.save(budget);
    }
    
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
                
                if (catBudget.getPlanned() > 0 && catBudget.getActual() > catBudget.getPlanned() * 1.2) {
                    alerts.add(String.format("🔴 CRITICAL: %s budget is 20%% over! Review your spending.", category));
                }
            }
        }
        
        if (budget.getTotalExpense() > budget.getTotalIncome() && budget.getTotalIncome() > 0) {
            double deficit = budget.getTotalExpense() - budget.getTotalIncome();
            alerts.add(String.format("⚠️ Your expenses exceed your income by %.2f TZS this month!", deficit));
        }
        
        if (budget.getSavingsTarget() > 0 && budget.getActualSavings() < budget.getSavingsTarget()) {
            double shortfall = budget.getSavingsTarget() - budget.getActualSavings();
            alerts.add(String.format("💰 You're %.2f TZS short of your savings target this month!", shortfall));
        }
        
        if (!alerts.isEmpty()) {
            userService.addNotifications(userId, alerts);
        }
    }
    
    public Optional<Budget> getBudget(String userId, String month) {
        return budgetRepository.findByUserIdAndMonth(userId, month);
    }
    
    public Budget getCurrentMonthBudget(String userId) {
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return budgetRepository.findByUserIdAndMonth(userId, currentMonth)
                .orElseGet(() -> createDefaultBudget(userId, currentMonth));
    }
    
    public Budget getPreviousMonthBudget(String userId) {
        String previousMonth = LocalDateTime.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return budgetRepository.findByUserIdAndMonth(userId, previousMonth)
                .orElse(null);
    }
    
    private Budget createDefaultBudget(String userId, String month) {
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setMonth(month);
        budget.setTotalIncome(0.0);
        budget.setTotalExpense(0.0);
        budget.setSavingsTarget(0.0);
        budget.setActualSavings(0.0);
        budget.setDeleted(false);
        
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
    
    public List<Budget> getBudgetHistory(String userId, int months) {
        List<Budget> allBudgets = budgetRepository.findByUserIdOrderByMonthDesc(userId);
        return allBudgets.stream().limit(months).toList();
    }
    
    public Map<String, Object> getYearlyBudgetSummary(String userId, int year) {
        Map<String, Object> yearlySummary = new HashMap<>();
        List<Map<String, Object>> monthlyData = new ArrayList<>();
        
        double totalIncomePlanned = 0;
        double totalExpensePlanned = 0;
        double totalSavingsTarget = 0;
        
        for (int month = 1; month <= 12; month++) {
            String monthStr = String.format("%d-%02d", year, month);
            Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonth(userId, monthStr);
            
            if (budgetOpt.isPresent()) {
                Budget budget = budgetOpt.get();
                totalIncomePlanned += budget.getTotalIncome();
                totalExpensePlanned += budget.getTotalExpense();
                totalSavingsTarget += budget.getSavingsTarget();
                
                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", month);
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
        yearlySummary.put("monthlyData", monthlyData);
        
        return yearlySummary;
    }
    
    public void softDeleteBudget(String budgetId) {
        budgetRepository.findById(budgetId).ifPresent(budget -> {
            budget.setDeleted(true);
            budget.setDeletedAt(LocalDateTime.now());
            budgetRepository.save(budget);
        });
    }
}