package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "debts")
@CompoundIndex(name = "user_type_idx", def = "{'userId': 1, 'type': 1}")
@CompoundIndex(name = "user_type_status_idx", def = "{'userId': 1, 'type': 1, 'status': 1}")
public class Debt {
    @Id
    private String id;
    @Indexed
    private String userId;
    private String personName;
    private String type; // "OWED_TO_ME" or "I_OWE"
    private Double amount;
    private Double remainingAmount;
    private String status; // "PARTIAL", "PENDING", "SETTLED"
    private String description;

    // Use LocalDate + @DateTimeFormat so HTML <input type="date"> binds correctly
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private LocalDateTime dateGiven;
    private LocalDateTime lastUpdated;
    private List<PaymentRecord> paymentHistory = new ArrayList<>();
    private String phoneNumber;
    private String notes;

    // Soft delete fields
    private boolean deleted = false;
    private LocalDateTime deletedAt;

    // ── Inner class ──────────────────────────────────────────────────────────

    public static class PaymentRecord {
        private LocalDateTime paymentDate;
        private Double amountPaid;
        private String notes;

        public PaymentRecord() {
            this.paymentDate = LocalDateTime.now();
        }

        public LocalDateTime getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
        public Double getAmountPaid() { return amountPaid; }
        public void setAmountPaid(Double amountPaid) { this.amountPaid = amountPaid; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    public Debt() {
        this.dateGiven = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.status = "PENDING";
        this.paymentHistory = new ArrayList<>();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) {
        this.amount = amount;
        // Only initialise remainingAmount if not yet set (new debt)
        if (this.remainingAmount == null) {
            this.remainingAmount = amount;
        }
    }

    public Double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Double remainingAmount) { this.remainingAmount = remainingAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getDateGiven() { return dateGiven; }
    public void setDateGiven(LocalDateTime dateGiven) { this.dateGiven = dateGiven; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public List<PaymentRecord> getPaymentHistory() { return paymentHistory; }
    public void setPaymentHistory(List<PaymentRecord> paymentHistory) { this.paymentHistory = paymentHistory; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}