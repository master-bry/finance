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

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getId();
    }

    // ─── LIST WITH PAGINATION, FILTERS AND GROUPED VIEW ──────────────────────

    @GetMapping
    public String listDebts(Authentication authentication,
                            Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String type,
                            @RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "false") boolean grouped) {

        String userId = getUserId(authentication);

        // ── Common summary stats ──
        model.addAttribute("totalOwedToMe", debtService.getTotalOwedToMe(userId));
        model.addAttribute("totalIOwe", debtService.getTotalIOwe(userId));
        model.addAttribute("netPosition", debtService.getNetPosition(userId));
        model.addAttribute("filterType", type);
        model.addAttribute("filterStatus", status);
        model.addAttribute("pageSize", size);
        model.addAttribute("groupedView", grouped);

        // Layout nav (must be string "debts" for active nav highlight)
        model.addAttribute("currentPage", "debts");
        model.addAttribute("pageSubtitle", "Manage your debts and lending");
        model.addAttribute("title", "Debts");

        if (grouped) {
            // ── Grouped view: no pagination ──
            List<DebtGroupDTO> groupedDebts = debtService.getGroupedDebtsByPerson(userId, type);
            model.addAttribute("groupedDebts", groupedDebts);
            model.addAttribute("totalItems", groupedDebts.stream()
                    .mapToInt(DebtGroupDTO::getDebtCount).sum());
            // Dummy pagination values so template th:if checks don't error
            model.addAttribute("paginationPage", 0);
            model.addAttribute("totalPages", 1);
        } else {
            // ── List view: paginated ──
            Pageable pageable = PageRequest.of(page, size, Sort.by("dateGiven").descending());
            Page<Debt> debtPage = debtService.getUserDebtsFiltered(userId, type, status, pageable);
            model.addAttribute("debts", debtPage.getContent());
            model.addAttribute("paginationPage", page);
            model.addAttribute("totalPages", debtPage.getTotalPages());
            model.addAttribute("totalItems", debtPage.getTotalElements());
        }

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
        model.addAttribute("title", "Add Debt");
        return "debts/add";
    }

    @PostMapping("/add")
    public String addDebt(@ModelAttribute Debt debt,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
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
        model.addAttribute("title", "Edit Debt");
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
        model.addAttribute("title", "Make Payment");
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

        try {
            // 1. Record the payment
            debtService.makePayment(id, amount, notes);

            // 2. Create corresponding Transaction
            Transaction tx = new Transaction();
            tx.setUserId(debt.getUserId());
            tx.setDescription("Debt Payment: " + debt.getPersonName()
                    + (notes.isEmpty() ? "" : " - " + notes));
            tx.setAmount(amount);
            tx.setType("I_OWE".equals(debt.getType()) ? "EXPENSE" : "INCOME");
            tx.setCategory("Debt");
            tx.setDate(LocalDateTime.now());
            tx.setDeleted(false);
            transactionService.saveTransaction(tx);

            // 3. Recalculate daily balances
            dailyEntryService.getOrCreateTodayEntry(debt.getUserId());
            dailyEntryService.recalculateBalancesFromDate(debt.getUserId(), LocalDateTime.now());

            // 4. Redirect to receipt page
            return "redirect:/debts/receipt/" + id;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/debts/make-payment/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Payment failed: " + e.getMessage());
            return "redirect:/debts";
        }
    }

    // ─── RECEIPT ─────────────────────────────────────────────────────────────

    /**
     * Shows the latest payment receipt for a debt.
     */
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
        Debt.PaymentRecord latestPayment = null;
        int paymentIndex = 0;

        if (debt.getPaymentHistory() != null && !debt.getPaymentHistory().isEmpty()) {
            paymentIndex = debt.getPaymentHistory().size() - 1;
            latestPayment = debt.getPaymentHistory().get(paymentIndex);
        }

        model.addAttribute("debt", debt);
        model.addAttribute("latestPayment", latestPayment);
        model.addAttribute("currentDate", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        model.addAttribute("receiptNo",
                "RCP-" + debt.getId().substring(0, 8).toUpperCase() + "-" + (paymentIndex + 1));
        model.addAttribute("currentPage", "debts");
        model.addAttribute("title", "Payment Receipt");

        return "debts/receipt";
    }

    /**
     * Shows a specific historical payment receipt by index.
     */
    @GetMapping("/view-receipt/{debtId}/{paymentIndex}")
    public String viewReceiptByIndex(@PathVariable String debtId,
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

        if (paymentIndex < 0 || paymentIndex >= debt.getPaymentHistory().size()) {
            redirectAttributes.addFlashAttribute("error", "Payment record not found.");
            return "redirect:/debts";
        }

        Debt.PaymentRecord payment = debt.getPaymentHistory().get(paymentIndex);

        model.addAttribute("debt", debt);
        model.addAttribute("latestPayment", payment);
        model.addAttribute("currentDate", payment.getPaymentDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        model.addAttribute("receiptNo",
                "RCP-" + debt.getId().substring(0, 8).toUpperCase() + "-" + (paymentIndex + 1));
        model.addAttribute("currentPage", "debts");
        model.addAttribute("title", "Payment Receipt");

        return "debts/receipt";
    }
}