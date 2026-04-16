package com.master.finance.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.master.finance.model.Investment;
import com.master.finance.repository.InvestmentRepository;

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
        return investmentRepository.save(investment);
    }
    
    public void softDeleteInvestment(String id) {
        investmentRepository.findById(id).ifPresent(investment -> {
            investment.setDeleted(true);
            investment.setDeletedAt(LocalDate.now());
            investmentRepository.save(investment);
        });
    }
    
    public void permanentDeleteInvestment(String id) {
        investmentRepository.deleteById(id);
    }
    
    public Investment updateCurrentValue(String id, Double newValue) {
        Investment investment = investmentRepository.findById(id).orElseThrow();
        investment.setCurrentValue(newValue);
        return investmentRepository.save(investment);
    }
    
    public Investment addTransaction(String id, String type, Double amount, String description) {
        Investment investment = investmentRepository.findById(id).orElseThrow();
        
        Investment.InvestmentTransaction transaction = new Investment.InvestmentTransaction();
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        investment.getTransactions().add(transaction);
        
        if ("DEPOSIT".equals(type)) {
            investment.setAmountInvested(investment.getAmountInvested() + amount);
            investment.setCurrentValue(investment.getCurrentValue() + amount);
        } else if ("WITHDRAWAL".equals(type)) {
            investment.setAmountInvested(investment.getAmountInvested() - amount);
            investment.setCurrentValue(investment.getCurrentValue() - amount);
        } else if ("INTEREST".equals(type) || "DIVIDEND".equals(type)) {
            investment.setCurrentValue(investment.getCurrentValue() + amount);
        }
        
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