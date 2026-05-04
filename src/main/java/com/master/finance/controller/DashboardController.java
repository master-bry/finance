package com.master.finance.controller;

import com.master.finance.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private DebtService debtService;

    @Autowired
    private InvestmentService investmentService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, 
                           @RequestParam(required = false) Integer month,
                           @RequestParam(required = false) Integer year,
                           Model model) {
        String username = authentication.getName();
        var user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String userId = user.getId();

        // Default to current month if not specified
        YearMonth selectedPeriod = (month != null && year != null) 
                ? YearMonth.of(year, month)
                : YearMonth.now();
        
        LocalDateTime startOfMonth = selectedPeriod.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = selectedPeriod.atEndOfMonth().atTime(23, 59, 59);
        LocalDateTime now = LocalDateTime.now();

        System.out.println("Dashboard: userId=" + userId);
        System.out.println("Dashboard: period=" + selectedPeriod + ", startOfMonth=" + startOfMonth + ", endOfMonth=" + endOfMonth);

        double totalIncome = transactionService.getTotalIncome(userId, startOfMonth, endOfMonth);
        double totalExpense = transactionService.getTotalExpense(userId, startOfMonth, endOfMonth);
        
        // Get previous month balance to carry forward
        double previousMonthBalance = getPreviousMonthBalance(userId, selectedPeriod);
        double currentMonthBalance = totalIncome - totalExpense;
        double totalBalance = previousMonthBalance + currentMonthBalance;
        
        double savingsRate = totalIncome > 0 ? (currentMonthBalance / totalIncome) * 100 : 0;

        System.out.println("Dashboard: totalIncome=" + totalIncome + ", totalExpense=" + totalExpense + ", currentMonthBalance=" + currentMonthBalance + ", previousMonthBalance=" + previousMonthBalance + ", totalBalance=" + totalBalance);

        double totalOwedToMe = debtService.getTotalOwedToMe(userId);
        double totalIOwe = debtService.getTotalIOwe(userId);
        double netDebtPosition = totalOwedToMe - totalIOwe;

        var investments = investmentService.getUserInvestments(userId);
        double totalInvested = investments.stream()
                .mapToDouble(i -> i.getAmountInvested() != null ? i.getAmountInvested() : 0)
                .sum();
        double totalCurrentValue = investments.stream()
                .mapToDouble(i -> i.getCurrentValue() != null ? i.getCurrentValue() : 0)
                .sum();
        double totalProfitLoss = totalCurrentValue - totalInvested;
        double profitLossPercentage = totalInvested > 0 ? (totalProfitLoss / totalInvested) * 100 : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncome", totalIncome);
        stats.put("totalExpense", totalExpense);
        stats.put("balance", totalBalance); // Use total balance including previous month
        stats.put("currentMonthBalance", currentMonthBalance); // Current month only
        stats.put("previousMonthBalance", previousMonthBalance); // Previous month carry forward
        stats.put("savingsRate", Math.round(savingsRate));
        stats.put("totalOwedToMe", totalOwedToMe);
        stats.put("totalIOwe", totalIOwe);
        stats.put("netDebtPosition", netDebtPosition);
        stats.put("totalInvested", totalInvested);
        stats.put("totalProfitLoss", totalProfitLoss);
        stats.put("profitLossPercentage", Math.round(profitLossPercentage * 10) / 10.0);

        model.addAttribute("stats", stats);
        model.addAttribute("recentTransactions",
                transactionService.getRecentTransactions(userId, 10));
        model.addAttribute("activeGoals", goalService.getActiveGoals(userId));

        // Notifications – we'll pass them as a list to be shown as toasts via JavaScript
        List<String> notifications = user.getNotifications();
        model.addAttribute("notifications", notifications);

        // Chart data – expense and income by category
        var expensesByCategory = transactionService.getExpenseByCategory(userId, startOfMonth, endOfMonth);
        var incomeByCategory = transactionService.getIncomeByCategory(userId, startOfMonth, endOfMonth);

        model.addAttribute("expenseCategories", expensesByCategory.keySet());
        model.addAttribute("expenseAmounts", expensesByCategory.values());
        model.addAttribute("incomeCategories", incomeByCategory.keySet());
        model.addAttribute("incomeAmounts", incomeByCategory.values());

        // Weekly data for the selected month
        List<Double> weeklyIncome = new ArrayList<>();
        List<Double> weeklyExpense = new ArrayList<>();
        List<String> weekLabels = new ArrayList<>();
        
        // Calculate weeks for the selected month
        int daysInMonth = selectedPeriod.lengthOfMonth();
        int weekCount = (int) Math.ceil((double) daysInMonth / 7);
        
        for (int i = 0; i < weekCount; i++) {
            LocalDateTime weekStart = startOfMonth.plus(i * 7L, ChronoUnit.DAYS);
            LocalDateTime weekEnd = weekStart.plus(7, ChronoUnit.DAYS);
            if (weekEnd.isAfter(endOfMonth)) {
                weekEnd = endOfMonth;
            }
            
            weeklyIncome.add(transactionService.getTotalIncome(userId, weekStart, weekEnd));
            weeklyExpense.add(transactionService.getTotalExpense(userId, weekStart, weekEnd));
            weekLabels.add("Week " + (i + 1));
        }
        
        model.addAttribute("weeklyIncome", weeklyIncome);
        model.addAttribute("weeklyExpense", weeklyExpense);
        model.addAttribute("weekLabels", weekLabels);
        
        // Add filter parameters to model for UI
        model.addAttribute("selectedMonth", selectedPeriod.getMonthValue());
        model.addAttribute("selectedYear", selectedPeriod.getYear());
        model.addAttribute("selectedMonthName", selectedPeriod.getMonth().toString());

        // Clear notifications after fetching
        userService.clearNotifications(userId);

        // Layout attributes
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("pageSubtitle", "Here's your financial overview");
        model.addAttribute("title", "Dashboard");

        return "dashboard";
    }

    /**
     * Calculate balance from all previous months up to the month before selected period
     */
    private double getPreviousMonthBalance(String userId, YearMonth selectedPeriod) {
        // If this is January 2024 or earlier, no previous months to calculate
        if (selectedPeriod.getYear() <= 2024 && selectedPeriod.getMonthValue() == 1) {
            return 0.0;
        }
        
        YearMonth previousMonth = selectedPeriod.minusMonths(1);
        double cumulativeBalance = 0.0;
        
        // Calculate from January 2024 up to the month before selected period
        YearMonth current = YearMonth.of(2024, 1); // Start from January 2024
        YearMonth endPeriod = previousMonth;
        
        while (!current.isAfter(endPeriod)) {
            LocalDateTime monthStart = current.atDay(1).atStartOfDay();
            LocalDateTime monthEnd = current.atEndOfMonth().atTime(23, 59, 59);
            
            double monthIncome = transactionService.getTotalIncome(userId, monthStart, monthEnd);
            double monthExpense = transactionService.getTotalExpense(userId, monthStart, monthEnd);
            double monthBalance = monthIncome - monthExpense;
            
            cumulativeBalance += monthBalance;
            
            System.out.println("Previous month balance calculation: " + current + 
                    " - Income: " + monthIncome + ", Expense: " + monthExpense + 
                    ", Month Balance: " + monthBalance + ", Cumulative: " + cumulativeBalance);
            
            current = current.plusMonths(1);
        }
        
        return cumulativeBalance;
    }
}