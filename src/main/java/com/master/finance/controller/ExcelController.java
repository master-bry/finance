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
    private TransactionService transactionService;  // Use service, not repository directly

    @Autowired
    private UserService userService;

    @GetMapping("/daily")
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

        return "excel/daily";
    }

    @PostMapping("/add-expense")
    public String addExpense(@RequestParam String description,
                             @RequestParam Double amount,
                             @RequestParam String category,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);

            // 1. Save Transaction (one time only)
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setDescription(description);
            transaction.setAmount(amount);
            transaction.setType("EXPENSE");
            transaction.setCategory(category);
            transaction.setDate(LocalDateTime.now());
            transaction.setDeleted(false);
            transactionService.saveTransaction(transaction); // Use service

            // 2. Get or create today's DailyEntry
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElseGet(() -> {
                DailyEntry newEntry = new DailyEntry();
                newEntry.setUserId(userId);
                newEntry.setDate(LocalDateTime.now());
                // Opening balance will be set by service when saving
                return newEntry;
            });

            // 3. Add expense item
            DailyEntry.ExpenseItem expense = new DailyEntry.ExpenseItem();
            expense.setDescription(description);
            expense.setAmount(amount);
            expense.setCategory(category);
            expense.setTime(LocalDateTime.now());
            entry.getExpenses().add(expense);

            // 4. Save DailyEntry (service will handle opening balance and calculations)
            dailyEntryService.saveDailyEntry(entry, userId);

            redirectAttributes.addFlashAttribute("success", "Expense added: " + amount + " TZS");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding expense: " + e.getMessage());
        }

        return "redirect:/excel/daily";
    }

    @PostMapping("/add-income")
    public String addIncome(@RequestParam String description,
                            @RequestParam Double amount,
                            @RequestParam String source,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);

            // 1. Save Transaction
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setDescription(description);
            transaction.setAmount(amount);
            transaction.setType("INCOME");
            transaction.setCategory(source);
            transaction.setDate(LocalDateTime.now());
            transaction.setDeleted(false);
            transactionService.saveTransaction(transaction);

            // 2. Get or create today's DailyEntry
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElseGet(() -> {
                DailyEntry newEntry = new DailyEntry();
                newEntry.setUserId(userId);
                newEntry.setDate(LocalDateTime.now());
                return newEntry;
            });

            // 3. Add income item
            DailyEntry.IncomeItem income = new DailyEntry.IncomeItem();
            income.setDescription(description);
            income.setAmount(amount);
            income.setSource(source);
            income.setTime(LocalDateTime.now());
            entry.getIncomes().add(income);

            // 4. Save DailyEntry
            dailyEntryService.saveDailyEntry(entry, userId);

            redirectAttributes.addFlashAttribute("success", "Income added: " + amount + " TZS");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding income: " + e.getMessage());
        }

        return "redirect:/excel/daily";
    }

    @GetMapping("/delete-expense/{index}")
    public String deleteExpense(@PathVariable int index, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(null);

            if (entry != null && index < entry.getExpenses().size()) {
                DailyEntry.ExpenseItem expense = entry.getExpenses().get(index);

                // Soft delete the corresponding transaction (optional)
                // You can implement a method to find and delete transaction by matching details
                // For simplicity, we'll just remove from daily entry
                entry.getExpenses().remove(index);
                entry.calculateTotals();
                dailyEntryService.saveDailyEntry(entry, userId);
                redirectAttributes.addFlashAttribute("success", "Expense removed");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error removing expense");
        }
        return "redirect:/excel/daily";
    }

    @GetMapping("/delete-income/{index}")
    public String deleteIncome(@PathVariable int index, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(null);

            if (entry != null && index < entry.getIncomes().size()) {
                entry.getIncomes().remove(index);
                entry.calculateTotals();
                dailyEntryService.saveDailyEntry(entry, userId);
                redirectAttributes.addFlashAttribute("success", "Income removed");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error removing income");
        }
        return "redirect:/excel/daily";
    }

    @PostMapping("/upload")
    public String uploadExcel(@RequestParam("file") MultipartFile file,
                              @RequestParam Double openingBalance,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            dailyEntryService.processExcelFile(file, userId, openingBalance);
            redirectAttributes.addFlashAttribute("success", "Excel file processed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing Excel: " + e.getMessage());
        }
        return "redirect:/excel/daily";
    }

    @GetMapping("/download-template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = dailyEntryService.generateExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=daily_entry_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(template);
    }

    @GetMapping("/history")
    public String viewHistory(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        List<DailyEntry> history = dailyEntryService.getUserEntries(userId);
        model.addAttribute("history", history);
        model.addAttribute("currentPage", "daily");
        model.addAttribute("pageSubtitle", "Daily Entry History");
        return "excel/history";
    }

    @GetMapping("/delete/{id}")
    public String deleteEntry(@PathVariable String id, RedirectAttributes redirectAttributes) {
        dailyEntryService.deleteEntry(id);
        redirectAttributes.addFlashAttribute("success", "Entry deleted");
        return "redirect:/excel/history";
    }

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}