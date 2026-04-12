package com.master.finance.controller;

import com.master.finance.model.*;
import com.master.finance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {
    
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
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        String username = authentication.getName();
        
        // Get user ID (you'll need to implement this based on your UserService)
        String userId = getUserId(username);
        
        // Get current month data
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime now = LocalDateTime.now();
        
        // Transactions
        List<Transaction> transactions = transactionRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
        List<Transaction> monthlyTransactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, startOfMonth, now);
        
        double totalIncome = monthlyTransactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        double totalExpense = monthlyTransactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        double balance = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (balance / totalIncome) * 100 : 0;
        
        // Expenses by category for chart
        Map<String, Double> expensesByCategory = monthlyTransactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .collect(Collectors.groupingBy(Transaction::getCategory, 
                         Collectors.summingDouble(Transaction::getAmount)));
        
        // Debts
        List<Debt> debts = debtRepository.findByUserIdAndDeletedFalse(userId);
        double totalOwedToMe = debts.stream()
                .filter(d -> "OWED_TO_ME".equals(d.getType()) && !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
        
        double totalIOwe = debts.stream()
                .filter(d -> "I_OWE".equals(d.getType()) && !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
        
        // Investments
        List<Investment> investments = investmentRepository.findByUserIdAndDeletedFalse(userId);
        double totalInvested = investments.stream().mapToDouble(Investment::getAmountInvested).sum();
        double totalCurrentValue = investments.stream().mapToDouble(Investment::getCurrentValue).sum();
        double totalProfitLoss = totalCurrentValue - totalInvested;
        
        // Goals
        List<Goal> goals = goalRepository.findByUserIdAndDeletedFalse(userId);
        List<Goal> activeGoals = goals.stream().filter(g -> !g.isAchieved()).toList();
        
        // Budget
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Budget currentBudget = budgetRepository.findByUserIdAndMonth(userId, currentMonth).orElse(null);
        
        // Recent transactions (last 5)
        List<Transaction> recentTransactions = transactions.stream().limit(5).toList();
        
        // Prepare data for charts
        model.addAttribute("expensesByCategory", expensesByCategory);
        model.addAttribute("expenseCategories", expensesByCategory.keySet());
        model.addAttribute("expenseAmounts", expensesByCategory.values());
        
        // Dashboard stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncome", totalIncome);
        stats.put("totalExpense", totalExpense);
        stats.put("balance", balance);
        stats.put("savingsRate", Math.round(savingsRate));
        stats.put("totalOwedToMe", totalOwedToMe);
        stats.put("totalIOwe", totalIOwe);
        stats.put("totalInvested", totalInvested);
        stats.put("totalCurrentValue", totalCurrentValue);
        stats.put("totalProfitLoss", totalProfitLoss);
        stats.put("activeGoalsCount", activeGoals.size());
        stats.put("totalTransactions", transactions.size());
        
        model.addAttribute("stats", stats);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("activeGoals", activeGoals.stream().limit(3).toList());
        model.addAttribute("currentBudget", currentBudget);
        model.addAttribute("username", username);
        
        return "dashboard";
    }
    
    private String getUserId(String username) {
        // Implement this based on your UserService
        // For now, return a placeholder - you'll need to inject UserService
        return username;
    }
}