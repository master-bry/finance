package com.master.finance.service;

import com.master.finance.model.*;
import com.master.finance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private DebtRepository debtRepository;
    
    @Autowired
    private InvestmentRepository investmentRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private DailyEntryRepository dailyEntryRepository;
    
    // Monthly Financial Report
    public Map<String, Object> generateMonthlyReport(String userId, int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
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
        
        // Expenses by category
        Map<String, Double> expensesByCategory = new HashMap<>();
        Map<String, Double> incomeByCategory = new HashMap<>();
        
        for (Transaction t : transactions) {
            if ("EXPENSE".equals(t.getType())) {
                expensesByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
            } else {
                incomeByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }
        
        // Top expenses
        List<Map.Entry<String, Double>> topExpenses = new ArrayList<>(expensesByCategory.entrySet());
        topExpenses.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Daily average spending
        long daysInMonth = endDate.minusDays(1).getDayOfMonth();
        double dailyAverage = daysInMonth > 0 ? totalExpense / daysInMonth : 0;
        
        Map<String, Object> report = new HashMap<>();
        report.put("year", year);
        report.put("month", month);
        report.put("monthName", getMonthName(month));
        report.put("totalIncome", totalIncome);
        report.put("totalExpense", totalExpense);
        report.put("balance", totalIncome - totalExpense);
        report.put("savingsRate", totalIncome > 0 ? ((totalIncome - totalExpense) / totalIncome) * 100 : 0);
        report.put("expensesByCategory", expensesByCategory);
        report.put("incomeByCategory", incomeByCategory);
        report.put("topExpenses", topExpenses.stream().limit(5).toList());
        report.put("dailyAverage", dailyAverage);
        report.put("transactionCount", transactions.size());
        
        return report;
    }
    
    // Yearly Financial Report
    public Map<String, Object> generateYearlyReport(String userId, int year) {
        Map<String, Object> yearlyReport = new HashMap<>();
        List<Map<String, Object>> monthlyReports = new ArrayList<>();
        
        double yearlyIncome = 0;
        double yearlyExpense = 0;
        
        for (int month = 1; month <= 12; month++) {
            Map<String, Object> monthlyReport = generateMonthlyReport(userId, year, month);
            monthlyReports.add(monthlyReport);
            yearlyIncome += (double) monthlyReport.get("totalIncome");
            yearlyExpense += (double) monthlyReport.get("totalExpense");
        }
        
        yearlyReport.put("year", year);
        yearlyReport.put("yearlyIncome", yearlyIncome);
        yearlyReport.put("yearlyExpense", yearlyExpense);
        yearlyReport.put("yearlyBalance", yearlyIncome - yearlyExpense);
        yearlyReport.put("averageMonthlySpending", yearlyExpense / 12);
        yearlyReport.put("monthlyReports", monthlyReports);
        
        return yearlyReport;
    }
    
    // Debt Report
    public Map<String, Object> generateDebtReport(String userId) {
        List<Debt> allDebts = debtRepository.findByUserIdAndDeletedFalse(userId);
        
        List<Debt> owedToMe = allDebts.stream()
                .filter(d -> "OWED_TO_ME".equals(d.getType()) && !"SETTLED".equals(d.getStatus()))
                .toList();
        
        List<Debt> iOwe = allDebts.stream()
                .filter(d -> "I_OWE".equals(d.getType()) && !"SETTLED".equals(d.getStatus()))
                .toList();
        
        double totalOwedToMe = owedToMe.stream().mapToDouble(Debt::getRemainingAmount).sum();
        double totalIOwe = iOwe.stream().mapToDouble(Debt::getRemainingAmount).sum();
        
        // Debts by status
        Map<String, Double> debtsByStatus = new HashMap<>();
        for (Debt d : allDebts) {
            debtsByStatus.merge(d.getStatus(), d.getRemainingAmount(), Double::sum);
        }
        
        // Overdue debts
        LocalDateTime now = LocalDateTime.now();
        List<Debt> overdueDebts = allDebts.stream()
                .filter(d -> d.getDueDate() != null && d.getDueDate().isBefore(now) && !"SETTLED".equals(d.getStatus()))
                .toList();
        
        Map<String, Object> report = new HashMap<>();
        report.put("totalOwedToMe", totalOwedToMe);
        report.put("totalIOwe", totalIOwe);
        report.put("netPosition", totalOwedToMe - totalIOwe);
        report.put("owedToMeList", owedToMe);
        report.put("iOweList", iOwe);
        report.put("debtsByStatus", debtsByStatus);
        report.put("overdueDebts", overdueDebts);
        report.put("totalDebts", allDebts.size());
        report.put("settledDebts", allDebts.stream().filter(d -> "SETTLED".equals(d.getStatus())).count());
        
        return report;
    }
    
    // Investment Report
    public Map<String, Object> generateInvestmentReport(String userId) {
        List<Investment> investments = investmentRepository.findByUserIdAndDeletedFalse(userId);
        
        double totalInvested = investments.stream().mapToDouble(Investment::getAmountInvested).sum();
        double totalCurrentValue = investments.stream().mapToDouble(Investment::getCurrentValue).sum();
        double totalProfitLoss = totalCurrentValue - totalInvested;
        double totalProfitLossPercentage = totalInvested > 0 ? (totalProfitLoss / totalInvested) * 100 : 0;
        
        // By type
        Map<String, Map<String, Double>> byType = new HashMap<>();
        for (Investment inv : investments) {
            byType.computeIfAbsent(inv.getType(), k -> new HashMap<>());
            Map<String, Double> typeData = byType.get(inv.getType());
            typeData.merge("invested", inv.getAmountInvested(), Double::sum);
            typeData.merge("current", inv.getCurrentValue(), Double::sum);
        }
        
        // By risk level
        Map<String, Map<String, Double>> byRisk = new HashMap<>();
        for (Investment inv : investments) {
            byRisk.computeIfAbsent(inv.getRiskLevel(), k -> new HashMap<>());
            Map<String, Double> riskData = byRisk.get(inv.getRiskLevel());
            riskData.merge("invested", inv.getAmountInvested(), Double::sum);
            riskData.merge("current", inv.getCurrentValue(), Double::sum);
        }
        
        // Best and worst performers
        List<Investment> sortedByProfit = new ArrayList<>(investments);
        sortedByProfit.sort((a, b) -> b.getProfitLossPercentage().compareTo(a.getProfitLossPercentage()));
        
        Map<String, Object> report = new HashMap<>();
        report.put("totalInvested", totalInvested);
        report.put("totalCurrentValue", totalCurrentValue);
        report.put("totalProfitLoss", totalProfitLoss);
        report.put("totalProfitLossPercentage", totalProfitLossPercentage);
        report.put("investmentsByType", byType);
        report.put("investmentsByRisk", byRisk);
        report.put("bestPerformers", sortedByProfit.stream().limit(3).toList());
        report.put("worstPerformers", sortedByProfit.stream().skip(Math.max(0, sortedByProfit.size() - 3)).toList());
        report.put("activeInvestments", investments.stream().filter(i -> "ACTIVE".equals(i.getStatus())).count());
        report.put("totalInvestments", investments.size());
        
        return report;
    }
    
    // Goals Progress Report
    public Map<String, Object> generateGoalsReport(String userId) {
        List<Goal> goals = goalRepository.findByUserIdAndDeletedFalse(userId);
        
        List<Goal> activeGoals = goals.stream().filter(g -> !g.isAchieved()).toList();
        List<Goal> achievedGoals = goals.stream().filter(Goal::isAchieved).toList();
        
        double totalTarget = goals.stream().mapToDouble(Goal::getTargetAmount).sum();
        double totalProgress = goals.stream().mapToDouble(Goal::getCurrentAmount).sum();
        
        // Goals by priority
        Map<String, List<Goal>> byPriority = new HashMap<>();
        for (Goal goal : goals) {
            byPriority.computeIfAbsent(goal.getPriority(), k -> new ArrayList<>()).add(goal);
        }
        
        // Goals by category
        Map<String, List<Goal>> byCategory = new HashMap<>();
        for (Goal goal : goals) {
            byCategory.computeIfAbsent(goal.getCategory(), k -> new ArrayList<>()).add(goal);
        }
        
        // At risk goals (target date approaching)
        LocalDateTime threeMonthsFromNow = LocalDateTime.now().plusMonths(3);
        List<Goal> atRiskGoals = activeGoals.stream()
                .filter(g -> g.getTargetDate() != null && g.getTargetDate().isBefore(threeMonthsFromNow))
                .filter(g -> g.getProgressPercentage() < 50)
                .toList();
        
        Map<String, Object> report = new HashMap<>();
        report.put("totalGoals", goals.size());
        report.put("achievedGoals", achievedGoals.size());
        report.put("activeGoals", activeGoals.size());
        report.put("totalTarget", totalTarget);
        report.put("totalProgress", totalProgress);
        report.put("overallProgress", totalTarget > 0 ? (totalProgress / totalTarget) * 100 : 0);
        report.put("goalsByPriority", byPriority);
        report.put("goalsByCategory", byCategory);
        report.put("atRiskGoals", atRiskGoals);
        report.put("activeGoalsList", activeGoals);
        report.put("achievedGoalsList", achievedGoals);
        
        return report;
    }
    
    // Budget vs Actual Report
    public Map<String, Object> generateBudgetVsActualReport(String userId, String month) {
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonth(userId, month);
        
        if (budgetOpt.isEmpty()) {
            Map<String, Object> emptyReport = new HashMap<>();
            emptyReport.put("message", "No budget found for " + month);
            return emptyReport;
        }
        
        Budget budget = budgetOpt.get();
        LocalDateTime startDate = LocalDateTime.parse(month + "-01T00:00:00");
        LocalDateTime endDate = startDate.plusMonths(1);
        
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, startDate, endDate);
        
        Map<String, Double> actualExpenses = new HashMap<>();
        for (Transaction t : transactions) {
            if ("EXPENSE".equals(t.getType())) {
                actualExpenses.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }
        
        List<Map<String, Object>> categoryComparisons = new ArrayList<>();
        double totalVariance = 0;
        
        if (budget.getCategoryBudgets() != null) {
            for (Map.Entry<String, Budget.CategoryBudget> entry : budget.getCategoryBudgets().entrySet()) {
                String category = entry.getKey();
                Budget.CategoryBudget catBudget = entry.getValue();
                double actual = actualExpenses.getOrDefault(category, 0.0);
                double variance = actual - catBudget.getPlanned();
                totalVariance += variance;
                
                Map<String, Object> comparison = new HashMap<>();
                comparison.put("category", category);
                comparison.put("planned", catBudget.getPlanned());
                comparison.put("actual", actual);
                comparison.put("variance", variance);
                comparison.put("variancePercentage", catBudget.getPlanned() > 0 ? (variance / catBudget.getPlanned()) * 100 : 0);
                comparison.put("status", variance > 0 ? "OVER" : variance < 0 ? "UNDER" : "ON_TRACK");
                categoryComparisons.add(comparison);
            }
        }
        
        Map<String, Object> report = new HashMap<>();
        report.put("month", month);
        report.put("totalIncomePlanned", budget.getTotalIncome());
        report.put("totalIncomeActual", transactions.stream().filter(t -> "INCOME".equals(t.getType())).mapToDouble(Transaction::getAmount).sum());
        report.put("totalExpensePlanned", budget.getTotalExpense());
        report.put("totalExpenseActual", actualExpenses.values().stream().mapToDouble(Double::doubleValue).sum());
        report.put("totalVariance", totalVariance);
        report.put("categoryComparisons", categoryComparisons);
        report.put("savingsTarget", budget.getSavingsTarget());
        report.put("savingsActual", budget.getActualSavings());
        
        return report;
    }
    
    // Cash Flow Report
    public Map<String, Object> generateCashFlowReport(String userId, int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1);
        
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, startDate, endDate);
        
        // Daily cash flow
        Map<String, Map<String, Double>> dailyCashFlow = new LinkedHashMap<>();
        for (int day = 1; day <= endDate.minusDays(1).getDayOfMonth(); day++) {
            Map<String, Double> daily = new HashMap<>();
            daily.put("income", 0.0);
            daily.put("expense", 0.0);
            dailyCashFlow.put(String.valueOf(day), daily);
        }
        
        for (Transaction t : transactions) {
            String day = String.valueOf(t.getDate().getDayOfMonth());
            if (dailyCashFlow.containsKey(day)) {
                if ("INCOME".equals(t.getType())) {
                    dailyCashFlow.get(day).merge("income", t.getAmount(), Double::sum);
                } else {
                    dailyCashFlow.get(day).merge("expense", t.getAmount(), Double::sum);
                }
            }
        }
        
        // Weekly summary
        Map<Integer, Map<String, Double>> weeklySummary = new HashMap<>();
        for (Transaction t : transactions) {
            int week = (t.getDate().getDayOfMonth() - 1) / 7 + 1;
            weeklySummary.computeIfAbsent(week, k -> {
                Map<String, Double> w = new HashMap<>();
                w.put("income", 0.0);
                w.put("expense", 0.0);
                return w;
            });
            
            if ("INCOME".equals(t.getType())) {
                weeklySummary.get(week).merge("income", t.getAmount(), Double::sum);
            } else {
                weeklySummary.get(week).merge("expense", t.getAmount(), Double::sum);
            }
        }
        
        Map<String, Object> report = new HashMap<>();
        report.put("year", year);
        report.put("month", month);
        report.put("dailyCashFlow", dailyCashFlow);
        report.put("weeklySummary", weeklySummary);
        report.put("totalIncome", transactions.stream().filter(t -> "INCOME".equals(t.getType())).mapToDouble(Transaction::getAmount).sum());
        report.put("totalExpense", transactions.stream().filter(t -> "EXPENSE".equals(t.getType())).mapToDouble(Transaction::getAmount).sum());
        
        return report;
    }
    
    // Export data as CSV
    public String exportTransactionsToCsv(String userId, int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1);
        
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, startDate, endDate);
        
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Description,Category,Amount,Type,Notes\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Transaction t : transactions) {
            csv.append(t.getDate().format(formatter)).append(",")
               .append(escapeCsv(t.getDescription())).append(",")
               .append(escapeCsv(t.getCategory())).append(",")
               .append(t.getAmount()).append(",")
               .append(t.getType()).append(",")
               .append(escapeCsv(t.getNotes())).append("\n");
        }
        
        return csv.toString();
    }
    
    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June", 
                           "July", "August", "September", "October", "November", "December"};
        return months[month - 1];
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}