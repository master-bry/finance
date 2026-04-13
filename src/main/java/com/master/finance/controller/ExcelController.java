package com.master.finance.controller;

import com.master.finance.model.DailyEntry;
import com.master.finance.model.Transaction;
import com.master.finance.service.DailyEntryService;
import com.master.finance.service.TransactionService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/excel")
public class ExcelController {

    @Autowired
    private DailyEntryService dailyEntryService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @GetMapping("/daily-entry")
    public String showDailyEntry(Authentication authentication, Model model) {
        String userId = getUserId(authentication);

        DailyEntry todayEntry = dailyEntryService.getTodayEntry(userId).orElse(new DailyEntry());
        Double currentBalance = dailyEntryService.getCurrentBalance(userId);
        List<DailyEntry> history = dailyEntryService.getUserEntries(userId);

        model.addAttribute("entry", todayEntry);
        model.addAttribute("currentBalance", currentBalance);
        model.addAttribute("history", history);
        model.addAttribute("currentPage", "daily");
        model.addAttribute("pageSubtitle", "Record your daily expenses and income");
        model.addAttribute("title", "Daily Entry");

        return "excel/daily-entry";
    }

    @PostMapping("/add-expense")
    public String addExpense(@RequestParam String description,
                             @RequestParam Double amount,
                             @RequestParam String category,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);

            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setDescription(description);
            transaction.setAmount(amount);
            transaction.setType("EXPENSE");
            transaction.setCategory(category);
            transaction.setDate(LocalDateTime.now());
            transaction.setDeleted(false);
            transactionService.saveTransaction(transaction);

            DailyEntry entry = dailyEntryService.getOrCreateTodayEntry(userId);

            DailyEntry.ExpenseItem expense = new DailyEntry.ExpenseItem();
            expense.setDescription(description);
            expense.setAmount(amount);
            expense.setCategory(category);
            expense.setTime(LocalDateTime.now());
            entry.getExpenses().add(expense);

            dailyEntryService.saveDailyEntry(entry, userId);
            dailyEntryService.recalculateBalancesFromDate(userId, LocalDateTime.now());

            redirectAttributes.addFlashAttribute("success", "Expense added: " + amount + " TZS");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding expense: " + e.getMessage());
        }

        return "redirect:/excel/daily-entry";
    }

    @PostMapping("/add-income")
    public String addIncome(@RequestParam String description,
                            @RequestParam Double amount,
                            @RequestParam String source,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);

            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setDescription(description);
            transaction.setAmount(amount);
            transaction.setType("INCOME");
            transaction.setCategory(source);
            transaction.setDate(LocalDateTime.now());
            transaction.setDeleted(false);
            transactionService.saveTransaction(transaction);

            DailyEntry entry = dailyEntryService.getOrCreateTodayEntry(userId);

            DailyEntry.IncomeItem income = new DailyEntry.IncomeItem();
            income.setDescription(description);
            income.setAmount(amount);
            income.setSource(source);
            income.setTime(LocalDateTime.now());
            entry.getIncomes().add(income);

            dailyEntryService.saveDailyEntry(entry, userId);
            dailyEntryService.recalculateBalancesFromDate(userId, LocalDateTime.now());

            redirectAttributes.addFlashAttribute("success", "Income added: " + amount + " TZS");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding income: " + e.getMessage());
        }

        return "redirect:/excel/daily-entry";
    }

    // ... other methods unchanged (delete, upload, history, etc.) ...
    // They should also call recalculateBalancesFromDate after changes

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}