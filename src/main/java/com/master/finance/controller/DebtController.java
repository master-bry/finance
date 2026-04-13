package com.master.finance.controller;

import com.master.finance.model.Debt;
import com.master.finance.service.DebtService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("/debts")
public class DebtController {

    @Autowired
    private DebtService debtService;

    @Autowired
    private UserService userService;

    // ─── LIST WITH PAGINATION ────────────────────────────────────────────────

    @GetMapping
    public String listDebts(Authentication authentication,
                            Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String type,
                            @RequestParam(required = false) String status) {
        String userId = getUserId(authentication);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateGiven").descending());
        Page<Debt> debtPage;
        
        if (type != null && !type.isEmpty()) {
            debtPage = debtService.getUserDebtsByType(userId, type, pageable);
        } else if (status != null && !status.isEmpty()) {
            debtPage = debtService.getUserDebtsByStatus(userId, status, pageable);
        } else {
            debtPage = debtService.getUserDebtsPaged(userId, pageable);
        }
        
        model.addAttribute("debts", debtPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", debtPage.getTotalPages());
        model.addAttribute("totalItems", debtPage.getTotalElements());
        model.addAttribute("pageSize", size);
        
        // Summary statistics
        model.addAttribute("totalOwedToMe", debtService.getTotalOwedToMe(userId));
        model.addAttribute("totalIOwe", debtService.getTotalIOwe(userId));
        model.addAttribute("netPosition", debtService.getNetPosition(userId));
        
        // Filter values
        model.addAttribute("filterType", type);
        model.addAttribute("filterStatus", status);
        
        // Layout attributes
        model.addAttribute("currentPageMenu", "debts");
        model.addAttribute("pageSubtitle", "Manage your debts and lending");
        model.addAttribute("title", "Debts");
        
        return "debts/index";
    }

    // ─── ADD ─────────────────────────────────────────────────────────────────

    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("debt")) {
            model.addAttribute("debt", new Debt());
        }
        model.addAttribute("currentPageMenu", "debts");
        model.addAttribute("pageSubtitle", "Add a new debt record");
        return "debts/add";
    }

    @PostMapping("/add")
    public String addDebt(@ModelAttribute Debt debt,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        // Manual validation
        if (debt.getPersonName() == null || debt.getPersonName().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Person name is required.");
            redirectAttributes.addFlashAttribute("debt", debt);
            return "redirect:/debts/add";
        }
        if (debt.getType() == null || debt.getType().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Debt type is required.");
            redirectAttributes.addFlashAttribute("debt", debt);
            return "redirect:/debts/add";
        }
        if (debt.getAmount() == null || debt.getAmount() <= 0) {
            redirectAttributes.addFlashAttribute("error", "A positive amount is required.");
            redirectAttributes.addFlashAttribute("debt", debt);
            return "redirect:/debts/add";
        }

        String userId = getUserId(authentication);
        debt.setUserId(userId);
        debt.setRemainingAmount(debt.getAmount()); // initial remaining = amount
        debt.setStatus("PENDING");
        debt.setDateGiven(LocalDateTime.now());
        debt.setLastUpdated(LocalDateTime.now());
        debtService.saveDebt(debt);
        redirectAttributes.addFlashAttribute("success", "Debt added successfully!");
        return "redirect:/debts";
    }

    // ─── EDIT ────────────────────────────────────────────────────────────────

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> debtOpt = debtService.getDebt(id);
        
        if (debtOpt.isEmpty() || !userId.equals(debtOpt.get().getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Debt not found or access denied.");
            return "redirect:/debts";
        }
        
        model.addAttribute("debt", debtOpt.get());
        model.addAttribute("currentPageMenu", "debts");
        model.addAttribute("pageSubtitle", "Edit debt record");
        return "debts/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateDebt(@PathVariable String id,
                             @ModelAttribute Debt debt,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> existingOpt = debtService.getDebt(id);
        
        if (existingOpt.isEmpty() || !userId.equals(existingOpt.get().getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Debt not found or access denied.");
            return "redirect:/debts";
        }
        
        Debt existing = existingOpt.get();
        // Update fields (preserve payment history and remaining amount logic)
        existing.setPersonName(debt.getPersonName());
        existing.setType(debt.getType());
        existing.setAmount(debt.getAmount());
        existing.setDescription(debt.getDescription());
        existing.setDueDate(debt.getDueDate());
        existing.setPhoneNumber(debt.getPhoneNumber());
        existing.setNotes(debt.getNotes());
        existing.setStatus(debt.getStatus());
        existing.setLastUpdated(LocalDateTime.now());
        
        // If amount changed, adjust remaining amount proportionally? 
        // For simplicity, keep existing remaining unless manually changed.
        
        debtService.saveDebt(existing);
        redirectAttributes.addFlashAttribute("success", "Debt updated successfully!");
        return "redirect:/debts";
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @GetMapping("/delete/{id}")
    public String deleteDebt(@PathVariable String id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        boolean deleted = debtService.getDebt(id)
                .filter(debt -> userId.equals(debt.getUserId()))
                .map(debt -> {
                    debtService.deleteDebt(debt.getId());
                    return true;
                })
                .orElse(false);

        if (deleted) {
            redirectAttributes.addFlashAttribute("success", "Debt deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Debt not found or access denied.");
        }
        return "redirect:/debts";
    }

    // ─── PAYMENT ─────────────────────────────────────────────────────────────

    @GetMapping("/make-payment/{id}")
    public String showPaymentForm(@PathVariable String id,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> debtOpt = debtService.getDebt(id);
        
        if (debtOpt.isEmpty() || !userId.equals(debtOpt.get().getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Debt not found.");
            return "redirect:/debts";
        }
        
        model.addAttribute("debt", debtOpt.get());
        model.addAttribute("currentPageMenu", "debts");
        model.addAttribute("pageSubtitle", "Record a payment");
        return "debts/payment";
    }

    @PostMapping("/make-payment/{id}")
    public String makePayment(@PathVariable String id,
                              @RequestParam Double amount,
                              @RequestParam(required = false, defaultValue = "") String notes,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> debtOpt = debtService.getDebt(id);
        
        if (debtOpt.isEmpty() || !userId.equals(debtOpt.get().getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Payment failed. Debt not found.");
            return "redirect:/debts";
        }
        
        Debt debt = debtOpt.get();
        if (amount <= 0 || amount > debt.getRemainingAmount()) {
            redirectAttributes.addFlashAttribute("error", "Invalid payment amount.");
            return "redirect:/debts/make-payment/" + id;
        }
        
        // Record payment
        Debt.PaymentRecord payment = new Debt.PaymentRecord();
        payment.setAmountPaid(amount);
        payment.setNotes(notes);
        payment.setPaymentDate(LocalDateTime.now());
        debt.getPaymentHistory().add(payment);
        
        // Update remaining amount
        double newRemaining = debt.getRemainingAmount() - amount;
        debt.setRemainingAmount(newRemaining);
        
        // Update status
        if (newRemaining <= 0) {
            debt.setStatus("SETTLED");
        } else {
            debt.setStatus("PARTIAL");
        }
        debt.setLastUpdated(LocalDateTime.now());
        
        debtService.saveDebt(debt);
        redirectAttributes.addFlashAttribute("success", "Payment recorded successfully!");
        return "redirect:/debts";
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getId();
    }
}