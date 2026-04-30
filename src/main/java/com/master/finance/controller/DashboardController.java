package com.master.finance.controller;

import com.master.finance.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
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
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        var user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String userId = user.getId();

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime now = LocalDateTime.now();

        double totalIncome = transactionService.getTotalIncome(userId, startOfMonth, now);
        double totalExpense = transactionService.getTotalExpense(userId, startOfMonth, now);
        double balance = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (balance / totalIncome) * 100 : 0;

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
        stats.put("balance", balance);
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
        var expensesByCategory = transactionService.getExpenseByCategory(userId, startOfMonth, now);
        var incomeByCategory = transactionService.getIncomeByCategory(userId, startOfMonth, now);

        model.addAttribute("expenseCategories", expensesByCategory.keySet());
        model.addAttribute("expenseAmounts", expensesByCategory.values());
        model.addAttribute("incomeCategories", incomeByCategory.keySet());
        model.addAttribute("incomeAmounts", incomeByCategory.values());

        // Weekly data
        List<Double> weeklyIncome = new ArrayList<>();
        List<Double> weeklyExpense = new ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            LocalDateTime weekStart = now.minus(i * 7L, ChronoUnit.DAYS).withHour(0).withMinute(0);
            LocalDateTime weekEnd = weekStart.plus(7, ChronoUnit.DAYS);
            weeklyIncome.add(transactionService.getTotalIncome(userId, weekStart, weekEnd));
            weeklyExpense.add(transactionService.getTotalExpense(userId, weekStart, weekEnd));
        }
        model.addAttribute("weeklyIncome", weeklyIncome);
        model.addAttribute("weeklyExpense", weeklyExpense);

        // Clear notifications after fetching
        userService.clearNotifications(userId);

        // Layout attributes
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("pageSubtitle", "Here's your financial overview");
        model.addAttribute("title", "Dashboard");

        return "dashboard";
    }
}