package com.master.finance.controller;

import com.master.finance.model.Debt;
import com.master.finance.repository.DebtRepository;
import com.master.finance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/debts")
public class DebtController {
    
    @Autowired
    private DebtRepository debtRepository;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listDebts(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        List<Debt> debts = debtRepository.findByUserIdAndDeletedFalse(userId);
        
        double totalOwedToMe = debts.stream()
                .filter(d -> "OWED_TO_ME".equals(d.getType()) && !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
        
        double totalIOwe = debts.stream()
                .filter(d -> "I_OWE".equals(d.getType()) && !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
        
        model.addAttribute("debts", debts);
        model.addAttribute("totalOwedToMe", totalOwedToMe);
        model.addAttribute("totalIOwe", totalIOwe);
        model.addAttribute("netPosition", totalOwedToMe - totalIOwe);
        model.addAttribute("currentPage", "debts");
        model.addAttribute("title", "Debts");
        model.addAttribute("pageSubtitle", "Track who owes you and who you owe");
        
        return "debts/index";
    }
    
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("debt", new Debt());
        model.addAttribute("currentPage", "debts");
        model.addAttribute("title", "Add Debt");
        return "debts/add";
    }
    
    @PostMapping("/add")
    public String addDebt(@Valid @ModelAttribute Debt debt,
                          BindingResult result,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fix the errors");
            return "redirect:/debts/add";
        }
        
        String userId = getUserId(authentication);
        debt.setUserId(userId);
        debt.setRemainingAmount(debt.getAmount());
        debt.setStatus("PENDING");
        debt.setDateGiven(LocalDateTime.now());
        debt.setLastUpdated(LocalDateTime.now());
        debt.setDeleted(false);
        debtRepository.save(debt);
        
        redirectAttributes.addFlashAttribute("success", "Debt added successfully!");
        return "redirect:/debts";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        debtRepository.findById(id).ifPresent(debt -> {
            if (debt.getUserId().equals(userId)) {
                model.addAttribute("debt", debt);
            }
        });
        model.addAttribute("currentPage", "debts");
        model.addAttribute("title", "Edit Debt");
        return "debts/edit";
    }
    
    @PostMapping("/edit/{id}")
    public String updateDebt(@PathVariable String id,
                             @Valid @ModelAttribute Debt debt,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Debt existingDebt = debtRepository.findById(id).orElse(null);
        if (existingDebt != null) {
            debt.setPaymentHistory(existingDebt.getPaymentHistory());
        }
        debt.setId(id);
        debt.setUserId(userId);
        debt.setLastUpdated(LocalDateTime.now());
        debtRepository.save(debt);
        redirectAttributes.addFlashAttribute("success", "Debt updated!");
        return "redirect:/debts";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteDebt(@PathVariable String id, RedirectAttributes redirectAttributes) {
        debtRepository.findById(id).ifPresent(debt -> {
            debt.setDeleted(true);
            debt.setDeletedAt(LocalDateTime.now());
            debtRepository.save(debt);
        });
        redirectAttributes.addFlashAttribute("success", "Debt deleted!");
        return "redirect:/debts";
    }
    
    @GetMapping("/make-payment/{id}")
    public String showPaymentForm(@PathVariable String id, Model model) {
        debtRepository.findById(id).ifPresent(debt -> model.addAttribute("debt", debt));
        model.addAttribute("currentPage", "debts");
        return "debts/payment";
    }
    
    @PostMapping("/make-payment/{id}")
    public String makePayment(@PathVariable String id,
                              @RequestParam Double amount,
                              @RequestParam String notes,
                              RedirectAttributes redirectAttributes) {
        debtRepository.findById(id).ifPresent(debt -> {
            Debt.PaymentRecord payment = new Debt.PaymentRecord();
            payment.setAmountPaid(amount);
            payment.setNotes(notes);
            debt.getPaymentHistory().add(payment);
            
            double newRemaining = debt.getRemainingAmount() - amount;
            debt.setRemainingAmount(newRemaining);
            
            if (newRemaining <= 0) {
                debt.setStatus("SETTLED");
                debt.setRemainingAmount(0.0);
            } else if (newRemaining < debt.getAmount()) {
                debt.setStatus("PARTIAL");
            }
            debt.setLastUpdated(LocalDateTime.now());
            debtRepository.save(debt);
        });
        redirectAttributes.addFlashAttribute("success", "Payment recorded!");
        return "redirect:/debts";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}