package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "recurring_transactions")
public class RecurringTransaction {
    @Id
    private String id;

    @Indexed
    private String userId;

    @NotBlank
    private String description;

    @NotNull
    private Double amount;

    @NotBlank
    private String type;

    @NotBlank
    private String category;

    private String frequency;
    private int intervalValue;
    private LocalDate nextDate;
    private LocalDate endDate;
    private boolean active;
    private String notes;
    private boolean deleted;
    private LocalDateTime createdAt;
    private int occurrencesGenerated;

    public RecurringTransaction() {
        this.active = true;
        this.intervalValue = 1;
        this.createdAt = LocalDateTime.now();
        this.occurrencesGenerated = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public int getIntervalValue() { return intervalValue; }
    public void setIntervalValue(int intervalValue) { this.intervalValue = intervalValue; }
    public LocalDate getNextDate() { return nextDate; }
    public void setNextDate(LocalDate nextDate) { this.nextDate = nextDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getOccurrencesGenerated() { return occurrencesGenerated; }
    public void setOccurrencesGenerated(int occurrencesGenerated) { this.occurrencesGenerated = occurrencesGenerated; }
}
