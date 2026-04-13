package com.master.finance.service;

import com.master.finance.model.DailyEntry;
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
     * Process uploaded Excel file (implementation depends on your logic).
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