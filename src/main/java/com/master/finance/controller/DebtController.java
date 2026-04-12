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
import java.util.List;
import java.util.Optional;

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
        List<Debt> debts = debtService.getUserDebts(userId);
        
        // Calculate totals using service methods
        double totalOwedToMe = debtService.getTotalOwedToMe(userId);
        double totalIOwe = debtService.getTotalIOwe(userId);
        
        model.addAttribute("debts", debts);
        model.addAttribute("totalOwedToMe", totalOwedToMe);
        model.addAttribute("totalIOwe", totalIOwe);
        model.addAttribute("netPosition", totalOwedToMe - totalIOwe);
        
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
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if (result.hasErrors()) {
            // Pass errors to view for display
            model.addAttribute("debt", debt);
            model.addAttribute("org.springframework.validation.BindingResult.debt", result);
            return "debts/add";
        }
        
        String userId = getUserId(authentication);
        debt.setUserId(userId);
        debt.setRemainingAmount(debt.getAmount());
        debt.setStatus("PENDING");
        
        debtService.saveDebt(debt);
        redirectAttributes.addFlashAttribute("success", "✓ Debt added successfully!");
        return "redirect:/debts";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        Optional<Debt> debtOpt = debtService.getDebt(id);
        
        if (debtOpt.isEmpty() || !debtOpt.get().getUserId().equals(userId)) {
            return "redirect:/debts?error=not-found";
        }
        
        model.addAttribute("debt", debtOpt.get());
        return "debts/edit";
    }
    
    @PostMapping("/edit/{id}")
    public String updateDebt(@PathVariable String id,
                             @Valid @ModelAttribute Debt debt,
                             BindingResult result,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        String userId = getUserId(authentication);
        
        // Verify authorization
        Optional<Debt> existingDebtOpt = debtService.getDebt(id);
        if (existingDebtOpt.isEmpty() || !existingDebtOpt.get().getUserId().equals(userId)) {
            return "redirect:/debts?error=unauthorized";
        }
        
        if (result.hasErrors()) {
            model.addAttribute("debt", debt);
            model.addAttribute("org.springframework.validation.BindingResult.debt", result);
            return "debts/edit";
        }
        
        Debt existingDebt = existingDebtOpt.get();
        
        // Preserve payment history and update fields
        debt.setId(id);
        debt.setUserId(userId);
        debt.setPaymentHistory(existingDebt.getPaymentHistory());
        debt.setRemainingAmount(existingDebt.getRemainingAmount());
        debt.setDateGiven(existingDebt.getDateGiven());
        
        debtService.saveDebt(debt);
        redirectAttributes.addFlashAttribute("success", "✓ Debt updated successfully!");
        return "redirect:/debts";
    }
    
    @PostMapping("/delete/{id}")
    public String deleteDebt(@PathVariable String id, 
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        
        // Verify authorization
        Optional<Debt> debtOpt = debtService.getDebt(id);
        if (debtOpt.isEmpty() || !debtOpt.get().getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "✗ You don't have permission to delete this debt");
            return "redirect:/debts";
        }
        
        debtService.deleteDebt(id);
        redirectAttributes.addFlashAttribute("success", "✓ Debt deleted successfully!");
        return "redirect:/debts";
    }
    
    @GetMapping("/make-payment/{id}")
    public String showPaymentForm(@PathVariable String id, 
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> debtOpt = debtService.getDebt(id);
        
        if (debtOpt.isEmpty() || !debtOpt.get().getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "✗ Debt not found");
            return "redirect:/debts";
        }
        
        model.addAttribute("debt", debtOpt.get());
        return "debts/payment";
    }
    
    @PostMapping("/make-payment/{id}")
    public String makePayment(@PathVariable String id,
                              @RequestParam Double amount,
                              @RequestParam(required = false, defaultValue = "") String notes,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        
        // Verify authorization
        Optional<Debt> debtOpt = debtService.getDebt(id);
        if (debtOpt.isEmpty() || !debtOpt.get().getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "✗ You don't have permission to make payments on this debt");
            return "redirect:/debts";
        }
        
        Debt debt = debtOpt.get();
        
        // Validate payment amount
        if (amount == null || amount <= 0) {
            redirectAttributes.addFlashAttribute("error", "✗ Payment amount must be greater than 0");
            return "redirect:/debts/make-payment/" + id;
        }
        
        if (amount > debt.getRemainingAmount()) {
            redirectAttributes.addFlashAttribute("error", "✗ Payment amount cannot exceed remaining amount of " + debt.getRemainingAmount());
            return "redirect:/debts/make-payment/" + id;
        }
        
        if ("SETTLED".equals(debt.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "✗ This debt is already settled");
            return "redirect:/debts";
        }
        
        try {
            debtService.makePayment(id, amount, notes);
            redirectAttributes.addFlashAttribute("success", "✓ Payment of " + amount + " recorded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "✗ Error recording payment: " + e.getMessage());
        }
        
        return "redirect:/debts";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}