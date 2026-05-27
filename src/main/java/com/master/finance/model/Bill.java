package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "bills")
@CompoundIndex(name = "user_deleted_status_idx", def = "{'userId': 1, 'deleted': 1, 'status': 1}")
@CompoundIndex(name = "user_deleted_due_status_idx", def = "{'userId': 1, 'deleted': 1, 'dueDate': 1, 'status': 1}")
public class Bill {
    @Id
    private String id;
    @Indexed
    private String userId;
    private String name;
    private Double amount;
    private String category;
    @Indexed
    private String status; // PENDING, PAID, PARTIAL
    private boolean recurring;
    private String frequency; // MONTHLY, WEEKLY, YEARLY, NONE

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private LocalDateTime paidAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Indexed
    private boolean deleted = false;
    private LocalDateTime deletedAt;

    public Bill() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public boolean isOverdue() {
        return "PENDING".equals(status) && dueDate != null && dueDate.isBefore(LocalDate.now());
    }
}