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
        model.addAttribute("budget", budget);
        model.addAttribute("comparison", budgetService.getBudgetVsActual(userId, budget.getMonth()));
        return "budget/index";
    }
    
    @GetMapping("/edit")
    public String showEditForm(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        Budget budget = budgetService.getCurrentMonthBudget(userId);
        model.addAttribute("budget", budget);
        return "budget/edit";
    }
    
    @PostMapping("/edit")
    public String saveBudget(@ModelAttribute Budget budget,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        budgetService.saveBudget(budget, userId);
        redirectAttributes.addFlashAttribute("success", "Budget saved successfully!");
        return "redirect:/budget";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}