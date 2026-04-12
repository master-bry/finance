package com.master.finance.controller;

import com.master.finance.model.Debt;
import com.master.finance.service.DebtService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        
        double totalOwedToMe = debtService.getTotalOwedToMe(userId);
        double totalIOwe = debtService.getTotalIOwe(userId);
        
        model.addAttribute("debts", debts);
        model.addAttribute("totalOwedToMe", totalOwedToMe);
        model.addAttribute("totalIOwe", totalIOwe);
        model.addAttribute("netPosition", totalOwedToMe - totalIOwe);
        
        return "debts/index";
    }
    
    @GetMapping("/add")
    public String showAddForm() {
        return "debts/add";
    }
    
    @PostMapping("/add")
    public String addDebt(@RequestParam String personName,
                          @RequestParam String type,
                          @RequestParam Double amount,
                          @RequestParam(required = false) String phoneNumber,
                          @RequestParam(required = false) String dueDate,
                          @RequestParam(required = false) String description,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            
            // Create new debt
            Debt debt = new Debt();
            debt.setUserId(userId);
            debt.setPersonName(personName);
            debt.setType(type);
            debt.setAmount(amount);
            debt.setPhoneNumber(phoneNumber);
            debt.setDescription(description);
            
            // Set due date if provided
            if (dueDate != null && !dueDate.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime dueDateTime = LocalDateTime.parse(dueDate + "T00:00:00");
                debt.setDueDate(dueDateTime);
            }
            
            // Save debt
            debtService.saveDebt(debt);
            redirectAttributes.addFlashAttribute("success", "Debt added successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error saving debt: " + e.getMessage());
        }
        
        return "redirect:/debts";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteDebt(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            debtService.deleteDebt(id);
            redirectAttributes.addFlashAttribute("success", "Debt deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting debt");
        }
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
                              @RequestParam(required = false) String notes,
                              RedirectAttributes redirectAttributes) {
        try {
            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Please enter a valid amount");
                return "redirect:/debts/make-payment/" + id;
            }
            
            debtService.makePayment(id, amount, notes);
            redirectAttributes.addFlashAttribute("success", "Payment recorded successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error recording payment");
        }
        
        return "redirect:/debts";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}