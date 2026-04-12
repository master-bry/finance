package com.master.finance.service;

import com.master.finance.model.Investment;
import com.master.finance.repository.InvestmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InvestmentService {
    
    @Autowired
    private InvestmentRepository investmentRepository;
    
    public List<Investment> getUserInvestments(String userId) {
        return investmentRepository.findByUserIdAndDeletedFalse(userId);
    }
    
    public Optional<Investment> getInvestment(String id) {
        return investmentRepository.findById(id);
    }
    
    public Investment saveInvestment(Investment investment) {
        if (investment.getStartDate() == null) {
            investment.setStartDate(LocalDateTime.now());
        }
        if (investment.getStatus() == null) {
            investment.setStatus("ACTIVE");
        }
        if (investment.getCurrentValue() == null) {
            investment.setCurrentValue(investment.getAmountInvested());
        }
        investment.setDeleted(false);
        return investmentRepository.save(investment);
    }
    
    public void deleteInvestment(String id) {
        investmentRepository.findById(id).ifPresent(investment -> {
            investment.setDeleted(true);
            investment.setDeletedAt(LocalDateTime.now());
            investmentRepository.save(investment);
        });
    }
    
    public Investment updateCurrentValue(String id, Double newValue) {
        Investment investment = investmentRepository.findById(id).orElseThrow();
        investment.setCurrentValue(newValue);
        return investmentRepository.save(investment);
    }
    
    public Double getTotalInvested(String userId) {
        return investmentRepository.findByUserIdAndDeletedFalse(userId)
                .stream()
                .mapToDouble(Investment::getAmountInvested)
                .sum();
    }
    
    public Double getTotalCurrentValue(String userId) {
        return investmentRepository.findByUserIdAndDeletedFalse(userId)
                .stream()
                .mapToDouble(Investment::getCurrentValue)
                .sum();
    }
    
    public Double getTotalProfitLoss(String userId) {
        return getTotalCurrentValue(userId) - getTotalInvested(userId);
    }
}