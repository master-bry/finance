package com.master.finance.service;

import com.master.finance.model.DailyEntry;
import com.master.finance.model.Transaction;
import com.master.finance.repository.DailyEntryRepository;
import com.master.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DailyEntryService {

    @Autowired
    private DailyEntryRepository dailyEntryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Get today's entry for the user. Creates a new one if none exists.
     */
    public DailyEntry getOrCreateTodayEntry(String userId) {
        return dailyEntryRepository.findTodayEntry(userId)
                .orElseGet(() -> {
                    DailyEntry newEntry = new DailyEntry();
                    newEntry.setUserId(userId);
                    newEntry.setDate(LocalDateTime.now());
                    newEntry.setOpeningBalance(getBalanceBeforeDate(userId, LocalDateTime.now()));
                    newEntry.calculateTotals();
                    return dailyEntryRepository.save(newEntry);
                });
    }

    /**
     * Get today's entry if exists.
     */
    public Optional<DailyEntry> getTodayEntry(String userId) {
        return dailyEntryRepository.findTodayEntry(userId);
    }

    /**
     * Save or update a daily entry.
     */
    public DailyEntry saveDailyEntry(DailyEntry entry, String userId) {
        if (entry.getId() == null || entry.getId().isEmpty()) {
            // New entry: opening balance = balance before the entry date
            entry.setOpeningBalance(getBalanceBeforeDate(userId, entry.getDate()));
        }
        entry.setUserId(userId);
        entry.calculateTotals();
        entry.setUpdatedAt(LocalDateTime.now());
        return dailyEntryRepository.save(entry);
    }

    /**
     * Get the current real balance from all transactions (income - expense).
     */
    public Double getCurrentBalance(String userId) {
        List<Transaction> allTransactions = transactionRepository.findByUserIdAndDeletedFalse(userId);
        double totalIncome = allTransactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        double totalExpense = allTransactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        return totalIncome - totalExpense;
    }

    /**
     * Get the balance at the start of a specific day (sum of all transactions before that day).
     */
    private Double getBalanceBeforeDate(String userId, LocalDateTime date) {
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<Transaction> transactionsBefore = transactionRepository
                .findByUserIdAndDateBeforeAndDeletedFalse(userId, startOfDay);
        double income = transactionsBefore.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        double expense = transactionsBefore.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        return income - expense;
    }

    /**
     * Get all entries for user (history).
     */
    public List<DailyEntry> getUserEntries(String userId) {
        return dailyEntryRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
    }

    /**
     * Get a single entry by ID.
     */
    public Optional<DailyEntry> getEntryById(String id) {
        return dailyEntryRepository.findById(id).filter(e -> !e.isDeleted());
    }

    /**
     * Soft delete an entry.
     */
    public void deleteEntry(String id) {
        dailyEntryRepository.findById(id).ifPresent(entry -> {
            entry.setDeleted(true);
            entry.setDeletedAt(LocalDateTime.now());
            dailyEntryRepository.save(entry);
        });
    }

    /**
     * Find the DailyEntry for a specific date.
     */
    public Optional<DailyEntry> getEntryByDate(String userId, LocalDateTime date) {
        LocalDateTime start = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        List<DailyEntry> entries = dailyEntryRepository.findByUserIdAndDateBetween(userId, start, end);
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(0));
    }

    /**
     * Sync a transaction change to the corresponding DailyEntry.
     */
    public void syncTransactionToDailyEntry(String userId, Transaction transaction, boolean isDeleted) {
        Optional<DailyEntry> entryOpt = getEntryByDate(userId, transaction.getDate());
        if (entryOpt.isEmpty()) return;

        DailyEntry entry = entryOpt.get();
        boolean updated = false;

        if ("EXPENSE".equals(transaction.getType())) {
            var iterator = entry.getExpenses().iterator();
            while (iterator.hasNext()) {
                DailyEntry.ExpenseItem item = iterator.next();
                if (item.getDescription().equals(transaction.getDescription()) &&
                    item.getAmount().equals(transaction.getAmount()) &&
                    item.getCategory().equals(transaction.getCategory())) {

                    if (isDeleted) {
                        iterator.remove();
                    } else {
                        item.setDescription(transaction.getDescription());
                        item.setAmount(transaction.getAmount());
                        item.setCategory(transaction.getCategory());
                    }
                    updated = true;
                    break;
                }
            }
        } else if ("INCOME".equals(transaction.getType())) {
            var iterator = entry.getIncomes().iterator();
            while (iterator.hasNext()) {
                DailyEntry.IncomeItem item = iterator.next();
                if (item.getDescription().equals(transaction.getDescription()) &&
                    item.getAmount().equals(transaction.getAmount()) &&
                    item.getSource().equals(transaction.getCategory())) {

                    if (isDeleted) {
                        iterator.remove();
                    } else {
                        item.setDescription(transaction.getDescription());
                        item.setAmount(transaction.getAmount());
                        item.setSource(transaction.getCategory());
                    }
                    updated = true;
                    break;
                }
            }
        }

        if (updated) {
            entry.calculateTotals();
            dailyEntryRepository.save(entry);
            // Recalculate balances for this and future days
            recalculateBalancesFromDate(userId, transaction.getDate());
        }
    }

    /**
     * Recalculate opening balances for all daily entries from a given date onward.
     */
    public void recalculateBalancesFromDate(String userId, LocalDateTime fromDate) {
        LocalDateTime startOfDay = fromDate.withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<DailyEntry> entries = dailyEntryRepository
                .findByUserIdAndDateGreaterThanEqualOrderByDateAsc(userId, startOfDay);

        Double runningBalance = getBalanceBeforeDate(userId, startOfDay);

        for (DailyEntry entry : entries) {
            entry.setOpeningBalance(runningBalance);
            entry.calculateTotals();
            runningBalance = entry.getClosingBalance();
            dailyEntryRepository.save(entry);
        }
    }

    /**
     * Process uploaded Excel file (implement as needed).
     */
    public void processExcelFile(MultipartFile file, String userId, Double openingBalance) {
        throw new UnsupportedOperationException("Implement Excel processing");
    }

    /**
     * Generate Excel template.
     */
    public byte[] generateExcelTemplate() {
        return new byte[0];
    }
}