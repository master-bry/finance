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

@Controller
@RequestMapping("/debts")
public class DebtController {

    @Autowired
    private DebtService debtService;

    @Autowired
    private UserService userService;

    // ─── LIST ────────────────────────────────────────────────────────────────

    @GetMapping
    public String listDebts(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        model.addAttribute("debts", debtService.getUserDebts(userId));
        model.addAttribute("totalOwedToMe", debtService.getTotalOwedToMe(userId));
        model.addAttribute("totalIOwe", debtService.getTotalIOwe(userId));
        model.addAttribute("netPosition", debtService.getNetPosition(userId));
        model.addAttribute("currentPage", "debts");
        model.addAttribute("pageSubtitle", "Manage your debts and lending");
        return "debts/index";
    }

    // ─── ADD ─────────────────────────────────────────────────────────────────

    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("debt")) {
            model.addAttribute("debt", new Debt());
        }
        model.addAttribute("currentPage", "debts");
        model.addAttribute("pageSubtitle", "Add a new debt record");
        return "debts/add";
    }

    @PostMapping("/add")
    public String addDebt(@ModelAttribute Debt debt,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        // Manual validation (Debt has no @NotBlank annotations)
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
        boolean found = debtService.getDebt(id)
                .filter(debt -> userId.equals(debt.getUserId()))
                .map(debt -> {
                    model.addAttribute("debt", debt);
                    return true;
                })
                .orElse(false);

        if (!found) {
            redirectAttributes.addFlashAttribute("error", "Debt not found or access denied.");
            return "redirect:/debts";
        }
        model.addAttribute("currentPage", "debts");
        model.addAttribute("pageSubtitle", "Edit debt record");
        return "debts/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateDebt(@PathVariable String id,
                             @ModelAttribute Debt debt,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        debt.setId(id);
        debt.setUserId(userId);
        debtService.updateDebt(debt);
        redirectAttributes.addFlashAttribute("success", "Debt updated successfully!");
        return "redirect:/debts";
    }

    // ─── SOFT DELETE ─────────────────────────────────────────────────────────

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
        boolean found = debtService.getDebt(id)
                .filter(debt -> userId.equals(debt.getUserId()))
                .map(debt -> {
                    model.addAttribute("debt", debt);
                    return true;
                })
                .orElse(false);

        if (!found) {
            redirectAttributes.addFlashAttribute("error", "Debt not found.");
            return "redirect:/debts";
        }
        model.addAttribute("currentPage", "debts");
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
        boolean ok = debtService.getDebt(id)
                .filter(debt -> userId.equals(debt.getUserId()))
                .map(debt -> {
                    debtService.makePayment(id, amount, notes);
                    return true;
                })
                .orElse(false);

        if (ok) {
            redirectAttributes.addFlashAttribute("success", "Payment recorded successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Payment failed. Debt not found.");
        }
        return "redirect:/debts";
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getId();
    }
}