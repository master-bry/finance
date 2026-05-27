package com.master.finance.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.master.finance.model.Bill;
import com.master.finance.model.DailyEntry;
import com.master.finance.model.Transaction;
import com.master.finance.model.User;
import com.master.finance.service.BillService;
import com.master.finance.service.DailyEntryService;
import com.master.finance.service.TransactionService;
import com.master.finance.service.UserService;

@Controller
@RequestMapping("/excel")
public class ExcelController {

    @Autowired
    private DailyEntryService dailyEntryService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BillService billService;

    @GetMapping("/daily-entry")
    public String showDailyEntry(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        DailyEntry todayEntry = dailyEntryService.getTodayEntry(userId).orElse(new DailyEntry());
        Double currentBalance = dailyEntryService.getCurrentBalance(userId);
        List<DailyEntry> history = dailyEntryService.getUserEntries(userId);
        List<Bill> allBills = billService.getUserBills(userId);
        // getPendingBills now returns PENDING + PARTIAL bills (available for use)
        List<Bill> pendingBills = billService.getPendingBills(userId);

        model.addAttribute("entry", todayEntry);
        model.addAttribute("currentBalance", currentBalance);
        model.addAttribute("history", history);
        model.addAttribute("allBills", allBills);
        model.addAttribute("pendingBills", pendingBills);
        model.addAttribute("currentPage", "daily");
        model.addAttribute("pageSubtitle", "Daily cash journal — log income, expenses, and track your daily balance");
        model.addAttribute("title", "Daily Entry");

        return "excel/daily-entry";
    }

    @PostMapping("/add-expense")
    public String addExpense(@RequestParam String description,
                             @RequestParam Double amount,
                             @RequestParam(required = false) String category,
                             @RequestParam(required = false, defaultValue = "CASH") String paymentMethod,
                             @RequestParam(required = false) List<String> billIds,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            String curr = userService.findById(userId).map(User::getCurrency).orElse("TZS");
            
            if ("CASH".equals(paymentMethod)) {
                Double currentBalance = dailyEntryService.getCurrentBalance(userId);
                if (currentBalance == null || currentBalance <= 0) {
                    redirectAttributes.addFlashAttribute("error", "Insufficient balance! Please add income first before using cash.");
                    return "redirect:/excel/daily-entry";
                }
                if (currentBalance < amount) {
                    redirectAttributes.addFlashAttribute("error", "Insufficient balance! You only have " + String.format("%.0f", currentBalance) + " " + curr + ", but you're trying to use " + String.format("%.0f", amount) + " " + curr + ". Please add income first.");
                    return "redirect:/excel/daily-entry";
                }
            }

            DailyEntry entry = dailyEntryService.getOrCreateTodayEntry(userId);

            DailyEntry.ExpenseItem expense = new DailyEntry.ExpenseItem();
            expense.setDescription(description);
            expense.setAmount(amount);
            expense.setCategory(category);
            expense.setTime(LocalDateTime.now());
            expense.setPaymentMethod(paymentMethod);

            if ("BILL".equals(paymentMethod)) {
                if (billIds == null || billIds.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Please select at least one prepaid credit.");
                    return "redirect:/excel/daily-entry";
                }
                
                double totalAvailable = 0;
                List<Bill> selectedBills = new ArrayList<>();
                for (String bid : billIds) {
                    Bill b = billService.getBill(bid).orElse(null);
                    if (b != null) {
                        selectedBills.add(b);
                        totalAvailable += b.getAmount();
                    }
                }
                
                if (totalAvailable + 0.01 < amount) {
                    redirectAttributes.addFlashAttribute("error",
                        "Insufficient prepaid balance. Selected credits have " + String.format("%.0f", totalAvailable) + " " + curr +
                        " but you need " + String.format("%.0f", amount) + " " + curr + ".");
                    return "redirect:/excel/daily-entry";
                }
                
                double remaining = amount;
                List<String> chargedNames = new ArrayList<>();
                String firstBillId = null;
                for (Bill bill : selectedBills) {
                    if (remaining <= 0.01) break;
                    double toCharge = Math.min(remaining, bill.getAmount());
                    if (toCharge <= 0) continue;
                    billService.applyPayment(bill.getId(), toCharge);
                    if (firstBillId == null) firstBillId = bill.getId();
                    chargedNames.add(bill.getName() + " (" + String.format("%.0f", toCharge) + " " + curr + ")");
                    remaining -= toCharge;
                }
                
                expense.setBillId(firstBillId);
                entry.getPrepaidExpenses().add(expense);
                redirectAttributes.addFlashAttribute("success",
                    "Used " + String.format("%.0f", amount) + " " + curr + " from: " + String.join(", ", chargedNames));
            } else {
                Transaction transaction = new Transaction();
                transaction.setUserId(userId);
                transaction.setDescription(description);
                transaction.setAmount(amount);
                transaction.setType("EXPENSE");
                transaction.setCategory(category);
                transaction.setDate(LocalDateTime.now());
                transaction.setDeleted(false);
                transactionService.saveTransaction(transaction);
                entry.getExpenses().add(expense);
                redirectAttributes.addFlashAttribute("success", "Cash expense added: " + String.format("%.0f", amount) + " " + curr);
            }

            entry.calculateTotals();
            dailyEntryService.saveDailyEntry(entry, userId);
            dailyEntryService.recalculateBalancesFromDate(userId, LocalDateTime.now());

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
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
            String curr = userService.findById(userId).map(User::getCurrency).orElse("TZS");
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
            entry.calculateTotals();
            dailyEntryService.saveDailyEntry(entry, userId);
            dailyEntryService.recalculateBalancesFromDate(userId, LocalDateTime.now());

            redirectAttributes.addFlashAttribute("success", "Income added: " + String.format("%.0f", amount) + " " + curr);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding income: " + e.getMessage());
        }
        return "redirect:/excel/daily-entry";
    }

    // Delete cash expense
    @GetMapping("/delete-expense/{index}")
    public String deleteExpense(@PathVariable int index, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(null);
            if (entry != null && index < entry.getExpenses().size()) {
                DailyEntry.ExpenseItem expense = entry.getExpenses().get(index);
                // Find and delete matching transaction
                List<Transaction> todayTransactions = transactionService.getTransactionsByDateRange(
                        userId,
                        LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                        LocalDateTime.now().withHour(23).withMinute(59).withSecond(59));
                for (Transaction t : todayTransactions) {
                    if (t.getDescription().equals(expense.getDescription()) &&
                        t.getAmount().equals(expense.getAmount()) &&
                        "EXPENSE".equals(t.getType())) {
                        transactionService.deleteTransaction(t.getId());
                        break;
                    }
                }
                entry.getExpenses().remove(index);
                entry.calculateTotals();
                dailyEntryService.saveDailyEntry(entry, userId);
                dailyEntryService.recalculateBalancesFromDate(userId, LocalDateTime.now());
                redirectAttributes.addFlashAttribute("success", "Cash expense removed");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error removing expense: " + e.getMessage());
        }
        return "redirect:/excel/daily-entry";
    }

    // Delete prepaid expense
    @GetMapping("/delete-prepaid-expense/{index}")
    public String deletePrepaidExpense(@PathVariable int index, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(null);
            if (entry != null && index < entry.getPrepaidExpenses().size()) {
                entry.getPrepaidExpenses().remove(index);
                entry.calculateTotals();
                dailyEntryService.saveDailyEntry(entry, userId);
                redirectAttributes.addFlashAttribute("success", "Prepaid expense removed");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error removing prepaid expense: " + e.getMessage());
        }
        return "redirect:/excel/daily-entry";
    }

    @GetMapping("/delete-income/{index}")
    public String deleteIncome(@PathVariable int index, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(null);
            if (entry != null && index < entry.getIncomes().size()) {
                DailyEntry.IncomeItem income = entry.getIncomes().get(index);
                List<Transaction> todayTransactions = transactionService.getTransactionsByDateRange(
                        userId,
                        LocalDateTime.now().withHour(0).withMinute(0).withSecond(0),
                        LocalDateTime.now().withHour(23).withMinute(59).withSecond(59));
                for (Transaction t : todayTransactions) {
                    if (t.getDescription().equals(income.getDescription()) &&
                        t.getAmount().equals(income.getAmount()) &&
                        "INCOME".equals(t.getType())) {
                        transactionService.deleteTransaction(t.getId());
                        break;
                    }
                }
                entry.getIncomes().remove(index);
                entry.calculateTotals();
                dailyEntryService.saveDailyEntry(entry, userId);
                dailyEntryService.recalculateBalancesFromDate(userId, LocalDateTime.now());
                redirectAttributes.addFlashAttribute("success", "Income removed");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error removing income: " + e.getMessage());
        }
        return "redirect:/excel/daily-entry";
    }

    @PostMapping("/upload")
    public String uploadExcel(@RequestParam("file") MultipartFile file,
                              @RequestParam Double openingBalance,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            dailyEntryService.processExcelFile(file, userId, openingBalance);
            dailyEntryService.recalculateBalancesFromDate(userId, LocalDateTime.now());
            redirectAttributes.addFlashAttribute("success", "Excel file processed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error processing Excel: " + e.getMessage());
        }
        return "redirect:/excel/daily-entry";
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
        model.addAttribute("pageSubtitle", "Historical daily entries — review past financial activity");
        model.addAttribute("title", "History");
        return "excel/history";
    }

    @GetMapping("/delete/{id}")
    public String deleteEntry(@PathVariable String id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            DailyEntry entry = dailyEntryService.getEntryById(id).orElse(null);
            if (entry != null && entry.getUserId().equals(userId)) {
                LocalDateTime start = entry.getDate().withHour(0).withMinute(0).withSecond(0);
                LocalDateTime end = entry.getDate().withHour(23).withMinute(59).withSecond(59);
                List<Transaction> dayTransactions = transactionService.getTransactionsByDateRange(userId, start, end);
                for (Transaction t : dayTransactions) {
                    transactionService.deleteTransaction(t.getId());
                }
                dailyEntryService.deleteEntry(id);
                dailyEntryService.recalculateBalancesFromDate(userId, entry.getDate());
                redirectAttributes.addFlashAttribute("success", "Entry and transactions removed");
            } else {
                redirectAttributes.addFlashAttribute("error", "Entry not found or permission denied");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting entry: " + e.getMessage());
        }
        return "redirect:/excel/history";
    }

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}