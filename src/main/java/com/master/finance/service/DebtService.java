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
        return debtRepository.findById(id);
    }
    
    public Debt saveDebt(Debt debt) {
        debt.setLastUpdated(LocalDateTime.now());
        debt.setDeleted(false);
        return debtRepository.save(debt);
    }
    
    // Soft delete - marks as deleted but keeps in database
    public void deleteDebt(String id) {
        debtRepository.findById(id).ifPresent(debt -> {
            debt.setDeleted(true);
            debt.setDeletedAt(LocalDateTime.now());
            debtRepository.save(debt);
        });
    }
    
    // Permanent delete - removes from database completely
    public void permanentDeleteDebt(String id) {
        debtRepository.deleteById(id);
    }
    
    public Debt makePayment(String debtId, Double amount, String notes) {
        Debt debt = debtRepository.findById(debtId).orElseThrow();
        
        Debt.PaymentRecord payment = new Debt.PaymentRecord();
        payment.setAmountPaid(amount);
        payment.setNotes(notes);
        debt.getPaymentHistory().add(payment);
        
        double newRemaining = debt.getRemainingAmount() - amount;
        debt.setRemainingAmount(newRemaining);
        
        if (newRemaining <= 0) {
            debt.setStatus("SETTLED");
            debt.setRemainingAmount(0.0);
        } else if (newRemaining < debt.getAmount()) {
            debt.setStatus("PARTIAL");
        }
        
        debt.setLastUpdated(LocalDateTime.now());
        return debtRepository.save(debt);
    }
    
    public Double getTotalOwedToMe(String userId) {
        return debtRepository.findActiveDebtsByUserIdAndTypeAndDeletedFalse(userId, "OWED_TO_ME")
                .stream()
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
    }
    
    public Double getTotalIOwe(String userId) {
        return debtRepository.findActiveDebtsByUserIdAndTypeAndDeletedFalse(userId, "I_OWE")
                .stream()
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
    }
    
    public Double getNetPosition(String userId) {
        return getTotalOwedToMe(userId) - getTotalIOwe(userId);
    }
    
    public List<Debt> getOverdueDebts(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return getUserDebts(userId).stream()
                .filter(d -> d.getDueDate() != null && d.getDueDate().isBefore(now))
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .toList();
    }
}