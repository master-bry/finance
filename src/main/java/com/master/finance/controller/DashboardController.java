package com.master.finance.controller;

import com.master.finance.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

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
        String userId = userService.findByUsername(username).get().getId();
        
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime now = LocalDateTime.now();
        
        double totalIncome = transactionService.getTotalIncome(userId, startOfMonth, now);
        double totalExpense = transactionService.getTotalExpense(userId, startOfMonth, now);
        double balance = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (balance / totalIncome) * 100 : 0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncome", totalIncome);
        stats.put("totalExpense", totalExpense);
        stats.put("balance", balance);
        stats.put("savingsRate", Math.round(savingsRate));
        
        model.addAttribute("stats", stats);
        model.addAttribute("debtsOwedToMe", debtService.getDebtsOwedToMe(userId));
        model.addAttribute("debtsIOwe", debtService.getDebtsIOwe(userId));
        model.addAttribute("investments", investmentService.getUserInvestments(userId));
        model.addAttribute("goals", goalService.getActiveGoals(userId));
        model.addAttribute("recentTransactions", transactionService.getUserTransactions(userId).stream().limit(10).toList());
        model.addAttribute("alerts", userService.getNotifications(userId));
        
        userService.clearNotifications(userId);
        
        return "dashboard";
    }
}