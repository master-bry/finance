package com.master.finance.service;

import com.master.finance.model.Transaction;
import com.master.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DailyEntryService dailyEntryService;

    /**
     * Get all transactions for a user, ordered by date descending (newest first)
     * @param userId the user ID
     * @return list of transactions
     */
    public List<Transaction> getUserTransactions(String userId) {
        return transactionRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
    }

    /**
     * Get a transaction by ID
     * @param id the transaction ID
     * @return Optional containing the transaction if found
     */
    public Optional<Transaction> getTransaction(String id) {
        return transactionRepository.findById(id);
    }

    /**
     * Save a new transaction
     * @param transaction the transaction to save
     * @return the saved transaction
     */
    public Transaction saveTransaction(Transaction transaction) {
        if (transaction.getDate() == null) {
            transaction.setDate(LocalDateTime.now());
        }
        transaction.setDeleted(false);
        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(Transaction transaction) {
        Transaction existing = transactionRepository.findById(transaction.getId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        transaction.setUserId(existing.getUserId());
        transaction.setDeleted(existing.isDeleted());
        transaction.setDeletedAt(existing.getDeletedAt());
        Transaction saved = transactionRepository.save(transaction);

        dailyEntryService.syncTransactionToDailyEntry(saved.getUserId(), saved, false);
        dailyEntryService.recalculateBalancesFromDate(saved.getUserId(), saved.getDate());
        return saved;
    }

    public void deleteTransaction(String id) {
        transactionRepository.findById(id).ifPresent(transaction -> {
            transaction.setDeleted(true);
            transaction.setDeletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            dailyEntryService.syncTransactionToDailyEntry(transaction.getUserId(), transaction, true);
            dailyEntryService.recalculateBalancesFromDate(transaction.getUserId(), transaction.getDate());
        });
    }

    public void permanentDeleteTransaction(String id) {
        transactionRepository.findById(id).ifPresent(transaction -> {
            dailyEntryService.syncTransactionToDailyEntry(transaction.getUserId(), transaction, true);
            dailyEntryService.recalculateBalancesFromDate(transaction.getUserId(), transaction.getDate());
            transactionRepository.deleteById(id);
        });
    }

    public List<Transaction> getTransactionsByDateRange(String userId, LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, start, end);
    }

    public Double getTotalIncome(String userId, LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, start, end);
        System.out.println("TransactionService.getTotalIncome: userId=" + userId + ", start=" + start + ", end=" + end + ", transactions found=" + transactions.size());
        return transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Double getTotalExpense(String userId, LocalDateTime start, LocalDateTime end) {
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, start, end);
        System.out.println("TransactionService.getTotalExpense: userId=" + userId + ", start=" + start + ", end=" + end + ", transactions found=" + transactions.size());
        return transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Map<String, Double> getExpenseByCategory(String userId, LocalDateTime start, LocalDateTime end) {
        Map<String, Double> expensesByCategory = new HashMap<>();
        transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, start, end)
                .stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .forEach(t -> expensesByCategory.merge(t.getCategory(), t.getAmount(), Double::sum));
        return expensesByCategory;
    }

    public Map<String, Double> getIncomeByCategory(String userId, LocalDateTime start, LocalDateTime end) {
        Map<String, Double> incomeByCategory = new HashMap<>();
        transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, start, end)
                .stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .forEach(t -> incomeByCategory.merge(t.getCategory(), t.getAmount(), Double::sum));
        return incomeByCategory;
    }

    public List<Transaction> getRecentTransactions(String userId, int limit) {
        return transactionRepository.findRecentTransactionsByUserId(userId)
                .stream()
                .limit(limit)
                .toList();
    }

    public Map<String, Object> getMonthlySummary(String userId, int year, int month) {
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

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("balance", totalIncome - totalExpense);
        summary.put("transactionCount", transactions.size());
        summary.put("transactions", transactions);
        return summary;
    }

    public List<Transaction> searchTransactions(String userId, String keyword) {
        return transactionRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId)
                .stream()
                .filter(t -> t.getDescription().toLowerCase().contains(keyword.toLowerCase()) ||
                            t.getCategory().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    public List<Transaction> filterByType(String userId, String type) {
        return transactionRepository.findByUserIdAndTypeAndDeletedFalse(userId, type);
    }

    public void saveAll(List<Transaction> transactions) {
        transactionRepository.saveAll(transactions);
    }
}