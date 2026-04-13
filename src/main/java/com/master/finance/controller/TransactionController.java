package com.master.finance.controller;

import com.master.finance.model.Transaction;
import com.master.finance.service.TransactionService;
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
@RequestMapping("/transactions")
public class TransactionController {
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listTransactions(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        var transactions = transactionService.getUserTransactions(userId);
        
        double totalIncome = transactions.stream()
            .filter(t -> "INCOME".equals(t.getType()))
            .mapToDouble(Transaction::getAmount)
            .sum();
        double totalExpense = transactions.stream()
            .filter(t -> "EXPENSE".equals(t.getType()))
            .mapToDouble(Transaction::getAmount)
            .sum();
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalIncome", totalIncome);
        stats.put("totalExpense", totalExpense);
        stats.put("balance", totalIncome - totalExpense);
        stats.put("transactionCount", transactions.size());
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("stats", stats);
        model.addAttribute("currentPage", "transactions");
        model.addAttribute("pageSubtitle", "Manage all your income and expenses");
        return "transactions/index";
    }
    
    // @GetMapping("/add")
    // public String showAddForm(Model model) {
    //     if (!model.containsAttribute("transaction")) {
    //         model.addAttribute("transaction", new Transaction());
    //     }
    //     model.addAttribute("currentPage", "transactions");
    //     return "transactions/add";
    // }
    
    // @PostMapping("/add")
    // public String addTransaction(@Valid @ModelAttribute Transaction transaction,
    //                              BindingResult result,
    //                              Authentication authentication,
    //                              RedirectAttributes redirectAttributes) {
    //     if (result.hasErrors()) {
    //         redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.transaction", result);
    //         redirectAttributes.addFlashAttribute("transaction", transaction);
    //         return "redirect:/transactions/add";
    //     }
        
    //     String userId = getUserId(authentication);
    //     transaction.setUserId(userId);
    //     transactionService.saveTransaction(transaction);
    //     redirectAttributes.addFlashAttribute("success", "Transaction added successfully!");
    //     return "redirect:/transactions";
    // }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        transactionService.getTransaction(id).ifPresent(transaction -> {
            if (transaction.getUserId().equals(userId)) {
                model.addAttribute("transaction", transaction);
            }
        });
        return "transactions/edit";
    }
    
    @PostMapping("/edit/{id}")
    public String updateTransaction(@PathVariable String id,
                                    @Valid @ModelAttribute Transaction transaction,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        transaction.setId(id);
        transaction.setUserId(userId);
        transactionService.saveTransaction(transaction);
        redirectAttributes.addFlashAttribute("success", "Transaction updated successfully!");
        return "redirect:/transactions";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteTransaction(@PathVariable String id, RedirectAttributes redirectAttributes) {
        transactionService.deleteTransaction(id);  // Now this method exists
        redirectAttributes.addFlashAttribute("success", "Transaction deleted successfully!");
        return "redirect:/transactions";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}