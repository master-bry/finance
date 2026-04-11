package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String userId;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;
    
    @NotBlank(message = "Type is required")
    private String type; // INCOME or EXPENSE
    
    @NotBlank(message = "Category is required")
    private String category;
    
    private LocalDateTime date;
    private String notes;
    
    public Transaction() {
        this.date = LocalDateTime.now();
    }
    
    // Getters and Setters
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
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getFormattedDate() {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}