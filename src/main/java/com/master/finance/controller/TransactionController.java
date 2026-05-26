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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
        model.addAttribute("title", "Transactions");
        return "transactions/index";
    }

    // ADD methods removed as requested

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        return transactionService.getTransaction(id)
                .filter(transaction -> transaction.getUserId().equals(userId))
                .map(transaction -> {
                    model.addAttribute("transaction", transaction);
                    model.addAttribute("currentPage", "transactions");
                    model.addAttribute("pageSubtitle", "Edit transaction");
                    model.addAttribute("title", "Edit Transaction");
                    return "transactions/edit";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Transaction not found or access denied.");
                    return "redirect:/transactions";
                });
    }

    @PostMapping("/edit/{id}")
    public String updateTransaction(@PathVariable String id,
                                    @Valid @ModelAttribute Transaction transaction,
                                    BindingResult result,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.transaction", result);
            redirectAttributes.addFlashAttribute("transaction", transaction);
            return "redirect:/transactions/edit/" + id;
        }

        String userId = getUserId(authentication);
        
        // Verify ownership
        var existingOpt = transactionService.getTransaction(id);
        if (existingOpt.isEmpty() || !existingOpt.get().getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "Transaction not found or access denied.");
            return "redirect:/transactions";
        }

        transaction.setId(id);
        transaction.setUserId(userId);
        
        // USE updateTransaction() NOT saveTransaction() - this triggers DailyEntry sync
        transactionService.updateTransaction(transaction);
        
        redirectAttributes.addFlashAttribute("success", "Transaction updated successfully!");
        return "redirect:/transactions";
    }

    @GetMapping("/delete/{id}")
    public String deleteTransaction(@PathVariable String id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        
        var transactionOpt = transactionService.getTransaction(id);
        if (transactionOpt.isEmpty() || !transactionOpt.get().getUserId().equals(userId)) {
            redirectAttributes.addFlashAttribute("error", "Transaction not found or access denied.");
            return "redirect:/transactions";
        }

        // This will soft delete AND sync removal to DailyEntry
        transactionService.deleteTransaction(id);
        redirectAttributes.addFlashAttribute("success", "Transaction deleted successfully!");
        return "redirect:/transactions";
    }

    @GetMapping("/import")
    public String showImportForm(Model model) {
        model.addAttribute("currentPage", "transactions");
        model.addAttribute("pageSubtitle", "Import transactions from CSV");
        return "transactions/import";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a CSV file.");
            return "redirect:/transactions/import";
        }
        String userId = getUserId(authentication);
        List<Transaction> imported = new ArrayList<>();
        int errors = 0;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] cols = line.split(",");
                if (cols.length < 4) { errors++; continue; }
                try {
                    Transaction tx = new Transaction();
                    tx.setUserId(userId);
                    tx.setDescription(cols[0].trim());
                    String type = cols[1].trim().toUpperCase();
                    tx.setType(type.startsWith("INC") || type.equals("CREDIT") ? "INCOME" : "EXPENSE");
                    tx.setAmount(Math.abs(Double.parseDouble(cols[2].trim())));
                    tx.setCategory(cols.length > 3 ? cols[3].trim() : "Uncategorized");
                    if (cols.length > 4 && !cols[4].trim().isEmpty()) {
                        tx.setDate(LocalDateTime.parse(cols[4].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                    }
                    tx.setNotes(cols.length > 5 ? cols[5].trim() : "");
                    imported.add(tx);
                } catch (Exception e) { errors++; }
            }
            transactionService.saveAll(imported);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to parse CSV: " + e.getMessage());
            return "redirect:/transactions/import";
        }
        redirectAttributes.addFlashAttribute("success",
                "Imported " + imported.size() + " transactions" + (errors > 0 ? " (" + errors + " skipped)" : ""));
        return "redirect:/transactions";
    }

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getId();
    }
}