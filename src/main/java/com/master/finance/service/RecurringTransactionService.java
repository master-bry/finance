package com.master.finance.service;

import com.master.finance.model.RecurringTransaction;
import com.master.finance.model.Transaction;
import com.master.finance.repository.RecurringTransactionRepository;
import com.master.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class RecurringTransactionService {

    @Autowired
    private RecurringTransactionRepository recurringRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public RecurringTransaction create(RecurringTransaction rt) {
        return recurringRepository.save(rt);
    }

    public Optional<RecurringTransaction> getById(String id) {
        return recurringRepository.findById(id);
    }

    public List<RecurringTransaction> getUserRecurringTransactions(String userId) {
        return recurringRepository.findByUserIdAndDeletedFalse(userId);
    }

    public RecurringTransaction update(String id, RecurringTransaction updated) {
        return recurringRepository.findById(id).map(rt -> {
            rt.setDescription(updated.getDescription());
            rt.setAmount(updated.getAmount());
            rt.setType(updated.getType());
            rt.setCategory(updated.getCategory());
            rt.setFrequency(updated.getFrequency());
            rt.setIntervalValue(updated.getIntervalValue());
            rt.setNextDate(updated.getNextDate());
            rt.setEndDate(updated.getEndDate());
            rt.setNotes(updated.getNotes());
            rt.setActive(updated.isActive());
            return recurringRepository.save(rt);
        }).orElseThrow(() -> new RuntimeException("Recurring transaction not found"));
    }

    public void delete(String id) {
        recurringRepository.findById(id).ifPresent(rt -> {
            rt.setDeleted(true);
            recurringRepository.save(rt);
        });
    }

    public void toggleActive(String id) {
        recurringRepository.findById(id).ifPresent(rt -> {
            rt.setActive(!rt.isActive());
            recurringRepository.save(rt);
        });
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void processRecurringTransactions() {
        List<RecurringTransaction> due = recurringRepository
                .findByActiveTrueAndDeletedFalseAndNextDateLessThanEqual(LocalDate.now());

        for (RecurringTransaction rt : due) {
            Transaction tx = new Transaction();
            tx.setUserId(rt.getUserId());
            tx.setDescription(rt.getDescription());
            tx.setAmount(rt.getAmount());
            tx.setType(rt.getType());
            tx.setCategory(rt.getCategory());
            tx.setDate(LocalDateTime.now());
            tx.setNotes("Auto-generated from recurring: " + rt.getId());
            transactionRepository.save(tx);

            LocalDate next = calculateNextDate(rt);
            rt.setNextDate(next);
            rt.setOccurrencesGenerated(rt.getOccurrencesGenerated() + 1);

            if (rt.getEndDate() != null && next.isAfter(rt.getEndDate())) {
                rt.setActive(false);
            }
            recurringRepository.save(rt);
        }
    }

    private LocalDate calculateNextDate(RecurringTransaction rt) {
        LocalDate current = rt.getNextDate();
        return switch (rt.getFrequency()) {
            case "DAILY" -> current.plusDays(rt.getIntervalValue());
            case "WEEKLY" -> current.plusWeeks(rt.getIntervalValue());
            case "MONTHLY" -> current.plusMonths(rt.getIntervalValue());
            case "YEARLY" -> current.plusYears(rt.getIntervalValue());
            default -> current.plusMonths(1);
        };
    }
}
