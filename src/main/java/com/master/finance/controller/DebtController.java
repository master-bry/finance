package com.master.finance.controller;

import com.master.finance.model.Debt;
import com.master.finance.service.DebtService;
import com.master.finance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/debts")
public class DebtController {
    
    @Autowired
    private DebtService debtService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listDebts(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        model.addAttribute("debts", debtService.getUserDebts(userId));
        model.addAttribute("totalOwedToMe", debtService.getTotalOwedToMe(userId));
        model.addAttribute("totalIOwe", debtService.getTotalIOwe(userId));
        model.addAttribute("netPosition", debtService.getTotalOwedToMe(userId) - debtService.getTotalIOwe(userId));
        return "debts/index";
    }
    
    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("debt")) {
            model.addAttribute("debt", new Debt());
        }
        return "debts/add";
    }
    
    @PostMapping("/add")
    public String addDebt(@Valid @ModelAttribute Debt debt,
                          BindingResult result,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.debt", result);
            redirectAttributes.addFlashAttribute("debt", debt);
            return "redirect:/debts/add";
        }
        
        String userId = getUserId(authentication);
        debt.setUserId(userId);
        debtService.saveDebt(debt);
        redirectAttributes.addFlashAttribute("success", "Debt added successfully!");
        return "redirect:/debts";
    }
    
    @GetMapping("/make-payment/{id}")
    public String showPaymentForm(@PathVariable String id, Model model) {
        debtService.getDebt(id).ifPresent(debt -> model.addAttribute("debt", debt));
        return "debts/payment";
    }
    
    @PostMapping("/make-payment/{id}")
    public String makePayment(@PathVariable String id,
                              @RequestParam Double amount,
                              @RequestParam String notes,
                              RedirectAttributes redirectAttributes) {
        debtService.makePayment(id, amount, notes);
        redirectAttributes.addFlashAttribute("success", "Payment recorded successfully!");
        return "redirect:/debts";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteDebt(@PathVariable String id, RedirectAttributes redirectAttributes) {
        debtService.deleteDebt(id);
        redirectAttributes.addFlashAttribute("success", "Debt deleted successfully!");
        return "redirect:/debts";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}