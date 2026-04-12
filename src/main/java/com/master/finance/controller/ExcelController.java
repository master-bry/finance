package com.master.finance.controller;

import com.master.finance.model.DailyEntry;
import com.master.finance.model.Transaction;
import com.master.finance.repository.TransactionRepository;
import com.master.finance.service.DailyEntryService;
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
    private TransactionRepository transactionRepository;
    
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
            
            // Save as transaction first
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setDescription(description);
            transaction.setAmount(amount);
            transaction.setType("EXPENSE");
            transaction.setCategory(category);
            transaction.setDate(LocalDateTime.now());
            transaction.setDeleted(false);
            transactionRepository.save(transaction);
            
            // Then update daily entry
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(new DailyEntry());
            if (entry.getId() == null) {
                entry.setUserId(userId);
                entry.setDate(LocalDateTime.now());
                entry.setOpeningBalance(dailyEntryService.getCurrentBalance(userId) - amount);
            }
            
            DailyEntry.ExpenseItem expense = new DailyEntry.ExpenseItem();
            expense.setDescription(description);
            expense.setAmount(amount);
            expense.setCategory(category);
            entry.getExpenses().add(expense);
            entry.calculateTotals();
            
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
            
            // Save as transaction first
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setDescription(description);
            transaction.setAmount(amount);
            transaction.setType("INCOME");
            transaction.setCategory(source);
            transaction.setDate(LocalDateTime.now());
            transaction.setDeleted(false);
            transactionRepository.save(transaction);
            
            // Then update daily entry
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(new DailyEntry());
            if (entry.getId() == null) {
                entry.setUserId(userId);
                entry.setDate(LocalDateTime.now());
                entry.setOpeningBalance(dailyEntryService.getCurrentBalance(userId) - amount);
            }
            
            DailyEntry.IncomeItem income = new DailyEntry.IncomeItem();
            income.setDescription(description);
            income.setAmount(amount);
            income.setSource(source);
            entry.getIncomes().add(income);
            entry.calculateTotals();
            
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
                
                // Delete from transactions
                List<Transaction> transactions = transactionRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
                for (Transaction t : transactions) {
                    if (t.getDescription().equals(expense.getDescription()) && 
                        t.getAmount().equals(expense.getAmount()) &&
                        t.getType().equals("EXPENSE")) {
                        transactionRepository.deleteById(t.getId());
                        break;
                    }
                }
                
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
                DailyEntry.IncomeItem income = entry.getIncomes().get(index);
                
                // Delete from transactions
                List<Transaction> transactions = transactionRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
                for (Transaction t : transactions) {
                    if (t.getDescription().equals(income.getDescription()) && 
                        t.getAmount().equals(income.getAmount()) &&
                        t.getType().equals("INCOME")) {
                        transactionRepository.deleteById(t.getId());
                        break;
                    }
                }
                
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
        return "excel/history";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteEntry(@PathVariable String id, RedirectAttributes redirectAttributes) {
        dailyEntryService.deleteEntry(id);
        redirectAttributes.addFlashAttribute("success", "Entry deleted");
        return "redirect:/excel/history";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}