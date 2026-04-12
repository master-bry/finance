package com.master.finance.controller;

import com.master.finance.model.Transaction;
import com.master.finance.repository.TransactionRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/transactions")
public class TransactionController {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listTransactions(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        List<Transaction> transactions = transactionRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
        
        double totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        double totalExpense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("balance", totalIncome - totalExpense);
        model.addAttribute("currentPage", "transactions");
        model.addAttribute("title", "Transactions");
        model.addAttribute("pageSubtitle", "Manage your income and expenses");
        
        return "transactions/index";
    }
    
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("currentPage", "transactions");
        model.addAttribute("title", "Add Transaction");
        return "transactions/add";
    }
    
    @PostMapping("/add")
    public String addTransaction(@Valid @ModelAttribute Transaction transaction,
                                 BindingResult result,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fix the errors");
            return "redirect:/transactions/add";
        }
        
        String userId = getUserId(authentication);
        transaction.setUserId(userId);
        transaction.setDate(LocalDateTime.now());
        transaction.setDeleted(false);
        transactionRepository.save(transaction);
        
        redirectAttributes.addFlashAttribute("success", "Transaction added successfully!");
        return "redirect:/transactions";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        transactionRepository.findById(id).ifPresent(transaction -> {
            if (transaction.getUserId().equals(userId)) {
                model.addAttribute("transaction", transaction);
            }
        });
        model.addAttribute("currentPage", "transactions");
        model.addAttribute("title", "Edit Transaction");
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
        transactionRepository.save(transaction);
        redirectAttributes.addFlashAttribute("success", "Transaction updated!");
        return "redirect:/transactions";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteTransaction(@PathVariable String id, RedirectAttributes redirectAttributes) {
        transactionRepository.findById(id).ifPresent(transaction -> {
            transaction.setDeleted(true);
            transaction.setDeletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        });
        redirectAttributes.addFlashAttribute("success", "Transaction deleted!");
        return "redirect:/transactions";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}