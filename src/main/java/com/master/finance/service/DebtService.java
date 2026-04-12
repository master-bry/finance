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
    
    public Optional<Debt> getDebt(String id) {
        return debtRepository.findById(id);
    }
    
    public Debt saveDebt(Debt debt) {
        // Set required fields
        if (debt.getDateGiven() == null) {
            debt.setDateGiven(LocalDateTime.now());
        }
        debt.setLastUpdated(LocalDateTime.now());
        debt.setDeleted(false);
        
        // Set remaining amount if not set
        if (debt.getRemainingAmount() == null || debt.getRemainingAmount() == 0) {
            debt.setRemainingAmount(debt.getAmount());
        }
        
        // Set status
        if (debt.getRemainingAmount() <= 0) {
            debt.setStatus("SETTLED");
        } else if (debt.getRemainingAmount() < debt.getAmount()) {
            debt.setStatus("PARTIAL");
        } else {
            debt.setStatus("PENDING");
        }
        
        // Initialize payment history if null
        if (debt.getPaymentHistory() == null) {
            debt.setPaymentHistory(new java.util.ArrayList<>());
        }
        
        return debtRepository.save(debt);
    }
    
    public void deleteDebt(String id) {
        debtRepository.findById(id).ifPresent(debt -> {
            debt.setDeleted(true);
            debt.setDeletedAt(LocalDateTime.now());
            debtRepository.save(debt);
        });
    }
    
    public Debt makePayment(String debtId, Double amount, String notes) {
        Debt debt = debtRepository.findById(debtId).orElseThrow();
        
        Debt.PaymentRecord payment = new Debt.PaymentRecord();
        payment.setAmountPaid(amount);
        payment.setNotes(notes != null ? notes : "");
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
        List<Debt> debts = debtRepository.findByUserIdAndTypeAndDeletedFalse(userId, "OWED_TO_ME");
        return debts.stream()
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
    }
    
    public Double getTotalIOwe(String userId) {
        List<Debt> debts = debtRepository.findByUserIdAndTypeAndDeletedFalse(userId, "I_OWE");
        return debts.stream()
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
    }
}