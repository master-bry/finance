package com.master.finance.controller;

import com.master.finance.model.Account;
import com.master.finance.service.AccountService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listAccounts(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        model.addAttribute("accounts", accountService.getUserAccounts(userId));
        model.addAttribute("totalBalance", accountService.getTotalBalance(userId));
        model.addAttribute("currentPage", "accounts");
        model.addAttribute("pageSubtitle", "Manage your bank accounts");
        return "accounts/index";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("account")) {
            model.addAttribute("account", new Account());
        }
        model.addAttribute("currentPage", "accounts");
        return "accounts/add";
    }

    @PostMapping("/add")
    public String addAccount(@ModelAttribute Account account,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (account.getName() == null || account.getName().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Account name is required.");
            return "redirect:/accounts/add";
        }
        String userId = getUserId(authentication);
        account.setUserId(userId);
        accountService.createAccount(account);
        redirectAttributes.addFlashAttribute("success", "Account added successfully!");
        return "redirect:/accounts";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        boolean found = accountService.getAccount(id)
                .filter(a -> userId.equals(a.getUserId()))
                .map(a -> { model.addAttribute("account", a); return true; })
                .orElse(false);
        if (!found) {
            redirectAttributes.addFlashAttribute("error", "Account not found.");
            return "redirect:/accounts";
        }
        model.addAttribute("currentPage", "accounts");
        return "accounts/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateAccount(@PathVariable String id,
                                @ModelAttribute Account account,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        boolean found = accountService.getAccount(id)
                .filter(a -> userId.equals(a.getUserId()))
                .isPresent();
        if (!found) {
            redirectAttributes.addFlashAttribute("error", "Account not found.");
            return "redirect:/accounts";
        }
        accountService.updateAccount(id, account);
        redirectAttributes.addFlashAttribute("success", "Account updated successfully!");
        return "redirect:/accounts";
    }

    @GetMapping("/delete/{id}")
    public String deleteAccount(@PathVariable String id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        accountService.getAccount(id)
                .filter(a -> userId.equals(a.getUserId()))
                .ifPresent(a -> accountService.deleteAccount(a.getId()));
        redirectAttributes.addFlashAttribute("success", "Account deleted successfully!");
        return "redirect:/accounts";
    }

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
