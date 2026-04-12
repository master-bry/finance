package com.master.finance.service;

import com.master.finance.model.Debt;
import com.master.finance.model.Investment;
import com.master.finance.model.Transaction;
import com.master.finance.repository.DebtRepository;
import com.master.finance.repository.InvestmentRepository;
import com.master.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private DebtRepository debtRepository;
    
    @Autowired
    private InvestmentRepository investmentRepository;
    
    // Generate monthly report
    public Map<String, Object> generateMonthlyReport(String userId, int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1);
        
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, startDate, endDate);
        
        double totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        double totalExpense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        
        double balance = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (balance / totalIncome) * 100 : 0;
        
        Map<String, Object> report = new HashMap<>();
        report.put("totalIncome", totalIncome);
        report.put("totalExpense", totalExpense);
        report.put("balance", balance);
        report.put("savingsRate", savingsRate);
        report.put("transactionCount", transactions.size());
        
        return report;
    }
    
    // Generate debt report
    public Map<String, Object> generateDebtReport(String userId) {
        List<Debt> debts = debtRepository.findByUserIdAndDeletedFalse(userId);
        
        double totalOwedToMe = debts.stream()
                .filter(d -> "OWED_TO_ME".equals(d.getType()))
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
        
        double totalIOwe = debts.stream()
                .filter(d -> "I_OWE".equals(d.getType()))
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount)
                .sum();
        
        Map<String, Object> report = new HashMap<>();
        report.put("totalOwedToMe", totalOwedToMe);
        report.put("totalIOwe", totalIOwe);
        report.put("netPosition", totalOwedToMe - totalIOwe);
        
        return report;
    }
    
    // Generate investment report
    public Map<String, Object> generateInvestmentReport(String userId) {
        List<Investment> investments = investmentRepository.findByUserIdAndDeletedFalse(userId);
        
        double totalInvested = investments.stream().mapToDouble(Investment::getAmountInvested).sum();
        double totalCurrentValue = investments.stream().mapToDouble(Investment::getCurrentValue).sum();
        double totalProfitLoss = totalCurrentValue - totalInvested;
        
        Map<String, Object> report = new HashMap<>();
        report.put("totalInvested", totalInvested);
        report.put("totalCurrentValue", totalCurrentValue);
        report.put("totalProfitLoss", totalProfitLoss);
        
        return report;
    }
}