package com.master.finance.service;

import com.master.finance.model.Bill;
import com.master.finance.repository.BillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BillService {

    @Autowired
    private BillRepository billRepository;

    public List<Bill> getPendingBills(String userId) {
        return billRepository.findByUserIdAndStatus(userId, "PENDING");
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

    // Optional: method to add bills manually if needed (via a simple form)
    public Bill saveBill(Bill bill) {
        return billRepository.save(bill);
    }
}