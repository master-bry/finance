package com.master.finance.service;

import com.master.finance.model.Budget;
import com.master.finance.model.Transaction;
import com.master.finance.repository.BudgetRepository;
import com.master.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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

    /**
     * Save or update a budget. Ensures only one budget exists per user per month.
     */
    public Budget saveBudget(Budget budget, String userId) {
        String month = budget.getMonth();
        
        // Find existing budgets for this user and month
        List<Budget> existingBudgets = budgetRepository.findByUserIdAndMonth(userId, month);
        
        Budget targetBudget;
        if (!existingBudgets.isEmpty()) {
            // Use the most recently updated one (or first)
            targetBudget = existingBudgets.stream()
                    .max(Comparator.comparing(Budget::getUpdatedAt))
                    .orElse(existingBudgets.get(0));
            
            // Soft delete any other duplicates
            for (Budget dup : existingBudgets) {
                if (!dup.getId().equals(targetBudget.getId())) {
                    dup.setDeleted(true);
                    dup.setDeletedAt(LocalDateTime.now());
                    budgetRepository.save(dup);
                }
            }
        } else {
            targetBudget = new Budget();
            targetBudget.setUserId(userId);
            targetBudget.setMonth(month);
            targetBudget.setCreatedAt(LocalDateTime.now());
        }
        
        // Update fields from incoming budget
        targetBudget.setTotalIncome(budget.getTotalIncome());
        targetBudget.setTotalExpense(budget.getTotalExpense());
        targetBudget.setSavingsTarget(budget.getSavingsTarget());
        targetBudget.setNotes(budget.getNotes());
        targetBudget.setUpdatedAt(LocalDateTime.now());
        
        // Merge category budgets
        if (budget.getCategoryBudgets() != null) {
            Map<String, Budget.CategoryBudget> existingCats = targetBudget.getCategoryBudgets();
            if (existingCats == null) {
                existingCats = new HashMap<>();
                targetBudget.setCategoryBudgets(existingCats);
            }
            for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                Budget.CategoryBudget incomingCat = entry.getValue();
                Budget.CategoryBudget targetCat = existingCats.get(entry.getKey());
                if (targetCat == null) {
                    targetCat = new Budget.CategoryBudget();
                    existingCats.put(entry.getKey(), targetCat);
                }
                targetCat.setPlanned(incomingCat.getPlanned());
                targetCat.setNotes(incomingCat.getNotes());
            }
        }
        
        // Recalculate actuals from transactions
        updateActualsFromTransactions(targetBudget);
        
        // Check alerts
        checkBudgetAlerts(targetBudget, userId);
        
        return budgetRepository.save(targetBudget);
    }

    /**
     * Update actual spending/income from transactions.
     */
    private void updateActualsFromTransactions(Budget budget) {
        String month = budget.getMonth();
        LocalDateTime startDate = LocalDateTime.parse(month + "-01T00:00:00");
        LocalDateTime endDate = startDate.plusMonths(1);
        
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndDateBetweenAndDeletedFalse(budget.getUserId(), startDate, endDate);
        
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
        budget.setActualSavings(totalIncome - totalExpense);
        
        // Update category actuals
        Map<String, Double> actualByCategory = new HashMap<>();
        for (Transaction t : transactions) {
            if ("EXPENSE".equals(t.getType())) {
                actualByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }
        
        if (budget.getCategoryBudgets() != null) {
            for (Budget.CategoryBudget cat : budget.getCategoryBudgets().values()) {
                cat.setActual(0.0);
            }
            for (Map.Entry<String, Double> entry : actualByCategory.entrySet()) {
                Budget.CategoryBudget cat = budget.getCategoryBudgets().get(entry.getKey());
                if (cat != null) {
                    cat.setActual(entry.getValue());
                }
            }
        }
    }

    public Optional<Budget> getBudget(String userId, String month) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonth(userId, month);
        // Return the latest (by updatedAt) or first
        return budgets.stream().max(Comparator.comparing(Budget::getUpdatedAt));
    }

    public Budget getCurrentMonthBudget(String userId) {
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Optional<Budget> existing = getBudget(userId, currentMonth);
        return existing.orElseGet(() -> createDefaultBudget(userId, currentMonth));
    }

    public Budget getPreviousMonthBudget(String userId) {
        String previousMonth = LocalDateTime.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return getBudget(userId, previousMonth).orElse(null);
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
        Optional<Budget> budgetOpt = getBudget(userId, month);
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
                    comparison.put("status", entry.getValue().getStatus());
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
            Optional<Budget> budgetOpt = getBudget(userId, monthStr);
            
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

    /**
     * Update budget actuals for a specific month (public method for transaction sync)
     */
    public void updateBudgetActuals(String userId, String month) {
        Optional<Budget> budgetOpt = getBudget(userId, month);
        if (budgetOpt.isPresent()) {
            Budget budget = budgetOpt.get();
            updateActualsFromTransactions(budget);
            budgetRepository.save(budget);
            
            // Check for alerts after updating
            checkBudgetAlerts(budget, userId);
        }
    }

    /**
     * Auto-create monthly budgets for all users (runs on 1st of each month)
     */
    @Scheduled(cron = "0 0 0 1 * ?") // 1st of every month at midnight
    public void createMonthlyBudgetsForAllUsers() {
        try {
            String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            // This would require getting all users - for now, we'll create on-demand
            // Implementation would depend on UserService.getAllUsers() method
            System.out.println("Monthly budget creation scheduled for: " + currentMonth);
        } catch (Exception e) {
            System.err.println("Error in monthly budget creation: " + e.getMessage());
        }
    }

    /**
     * Enhanced budget alerts with multiple warning levels
     */
    private void checkBudgetAlerts(Budget budget, String userId) {
        List<String> alerts = new ArrayList<>();
        
        if (budget.getCategoryBudgets() != null) {
            for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                String category = entry.getKey();
                Budget.CategoryBudget catBudget = entry.getValue();
                
                if (catBudget.getPlanned() > 0) {
                    double percentage = (catBudget.getActual() / catBudget.getPlanned()) * 100;
                    
                    // Warning at 80%
                    if (percentage >= 80 && percentage < 100) {
                        alerts.add(String.format("⚠️ %s budget at %.1f%% (%.0f TZS remaining)", 
                                category, percentage, catBudget.getPlanned() - catBudget.getActual()));
                    }
                    // Critical at 100%
                    else if (percentage >= 100 && percentage < 120) {
                        alerts.add(String.format("🔴 %s budget exceeded by %.0f TZS!", 
                                category, catBudget.getActual() - catBudget.getPlanned()));
                    }
                    // Emergency at 120%+
                    else if (percentage >= 120) {
                        alerts.add(String.format("🚨 EMERGENCY: %s budget 20%%+ over! Stop spending!", category));
                    }
                }
            }
        }
        
        // Overall budget alerts
        if (budget.getTotalExpense() > budget.getTotalIncome() && budget.getTotalIncome() > 0) {
            double deficit = budget.getTotalExpense() - budget.getTotalIncome();
            alerts.add(String.format("💸 Monthly deficit: %.0f TZS. Review expenses!", deficit));
        }
        
        // Savings target alerts
        if (budget.getSavingsTarget() > 0) {
            double savingsRate = budget.getSavingsRate();
            if (savingsRate < 0) {
                alerts.add("💰 No savings this month. Focus on reducing expenses!");
            } else if (savingsRate < (budget.getSavingsTarget() / budget.getTotalIncome()) * 100) {
                double shortfall = budget.getSavingsTarget() - budget.getActualSavings();
                alerts.add(String.format("🎯 %.0f TZS short of savings target", shortfall));
            }
        }
        
        if (!alerts.isEmpty()) {
            userService.addNotifications(userId, alerts);
        }
    }

    /**
     * Get budget recommendations based on spending history
     */
    public Map<String, Object> getBudgetRecommendations(String userId) {
        Map<String, Object> recommendations = new HashMap<>();
        
        // Get last 3 months of spending data
        List<Map<String, Object>> monthlySpending = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            String month = LocalDateTime.now().minusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            Map<String, Object> monthData = getBudgetVsActual(userId, month);
            if (monthData.containsKey("budget")) {
                monthlySpending.add(monthData);
            }
        }
        
        if (monthlySpending.isEmpty()) {
            recommendations.put("message", "No spending history available for recommendations");
            return recommendations;
        }
        
        // Calculate average spending by category
        Map<String, Double> avgCategorySpending = new HashMap<>();
        Map<String, Integer> categoryCount = new HashMap<>();
        
        for (Map<String, Object> monthData : monthlySpending) {
            Budget budget = (Budget) monthData.get("budget");
            if (budget.getCategoryBudgets() != null) {
                for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                    String category = entry.getKey();
                    Double actual = entry.getValue().getActual();
                    if (actual > 0) {
                        avgCategorySpending.merge(category, actual, Double::sum);
                        categoryCount.merge(category, 1, Integer::sum);
                    }
                }
            }
        }
        
        // Calculate averages
        for (String category : avgCategorySpending.keySet()) {
            avgCategorySpending.put(category, avgCategorySpending.get(category) / categoryCount.get(category));
        }
        
        // Calculate average income and expenses
        double avgIncome = monthlySpending.stream()
                .mapToDouble(m -> ((Budget) m.get("budget")).getTotalIncome())
                .average().orElse(0.0);
        
        double avgExpense = monthlySpending.stream()
                .mapToDouble(m -> ((Budget) m.get("budget")).getTotalExpense())
                .average().orElse(0.0);
        
        double avgSavings = avgIncome - avgExpense;
        
        // Generate recommendations
        List<String> recommendationsList = new ArrayList<>();
        
        // Income recommendation
        if (avgIncome > 0) {
            recommendationsList.add(String.format("💰 Based on history, plan for income of %.0f TZS", avgIncome));
        }
        
        // Expense recommendations
        if (avgExpense > 0) {
            double recommendedExpense = avgIncome * 0.8; // 80% of income
            recommendationsList.add(String.format("💸 Consider budgeting %.0f TZS for expenses (80%% of income)", recommendedExpense));
        }
        
        // Category recommendations
        for (Map.Entry<String, Double> entry : avgCategorySpending.entrySet()) {
            String category = entry.getKey();
            Double avgSpending = entry.getValue();
            
            // Add 10% buffer for unexpected expenses
            double recommendedBudget = avgSpending * 1.1;
            recommendationsList.add(String.format("📊 %s: Budget %.0f TZS (based on %.0f TZS average)", 
                    category, recommendedBudget, avgSpending));
        }
        
        // Savings recommendation
        if (avgSavings > 0) {
            double recommendedSavings = avgIncome * 0.2; // 20% savings rate
            recommendationsList.add(String.format("🎯 Target savings of %.0f TZS (20%% of income)", recommendedSavings));
        } else {
            recommendationsList.add("💡 Focus on reducing expenses to achieve positive savings");
        }
        
        // High spending alerts
        for (Map.Entry<String, Double> entry : avgCategorySpending.entrySet()) {
            String category = entry.getKey();
            Double avgSpending = entry.getValue();
            
            if (avgIncome > 0 && (avgSpending / avgIncome) > 0.3) { // More than 30% of income
                recommendationsList.add(String.format("⚠️ %s consumes %.1f%% of income - consider reducing", 
                        category, (avgSpending / avgIncome) * 100));
            }
        }
        
        recommendations.put("recommendations", recommendationsList);
        recommendations.put("avgIncome", avgIncome);
        recommendations.put("avgExpense", avgExpense);
        recommendations.put("avgSavings", avgSavings);
        recommendations.put("categoryAverages", avgCategorySpending);
        recommendations.put("monthsAnalyzed", monthlySpending.size());
        
        return recommendations;
    }

    /**
     * Utility method to clean duplicates (call once if needed).
     */
    public void cleanDuplicates(String userId, String month) {
        List<Budget> budgets = budgetRepository.findByUserIdAndMonth(userId, month);
        if (budgets.size() > 1) {
            // Keep the most recently updated, soft delete others
            Budget keeper = budgets.stream().max(Comparator.comparing(Budget::getUpdatedAt)).orElse(budgets.get(0));
            for (Budget b : budgets) {
                if (!b.getId().equals(keeper.getId())) {
                    b.setDeleted(true);
                    b.setDeletedAt(LocalDateTime.now());
                    budgetRepository.save(b);
                }
            }
        }
    }
}