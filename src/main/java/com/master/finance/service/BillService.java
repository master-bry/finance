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

    // Zote transactionService na dailyEntryService hazihitajiki kwa prepaid
    // Unaweza kuzifuta au kuziacha (situmii)

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

    // Kwa prepaid: hii itatumika wakati wa kulipa kutoka kwa expense form
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

        // Hakuna Transaction – prepaid haithiri cash balance
        return billRepository.save(bill);
    }

    // Kwa prepaid: markAsPaid haiumbi transaction pia
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

        // Hakuna transaction – prepaid haihusiki na cash balance
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