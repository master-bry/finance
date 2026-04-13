package com.master.finance.service;

import com.master.finance.model.DailyEntry;
import com.master.finance.model.Transaction;
import com.master.finance.repository.DailyEntryRepository;
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

    /**
     * Get today's entry for the user. Creates a new one if none exists.
     */
    public DailyEntry getOrCreateTodayEntry(String userId) {
        return dailyEntryRepository.findTodayEntry(userId)
                .orElseGet(() -> {
                    DailyEntry newEntry = new DailyEntry();
                    newEntry.setUserId(userId);
                    newEntry.setDate(LocalDateTime.now());
                    newEntry.setOpeningBalance(getCurrentBalance(userId));
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
            // New entry: set opening balance from previous day's closing
            Double previousClosing = getPreviousDayClosingBalance(userId);
            entry.setOpeningBalance(previousClosing);
        }
        entry.setUserId(userId);
        entry.calculateTotals();
        entry.setUpdatedAt(LocalDateTime.now());
        return dailyEntryRepository.save(entry);
    }

    /**
     * Get current balance (latest closing balance or 0).
     */
    public Double getCurrentBalance(String userId) {
        List<DailyEntry> entries = dailyEntryRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
        if (!entries.isEmpty()) {
            return entries.get(0).getClosingBalance();
        }
        return 0.0;
    }

    /**
     * Get closing balance of the most recent entry before today.
     */
    private Double getPreviousDayClosingBalance(String userId) {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<DailyEntry> allEntries = dailyEntryRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
        for (DailyEntry entry : allEntries) {
            if (entry.getDate().isBefore(todayStart)) {
                return entry.getClosingBalance();
            }
        }
        return 0.0;
    }

    /**
     * Get all entries for user (history).
     */
    public List<DailyEntry> getUserEntries(String userId) {
        return dailyEntryRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
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
     * Find the DailyEntry for a specific date (user's local date).
     */
    public Optional<DailyEntry> getEntryByDate(String userId, LocalDateTime date) {
        LocalDateTime start = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        List<DailyEntry> entries = dailyEntryRepository.findByUserIdAndDateBetween(userId, start, end);
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(0));
    }

    /**
     * Sync a transaction change (update/delete) to the corresponding DailyEntry.
     * Called from TransactionService after transaction is updated or deleted.
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
                // Match by description, amount, and category (and approximate time if needed)
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
        }
    }

    /**
     * Process uploaded Excel file (implement as needed).
     */
    public void processExcelFile(MultipartFile file, String userId, Double openingBalance) {
        // Implement Excel parsing and create DailyEntry + Transactions
        throw new UnsupportedOperationException("Implement Excel processing");
    }

    /**
     * Generate Excel template.
     */
    public byte[] generateExcelTemplate() {
        // Return byte array of template
        return new byte[0];
    }
}