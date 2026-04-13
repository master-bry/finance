package com.master.finance.controller;

import com.master.finance.model.Budget;
import com.master.finance.service.BudgetService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/budget")
public class BudgetController {
    
    @Autowired
    private BudgetService budgetService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String viewBudget(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        Budget budget = budgetService.getCurrentMonthBudget(userId);
        
        // Initialize default categories if budget is new
        if (budget == null) {
            budget = new Budget();
            budget.setMonth(YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        
        // Ensure categoryBudgets is not null
        if (budget.getCategoryBudgets() == null || budget.getCategoryBudgets().isEmpty()) {
            // Initialize default categories
            String[] defaultCategories = {"Food", "Transport", "Rent", "Utilities", "Entertainment", "Shopping", "Healthcare", "Other"};
            for (String cat : defaultCategories) {
                Budget.CategoryBudget cb = new Budget.CategoryBudget();
                cb.setPlanned(0.0);
                cb.setActual(0.0);
                budget.getCategoryBudgets().put(cat, cb);
            }
        }
        
        model.addAttribute("budget", budget);
        model.addAttribute("comparison", budgetService.getBudgetVsActual(userId, budget.getMonth()));
        
        // Layout attributes
        model.addAttribute("currentPage", "budget");
        model.addAttribute("pageSubtitle", "Plan and track your spending");
        model.addAttribute("title", "Budget");
        
        // Current month display
        String currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        model.addAttribute("currentMonth", currentMonth);
        
        return "budget/index";
    }
    
    @GetMapping("/edit")
    public String showEditForm(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        Budget budget = budgetService.getCurrentMonthBudget(userId);
        
        if (budget == null) {
            budget = new Budget();
            budget.setMonth(YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        
        // Initialize default categories if empty
        if (budget.getCategoryBudgets() == null || budget.getCategoryBudgets().isEmpty()) {
            String[] defaultCategories = {"Food", "Transport", "Rent", "Utilities", "Entertainment", "Shopping", "Healthcare", "Other"};
            for (String cat : defaultCategories) {
                Budget.CategoryBudget cb = new Budget.CategoryBudget();
                cb.setPlanned(0.0);
                cb.setActual(0.0);
                budget.getCategoryBudgets().put(cat, cb);
            }
        }
        
        model.addAttribute("budget", budget);
        
        // Layout attributes
        model.addAttribute("currentPage", "budget");
        model.addAttribute("pageSubtitle", "Set your monthly budget");
        model.addAttribute("title", "Edit Budget");
        
        return "budget/edit";
    }
    
    @PostMapping("/edit")
    public String saveBudget(@ModelAttribute Budget budget,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            budget.setUserId(userId);
            budget.setMonth(YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            budget.setUpdatedAt(java.time.LocalDateTime.now());
            
            budgetService.saveBudget(budget, userId);
            redirectAttributes.addFlashAttribute("success", "Budget saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving budget: " + e.getMessage());
        }
        return "redirect:/budget";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}