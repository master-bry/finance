package com.master.finance.controller;

import com.master.finance.model.Debt;
import com.master.finance.model.Transaction;
import com.master.finance.service.DebtService;
import com.master.finance.service.TransactionService;
import com.master.finance.service.DailyEntryService;
import com.master.finance.service.UserService;
import com.master.finance.dto.DebtGroupDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/debts")
public class DebtController {

    @Autowired
    private DebtService debtService;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private DailyEntryService dailyEntryService;

    // ─── HELPER METHOD TO GET USER ID ─────────────────────────────────────────
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getId();
    }

    // ─── LIST WITH PAGINATION AND FILTERS ─────────────────────────────────────

    @GetMapping
    public String listDebts(Authentication authentication,
                            Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String type,
                            @RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "false") boolean grouped) {
        String userId = getUserId(authentication);

        if (grouped) {
            // Show grouped view
            List<DebtGroupDTO> groupedDebts = debtService.getGroupedDebtsByPerson(userId, type);
            model.addAttribute("groupedDebts", groupedDebts);
            model.addAttribute("groupedView", true);
        } else {
            // Show paginated view
            Pageable pageable = PageRequest.of(page, size, Sort.by("dateGiven").descending());
            Page<Debt> debtPage = debtService.getUserDebtsFiltered(userId, type, status, pageable);
            model.addAttribute("debts", debtPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", debtPage.getTotalPages());
            model.addAttribute("totalItems", debtPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("groupedView", false);
        }

        // Summary statistics
        model.addAttribute("totalOwedToMe", debtService.getTotalOwedToMe(userId));
        model.addAttribute("totalIOwe", debtService.getTotalIOwe(userId));
        model.addAttribute("netPosition", debtService.getNetPosition(userId));

        // Keep filter values in the model for form persistence
        model.addAttribute("filterType", type);
        model.addAttribute("filterStatus", status);
        model.addAttribute("showGrouped", grouped);

        // Layout attributes
        model.addAttribute("currentPage", "debts");
        model.addAttribute("pageSubtitle", "Manage your debts and lending");
        model.addAttribute("title", "Debts");

        return "debts/index";
    }

    // ─── ADD ─────────────────────────────────────────────────────────────────

    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("debt")) {
            model.addAttribute("debt", new Debt());
        }
        model.addAttribute("currentPage", "debts");
        model.addAttribute("pageSubtitle", "Add a new debt record");
        return "debts/add";
    }

    @PostMapping("/add")
    public String addDebt(@ModelAttribute Debt debt,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        // Validation
        if (debt.getPersonName() == null || debt.getPersonName().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Person name is required.");
            redirectAttributes.addFlashAttribute("debt", debt);
            return "redirect:/debts/add";
        }
        if (debt.getType() == null || debt.getType().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Debt type is required.");
            redirectAttributes.addFlashAttribute("debt", debt);
            return "redirect:/debts/add";
        }
        if (debt.getAmount() == null || debt.getAmount() <= 0) {
            redirectAttributes.addFlashAttribute("error", "A positive amount is required.");
            redirectAttributes.addFlashAttribute("debt", debt);
            return "redirect:/debts/add";
        }

        String userId = getUserId(authentication);
        debt.setUserId(userId);
        debt.setRemainingAmount(debt.getAmount());
        debt.setStatus("PENDING");
        debt.setDateGiven(LocalDateTime.now());
        debt.setLastUpdated(LocalDateTime.now());

        debtService.saveDebt(debt);
        redirectAttributes.addFlashAttribute("success", "Debt added successfully!");
        return "redirect:/debts";
    }

    // ─── EDIT ────────────────────────────────────────────────────────────────

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> debtOpt = debtService.getDebt(id);

        if (debtOpt.isEmpty() || !userId.equals(debtOpt.get().getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Debt not found or access denied.");
            return "redirect:/debts";
        }

        model.addAttribute("debt", debtOpt.get());
        model.addAttribute("currentPage", "debts");
        model.addAttribute("pageSubtitle", "Edit debt record");
        return "debts/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateDebt(@PathVariable String id,
                             @ModelAttribute Debt debt,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> existingOpt = debtService.getDebt(id);

        if (existingOpt.isEmpty() || !userId.equals(existingOpt.get().getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Debt not found or access denied.");
            return "redirect:/debts";
        }

        Debt existing = existingOpt.get();
        existing.setPersonName(debt.getPersonName());
        existing.setType(debt.getType());
        existing.setAmount(debt.getAmount());
        existing.setDescription(debt.getDescription());
        existing.setDueDate(debt.getDueDate());
        existing.setPhoneNumber(debt.getPhoneNumber());
        existing.setNotes(debt.getNotes());
        existing.setStatus(debt.getStatus());
        existing.setLastUpdated(LocalDateTime.now());

        debtService.updateDebt(existing);
        redirectAttributes.addFlashAttribute("success", "Debt updated successfully!");
        return "redirect:/debts";
    }
  //recept
    @GetMapping("/view-receipt/{debtId}/{paymentIndex}")
public String viewReceipt(@PathVariable String debtId,
                          @PathVariable int paymentIndex,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
    String userId = getUserId(authentication);
    Optional<Debt> debtOpt = debtService.getDebt(debtId);
    
    if (debtOpt.isEmpty() || !userId.equals(debtOpt.get().getUserId())) {
        redirectAttributes.addFlashAttribute("error", "Debt not found.");
        return "redirect:/debts";
    }
    
    Debt debt = debtOpt.get();
    
    if (paymentIndex >= 0 && paymentIndex < debt.getPaymentHistory().size()) {
        model.addAttribute("debt", debt);
        model.addAttribute("latestPayment", debt.getPaymentHistory().get(paymentIndex));
        model.addAttribute("currentDate", debt.getPaymentHistory().get(paymentIndex).getPaymentDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        model.addAttribute("receiptNo", "RCP-" + debt.getId().substring(0, 8) + "-" + (paymentIndex + 1));
        model.addAttribute("currentPage", "debts");
        return "debts/receipt";
    }
    
    redirectAttributes.addFlashAttribute("error", "Payment not found.");
    return "redirect:/debts";
}

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @GetMapping("/delete/{id}")
    public String deleteDebt(@PathVariable String id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        boolean deleted = debtService.getDebt(id)
                .filter(debt -> userId.equals(debt.getUserId()))
                .map(debt -> {
                    debtService.deleteDebt(debt.getId());
                    return true;
                })
                .orElse(false);

        if (deleted) {
            redirectAttributes.addFlashAttribute("success", "Debt deleted successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Debt not found or access denied.");
        }
        return "redirect:/debts";
    }

    // ─── PAYMENT ─────────────────────────────────────────────────────────────

    @GetMapping("/make-payment/{id}")
    public String showPaymentForm(@PathVariable String id,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> debtOpt = debtService.getDebt(id);

        if (debtOpt.isEmpty() || !userId.equals(debtOpt.get().getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Debt not found.");
            return "redirect:/debts";
        }

        model.addAttribute("debt", debtOpt.get());
        model.addAttribute("currentPage", "debts");
        model.addAttribute("pageSubtitle", "Record a payment");
        return "debts/payment";
    }

    @PostMapping("/make-payment/{id}")
    public String makePayment(@PathVariable String id,
                              @RequestParam Double amount,
                              @RequestParam(required = false, defaultValue = "") String notes,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> debtOpt = debtService.getDebt(id);

        if (debtOpt.isEmpty() || !userId.equals(debtOpt.get().getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Payment failed. Debt not found.");
            return "redirect:/debts";
        }

        Debt debt = debtOpt.get();
        boolean isFullyPaid = (debt.getRemainingAmount() - amount) <= 0;

        try {
            // 1. Record the payment in DebtService
            debtService.makePayment(id, amount, notes);

            // 2. Create a corresponding Transaction
            Transaction tx = new Transaction();
            tx.setUserId(debt.getUserId());
            tx.setDescription("Debt Payment: " + debt.getPersonName() + (notes.isEmpty() ? "" : " - " + notes));
            tx.setAmount(amount);
            tx.setType("I_OWE".equals(debt.getType()) ? "EXPENSE" : "INCOME");
            tx.setCategory("Debt");
            tx.setDate(LocalDateTime.now());
            tx.setDeleted(false);
            transactionService.saveTransaction(tx);

            // 3. Update today's DailyEntry and recalculate balances
            dailyEntryService.getOrCreateTodayEntry(debt.getUserId());
            dailyEntryService.recalculateBalancesFromDate(debt.getUserId(), LocalDateTime.now());

            // 4. Generate receipt after payment
            Map<String, Object> receiptData = generateReceiptData(debt, amount, notes, isFullyPaid);
            
            redirectAttributes.addFlashAttribute("showReceipt", true);
            redirectAttributes.addFlashAttribute("receiptData", receiptData);
            redirectAttributes.addFlashAttribute("success", "Payment recorded successfully!");
            
            // Redirect to receipt page instead of debts list
            return "redirect:/debts/receipt/" + id;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment failed: " + e.getMessage());
            return "redirect:/debts";
        }
    }
    
    // ─── RECEIPT GENERATION ──────────────────────────────────────────────────
    
    @GetMapping("/receipt/{debtId}")
    public String showReceipt(@PathVariable String debtId,
                              Authentication authentication,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        Optional<Debt> debtOpt = debtService.getDebt(debtId);
        
        if (debtOpt.isEmpty() || !userId.equals(debtOpt.get().getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Debt not found.");
            return "redirect:/debts";
        }
        
        Debt debt = debtOpt.get();
        
        // Get the latest payment
        Debt.PaymentRecord latestPayment = null;
        if (debt.getPaymentHistory() != null && !debt.getPaymentHistory().isEmpty()) {
            latestPayment = debt.getPaymentHistory().get(debt.getPaymentHistory().size() - 1);
        }
        
        model.addAttribute("debt", debt);
        model.addAttribute("latestPayment", latestPayment);
        model.addAttribute("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        model.addAttribute("receiptNo", "RCP-" + System.currentTimeMillis());
        model.addAttribute("currentPage", "debts");
        
        return "debts/receipt";
    }
    
    private Map<String, Object> generateReceiptData(Debt debt, Double amount, String notes, boolean isFullyPaid) {
        Map<String, Object> receiptData = new HashMap<>();
        receiptData.put("personName", debt.getPersonName());
        receiptData.put("amountPaid", amount);
        receiptData.put("remainingBalance", debt.getRemainingAmount() - amount);
        receiptData.put("fullyPaid", isFullyPaid);
        receiptData.put("type", debt.getType());
        receiptData.put("notes", notes);
        receiptData.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        receiptData.put("receiptNo", "RCP-" + System.currentTimeMillis());
        receiptData.put("phoneNumber", debt.getPhoneNumber());
        return receiptData;
    }
}