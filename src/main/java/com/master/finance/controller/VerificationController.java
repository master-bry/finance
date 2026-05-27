package com.master.finance.controller;

import com.master.finance.model.Debt;
import com.master.finance.service.DebtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
@RequestMapping("/verify")
public class VerificationController {

    @Autowired
    private DebtService debtService;

    @GetMapping("/debt/{debtId}/{paymentIndex}")
    public String verifyDebtPayment(@PathVariable String debtId,
                                    @PathVariable int paymentIndex,
                                    Model model) {
        Optional<Debt> debtOpt = debtService.getDebt(debtId);

        if (debtOpt.isEmpty()) {
            model.addAttribute("error", "Invalid receipt. Transaction record not found.");
            return "verify/error";
        }

        Debt debt = debtOpt.get();

        if (paymentIndex < 0 || paymentIndex >= debt.getPaymentHistory().size()) {
            model.addAttribute("error", "Invalid receipt. Payment record not found.");
            return "verify/error";
        }

        Debt.PaymentRecord payment = debt.getPaymentHistory().get(paymentIndex);

        model.addAttribute("debt", debt);
        model.addAttribute("payment", payment);
        model.addAttribute("paymentDate", payment.getPaymentDate()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        model.addAttribute("verified", true);

        return "verify/debt";
    }
}
