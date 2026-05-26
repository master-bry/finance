package com.master.finance.controller;

import com.master.finance.model.RecurringTransaction;
import com.master.finance.service.RecurringTransactionService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recurring")
public class RecurringTransactionController {

    @Autowired
    private RecurringTransactionService recurringService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listRecurring(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        model.addAttribute("recurringList", recurringService.getUserRecurringTransactions(userId));
        model.addAttribute("currentPage", "recurring");
        model.addAttribute("pageSubtitle", "Manage recurring transactions");
        return "recurring/index";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("recurring")) {
            model.addAttribute("recurring", new RecurringTransaction());
        }
        model.addAttribute("currentPage", "recurring");
        return "recurring/add";
    }

    @PostMapping("/add")
    public String addRecurring(@ModelAttribute RecurringTransaction recurring,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (recurring.getDescription() == null || recurring.getDescription().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Description is required.");
            return "redirect:/recurring/add";
        }
        if (recurring.getAmount() == null || recurring.getAmount() <= 0) {
            redirectAttributes.addFlashAttribute("error", "A positive amount is required.");
            return "redirect:/recurring/add";
        }
        String userId = getUserId(authentication);
        recurring.setUserId(userId);
        recurringService.create(recurring);
        redirectAttributes.addFlashAttribute("success", "Recurring transaction created!");
        return "redirect:/recurring";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        boolean found = recurringService.getById(id)
                .filter(r -> userId.equals(r.getUserId()))
                .map(r -> { model.addAttribute("recurring", r); return true; })
                .orElse(false);
        if (!found) {
            redirectAttributes.addFlashAttribute("error", "Recurring transaction not found.");
            return "redirect:/recurring";
        }
        model.addAttribute("currentPage", "recurring");
        return "recurring/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateRecurring(@PathVariable String id,
                                  @ModelAttribute RecurringTransaction recurring,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        boolean found = recurringService.getById(id)
                .filter(r -> userId.equals(r.getUserId()))
                .isPresent();
        if (!found) {
            redirectAttributes.addFlashAttribute("error", "Recurring transaction not found.");
            return "redirect:/recurring";
        }
        recurringService.update(id, recurring);
        redirectAttributes.addFlashAttribute("success", "Recurring transaction updated!");
        return "redirect:/recurring";
    }

    @GetMapping("/delete/{id}")
    public String deleteRecurring(@PathVariable String id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        recurringService.getById(id)
                .filter(r -> userId.equals(r.getUserId()))
                .ifPresent(r -> recurringService.delete(r.getId()));
        redirectAttributes.addFlashAttribute("success", "Recurring transaction deleted!");
        return "redirect:/recurring";
    }

    @GetMapping("/toggle/{id}")
    public String toggleRecurring(@PathVariable String id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        recurringService.getById(id)
                .filter(r -> userId.equals(r.getUserId()))
                .ifPresent(r -> recurringService.toggleActive(r.getId()));
        redirectAttributes.addFlashAttribute("success", "Recurring transaction toggled!");
        return "redirect:/recurring";
    }

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
