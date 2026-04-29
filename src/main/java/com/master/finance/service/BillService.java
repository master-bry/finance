package com.master.finance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.master.finance.model.Bill;
import com.master.finance.repository.BillRepository;

@Service
public class BillService {

    @Autowired
    private BillRepository billRepository;

    public List<Bill> getUserBills(String userId) {
        List<Bill> bills = billRepository.findByUserIdAndDeletedFalse(userId);
        return bills != null ? bills : Collections.emptyList();
    }

    /**
     * Returns all bills with usable balance: status PENDING or PARTIAL.
     * Previously only returned PENDING — this caused PARTIAL bills to disappear
     * from the dropdown after a partial payment was made.
     */
    public List<Bill> getPendingBills(String userId) {
        List<Bill> bills = billRepository.findAvailableBills(userId);
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

    /**
     * Apply a partial or full payment from a prepaid bill.
     * Updates status to PARTIAL if balance remains, or PAID if fully used.
     * Does NOT create a Transaction — prepaid bills do not affect cash balance.
     */
    public Bill applyPayment(String billId, Double amount) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        if ("PAID".equals(bill.getStatus())) {
            throw new IllegalStateException("Bill is already fully used");
        }
        if (amount > bill.getAmount()) {
            throw new IllegalArgumentException("Insufficient prepaid balance");
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

    /**
     * Mark a bill as fully paid/used up manually.
     * Does NOT create a Transaction — prepaid bills do not affect cash balance.
     */
    public Bill markAsPaid(String billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        if ("PAID".equals(bill.getStatus())) {
            throw new IllegalStateException("Bill is already paid");
        }

        bill.setStatus("PAID");
        bill.setPaidAt(LocalDateTime.now());
        bill.setAmount(0.0);
        bill.setUpdatedAt(LocalDateTime.now());

        return billRepository.save(bill);
    }

    public void deleteBill(String id) {
        billRepository.findById(id).ifPresent(bill -> {
            bill.setDeleted(true);
            bill.setDeletedAt(LocalDateTime.now());
            billRepository.save(bill);
        });
    }
}