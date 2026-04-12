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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    // ─── MONTHLY REPORT ───────────────────────────────────────────────────────

    public Map<String, Object> generateMonthlyReport(String userId, int year, int month) {
        List<Transaction> transactions = getMonthTransactions(userId, year, month);

        double totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();

        double totalExpense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();

        double balance = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (balance / totalIncome) * 100 : 0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalIncome", totalIncome);
        report.put("totalExpense", totalExpense);
        report.put("balance", balance);
        report.put("savingsRate", savingsRate);
        report.put("transactionCount", transactions.size());
        return report;
    }

    // ─── TRANSACTIONS FOR REPORT PAGE ─────────────────────────────────────────

    /**
     * Returns all transactions for the given month, newest first.
     * Used by the report page to display the transaction table.
     */
    public List<Transaction> getRecentTransactions(String userId, int year, int month) {
        return getMonthTransactions(userId, year, month);
    }

    /**
     * Returns a map of { category -> total amount } for expenses in the given month.
     */
    public Map<String, Double> getExpenseByCategory(String userId, int year, int month) {
        return getMonthTransactions(userId, year, month).stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    // ─── DEBT REPORT ──────────────────────────────────────────────────────────

    public Map<String, Object> generateDebtReport(String userId) {
        List<Debt> debts = debtRepository.findByUserIdAndDeletedFalse(userId);

        double totalOwedToMe = debts.stream()
                .filter(d -> "OWED_TO_ME".equals(d.getType()))
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount).sum();

        double totalIOwe = debts.stream()
                .filter(d -> "I_OWE".equals(d.getType()))
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount).sum();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalOwedToMe", totalOwedToMe);
        report.put("totalIOwe", totalIOwe);
        report.put("netPosition", totalOwedToMe - totalIOwe);
        report.put("activeDebtsCount", debts.stream()
                .filter(d -> !"SETTLED".equals(d.getStatus())).count());
        return report;
    }

    // ─── INVESTMENT REPORT ────────────────────────────────────────────────────

    public Map<String, Object> generateInvestmentReport(String userId) {
        List<Investment> investments = investmentRepository.findByUserIdAndDeletedFalse(userId);

        double totalInvested     = investments.stream().mapToDouble(Investment::getAmountInvested).sum();
        double totalCurrentValue = investments.stream().mapToDouble(Investment::getCurrentValue).sum();
        double totalProfitLoss   = totalCurrentValue - totalInvested;
        double roiPercent        = totalInvested > 0 ? (totalProfitLoss / totalInvested) * 100 : 0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalInvested", totalInvested);
        report.put("totalCurrentValue", totalCurrentValue);
        report.put("totalProfitLoss", totalProfitLoss);
        report.put("roiPercent", roiPercent);
        report.put("investmentCount", investments.size());
        return report;
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private List<Transaction> getMonthTransactions(String userId, int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end   = start.plusMonths(1);
        return transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, start, end);
    }
}