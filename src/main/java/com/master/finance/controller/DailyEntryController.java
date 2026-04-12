package com.master.finance.controller;

import com.master.finance.model.DailyEntry;
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
@RequestMapping("/daily-entry")
public class DailyEntryController {
    
    @Autowired
    private DailyEntryService dailyEntryService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String showDailyEntry(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        
        // Get today's entry if exists
        DailyEntry todayEntry = dailyEntryService.getTodayEntry(userId).orElse(new DailyEntry());
        Double currentBalance = dailyEntryService.getCurrentBalance(userId);
        List<DailyEntry> history = dailyEntryService.getUserEntries(userId);
        
        model.addAttribute("entry", todayEntry);
        model.addAttribute("currentBalance", currentBalance);
        model.addAttribute("history", history);
        model.addAttribute("currentPage", "daily");
        model.addAttribute("title", "Daily Entry");
        
        return "daily-entry/index";
    }
    
    @PostMapping("/add-expense")
    public String addExpense(@RequestParam String description,
                             @RequestParam Double amount,
                             @RequestParam String category,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(new DailyEntry());
            
            if (entry.getId() == null) {
                entry.setUserId(userId);
                entry.setDate(LocalDateTime.now());
                entry.setOpeningBalance(dailyEntryService.getCurrentBalance(userId));
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
        
        return "redirect:/daily-entry";
    }
    
    @PostMapping("/add-income")
    public String addIncome(@RequestParam String description,
                            @RequestParam Double amount,
                            @RequestParam String source,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(new DailyEntry());
            
            if (entry.getId() == null) {
                entry.setUserId(userId);
                entry.setDate(LocalDateTime.now());
                entry.setOpeningBalance(dailyEntryService.getCurrentBalance(userId));
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
        
        return "redirect:/daily-entry";
    }
    
    @GetMapping("/delete-expense/{index}")
    public String deleteExpense(@PathVariable int index, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            DailyEntry entry = dailyEntryService.getTodayEntry(userId).orElse(null);
            
            if (entry != null && index < entry.getExpenses().size()) {
                entry.getExpenses().remove(index);
                entry.calculateTotals();
                dailyEntryService.saveDailyEntry(entry, userId);
                redirectAttributes.addFlashAttribute("success", "Expense removed");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error removing expense");
        }
        return "redirect:/daily-entry";
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
        return "redirect:/daily-entry";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteEntry(@PathVariable String id, RedirectAttributes redirectAttributes) {
        dailyEntryService.deleteEntry(id);
        redirectAttributes.addFlashAttribute("success", "Entry deleted");
        return "redirect:/daily-entry";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}