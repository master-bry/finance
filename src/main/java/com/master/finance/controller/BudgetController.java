package com.master.finance.controller;

import com.master.finance.model.Budget;
import com.master.finance.repository.BudgetRepository;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/budget")
public class BudgetController {
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String viewBudget(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, currentMonth)
                .orElseGet(() -> createDefaultBudget(userId, currentMonth));
        
        model.addAttribute("budget", budget);
        model.addAttribute("currentMonth", currentMonth);
        
        return "budget/index";
    }
    
    @GetMapping("/edit")
    public String showEditForm(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        Budget budget = budgetRepository.findByUserIdAndMonth(userId, currentMonth)
                .orElseGet(() -> createDefaultBudget(userId, currentMonth));
        
        model.addAttribute("budget", budget);
        return "budget/edit";
    }
    
    @PostMapping("/edit")
    public String saveBudget(@RequestParam Double totalIncome,
                             @RequestParam Double savingsTarget,
                             @RequestParam Map<String, String> allParams,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            Budget budget = budgetRepository.findByUserIdAndMonth(userId, currentMonth)
                    .orElseGet(() -> createDefaultBudget(userId, currentMonth));
            
            budget.setTotalIncome(totalIncome);
            budget.setSavingsTarget(savingsTarget);
            
            // Update category budgets
            Map<String, Budget.CategoryBudget> categoryBudgets = new LinkedHashMap<>();
            String[] categories = {"Food", "Transport", "Rent", "Utilities", "Entertainment", "Shopping", "Healthcare", "Education", "Savings", "Other"};
            
            for (String category : categories) {
                String plannedKey = "planned_" + category;
                if (allParams.containsKey(plannedKey)) {
                    Budget.CategoryBudget catBudget = new Budget.CategoryBudget();
                    catBudget.setPlanned(Double.parseDouble(allParams.get(plannedKey)));
                    catBudget.setActual(0.0);
                    categoryBudgets.put(category, catBudget);
                }
            }
            
            budget.setCategoryBudgets(categoryBudgets);
            budget.setUpdatedAt(LocalDateTime.now());
            budgetRepository.save(budget);
            
            redirectAttributes.addFlashAttribute("success", "Budget saved successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error saving budget: " + e.getMessage());
        }
        
        return "redirect:/budget";
    }
    
    private Budget createDefaultBudget(String userId, String month) {
        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setMonth(month);
        budget.setTotalIncome(0.0);
        budget.setTotalExpense(0.0);
        budget.setSavingsTarget(0.0);
        
        Map<String, Budget.CategoryBudget> defaults = new LinkedHashMap<>();
        String[] categories = {"Food", "Transport", "Rent", "Utilities", "Entertainment", "Shopping", "Healthcare", "Education", "Savings", "Other"};
        
        for (String category : categories) {
            Budget.CategoryBudget catBudget = new Budget.CategoryBudget();
            catBudget.setPlanned(0.0);
            catBudget.setActual(0.0);
            defaults.put(category, catBudget);
        }
        
        budget.setCategoryBudgets(defaults);
        return budgetRepository.save(budget);
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}