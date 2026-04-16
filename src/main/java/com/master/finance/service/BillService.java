package com.master.finance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.master.finance.model.Bill;
import com.master.finance.model.Transaction;
import com.master.finance.repository.BillRepository;

@Service
public class BillService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private DailyEntryService dailyEntryService;

    public List<Bill> getUserBills(String userId) {
        List<Bill> bills = billRepository.findByUserIdAndDeletedFalse(userId);
        return bills != null ? bills : Collections.emptyList();
    }

    public List<Bill> getPendingBills(String userId) {
        List<Bill> bills = billRepository.findByUserIdAndStatus(userId, "PENDING");
        return bills != null ? bills : Collections.emptyList();
    }

    public List<Bill> getOverdueBills(String userId) {
        List<Bill> bills = billRepository.findOverdueBills(userId, LocalDate.now());
        return bills != null ? bills : Collections.emptyList();
    }

    public Optional<Bill> getBill(String id) {
        return billRepository.findById(id).filter(b -> !b.isDeleted());
    }

    public Bill saveBill(Bill bill) {
        if (bill.getCreatedAt() == null) {
            bill.setCreatedAt(LocalDateTime.now());
        }
        bill.setUpdatedAt(LocalDateTime.now());
        return billRepository.save(bill);
    }

    public Bill markAsPaid(String billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        if ("PAID".equals(bill.getStatus())) {
            throw new IllegalStateException("Bill is already paid");
        }

        bill.setStatus("PAID");
        bill.setPaidAt(LocalDateTime.now());
        bill.setUpdatedAt(LocalDateTime.now());

        // Create expense transaction
        Transaction tx = new Transaction();
        tx.setUserId(bill.getUserId());
        tx.setDescription("Bill Payment: " + bill.getName());
        tx.setAmount(bill.getAmount());
        tx.setType("EXPENSE");
        tx.setCategory(bill.getCategory() != null ? bill.getCategory() : "Bills");
        tx.setDate(LocalDateTime.now());
        tx.setDeleted(false);
        transactionService.saveTransaction(tx);

        // Update daily entry
        dailyEntryService.getOrCreateTodayEntry(bill.getUserId());
        dailyEntryService.recalculateBalancesFromDate(bill.getUserId(), LocalDateTime.now());

        // Handle recurring
        if (bill.isRecurring() && bill.getFrequency() != null && !bill.getFrequency().isEmpty()) {
            Bill nextBill = new Bill();
            nextBill.setUserId(bill.getUserId());
            nextBill.setName(bill.getName());
            nextBill.setAmount(bill.getAmount());
            nextBill.setCategory(bill.getCategory());
            nextBill.setRecurring(true);
            nextBill.setFrequency(bill.getFrequency());
            nextBill.setNotes(bill.getNotes());
            nextBill.setStatus("PENDING");
            LocalDate nextDue = bill.getDueDate();
            if (nextDue != null) {
                switch (bill.getFrequency()) {
                    case "MONTHLY": nextDue = nextDue.plusMonths(1); break;
                    case "WEEKLY":  nextDue = nextDue.plusWeeks(1); break;
                    case "YEARLY":  nextDue = nextDue.plusYears(1); break;
                }
                nextBill.setDueDate(nextDue);
            }
            billRepository.save(nextBill);
        }

        return billRepository.save(bill);
    }

    public void deleteBill(String id) {
        billRepository.findById(id).ifPresent(bill -> {
            bill.setDeleted(true);
            bill.setDeletedAt(LocalDateTime.now());
            billRepository.save(bill);
        });
    }

    public Bill applyPayment(String billId, Double amount) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        if (!"PENDING".equals(bill.getStatus()) && !"PARTIAL".equals(bill.getStatus())) {
            throw new IllegalStateException("Bill is already paid");
        }
        if (amount > bill.getAmount()) {
            throw new IllegalArgumentException("Payment exceeds bill amount");
        }

        double newAmount = bill.getAmount() - amount;
        if (newAmount <= 0.01) {
            bill.setStatus("PAID");
            bill.setPaidAt(LocalDateTime.now());
            bill.setAmount(0.0);
        } else {
            bill.setStatus("PARTIAL");
            bill.setAmount(newAmount);
        }
        bill.setUpdatedAt(LocalDateTime.now());
        return billRepository.save(bill);
    }
}