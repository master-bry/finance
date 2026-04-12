package com.master.finance.service;

import com.master.finance.model.Debt;
import com.master.finance.repository.DebtRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DebtService {

    @Autowired
    private DebtRepository debtRepository;

    // ─── READ ─────────────────────────────────────────────────────────────────

    public List<Debt> getUserDebts(String userId) {
        return debtRepository.findByUserIdAndDeletedFalse(userId);
    }

    public List<Debt> getDebtsOwedToMe(String userId) {
        return debtRepository.findByUserIdAndTypeAndDeletedFalse(userId, "OWED_TO_ME");
    }

    public List<Debt> getDebtsIOwe(String userId) {
        return debtRepository.findByUserIdAndTypeAndDeletedFalse(userId, "I_OWE");
    }

    public Optional<Debt> getDebt(String id) {
        return debtRepository.findById(id).filter(d -> !d.isDeleted());
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    /**
     * Use for NEW debts only.  Sets deleted=false, initialises remainingAmount.
     */
    public Debt saveDebt(Debt debt) {
        debt.setLastUpdated(LocalDateTime.now());
        debt.setDeleted(false);
        if (debt.getRemainingAmount() == null && debt.getAmount() != null) {
            debt.setRemainingAmount(debt.getAmount());
        }
        return debtRepository.save(debt);
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    /**
     * Use for EDITING existing debts.
     * Loads the stored record first so paymentHistory / deleted flag are not wiped.
     */
    public Debt updateDebt(Debt incoming) {
        return debtRepository.findById(incoming.getId()).map(existing -> {
            existing.setPersonName(incoming.getPersonName());
            existing.setType(incoming.getType());
            existing.setAmount(incoming.getAmount());
            existing.setDescription(incoming.getDescription());
            existing.setDueDate(incoming.getDueDate());
            existing.setPhoneNumber(incoming.getPhoneNumber());
            existing.setNotes(incoming.getNotes());

            // Only update status / remainingAmount if explicitly supplied and debt not settled via payments
            if (incoming.getStatus() != null) {
                existing.setStatus(incoming.getStatus());
            }
            if (incoming.getRemainingAmount() != null) {
                existing.setRemainingAmount(incoming.getRemainingAmount());
            }

            existing.setLastUpdated(LocalDateTime.now());
            return debtRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Debt not found: " + incoming.getId()));
    }

    // ─── SOFT DELETE ──────────────────────────────────────────────────────────

    public void deleteDebt(String id) {
        debtRepository.findById(id).ifPresent(debt -> {
            debt.setDeleted(true);
            debt.setDeletedAt(LocalDateTime.now());
            debtRepository.save(debt);
        });
    }

    /** Permanent delete — removes from DB entirely. */
    public void permanentDeleteDebt(String id) {
        debtRepository.deleteById(id);
    }

    // ─── PAYMENT ──────────────────────────────────────────────────────────────

    public Debt makePayment(String debtId, Double amount, String notes) {
        Debt debt = debtRepository.findById(debtId)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new RuntimeException("Debt not found: " + debtId));

        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (amount > debt.getRemainingAmount()) {
            throw new IllegalArgumentException("Payment exceeds remaining balance");
        }

        Debt.PaymentRecord payment = new Debt.PaymentRecord();
        payment.setAmountPaid(amount);
        payment.setNotes(notes);
        debt.getPaymentHistory().add(payment);

        double newRemaining = debt.getRemainingAmount() - amount;
        debt.setRemainingAmount(newRemaining <= 0 ? 0.0 : newRemaining);

        if (newRemaining <= 0) {
            debt.setStatus("SETTLED");
        } else if (newRemaining < debt.getAmount()) {
            debt.setStatus("PARTIAL");
        }

        debt.setLastUpdated(LocalDateTime.now());
        return debtRepository.save(debt);
    }

    // ─── AGGREGATES ───────────────────────────────────────────────────────────

    public Double getTotalOwedToMe(String userId) {
        return debtRepository.findActiveDebtsByUserIdAndTypeAndDeletedFalse(userId, "OWED_TO_ME")
                .stream().mapToDouble(Debt::getRemainingAmount).sum();
    }

    public Double getTotalIOwe(String userId) {
        return debtRepository.findActiveDebtsByUserIdAndTypeAndDeletedFalse(userId, "I_OWE")
                .stream().mapToDouble(Debt::getRemainingAmount).sum();
    }

    public Double getNetPosition(String userId) {
        return getTotalOwedToMe(userId) - getTotalIOwe(userId);
    }

    public List<Debt> getOverdueDebts(String userId) {
        java.time.LocalDate today = java.time.LocalDate.now();
        return getUserDebts(userId).stream()
                .filter(d -> d.getDueDate() != null && d.getDueDate().isBefore(today))
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .toList();
    }
}