package com.master.finance.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "goals")
public class Goal {
    @Id
    private String id;
    private String userId;
    private String name;
    private String category;
    private Double targetAmount;
    private Double currentAmount;

    // LocalDate + @DateTimeFormat so HTML <input type="date"> binds correctly
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    private String priority;
    private boolean achieved;
    private LocalDateTime achievedDate;
    private String description;
    private List<Milestone> milestones   = new ArrayList<>();
    private List<DailyProgress> dailyProgress = new ArrayList<>();

    // Soft delete
    private boolean deleted = false;
    private LocalDateTime deletedAt;

    // ── Inner classes ────────────────────────────────────────────────────────

    public static class Milestone {
        private String name;
        private Double targetAmount;
        private boolean completed;
        private LocalDateTime completedDate;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getTargetAmount() { return targetAmount; }
        public void setTargetAmount(Double targetAmount) { this.targetAmount = targetAmount; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public LocalDateTime getCompletedDate() { return completedDate; }
        public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }
    }

    public static class DailyProgress {
        private LocalDateTime date;
        private Double amount;
        private String notes;
        private boolean markedComplete;

        public LocalDateTime getDate() { return date; }
        public void setDate(LocalDateTime date) { this.date = date; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public boolean isMarkedComplete() { return markedComplete; }
        public void setMarkedComplete(boolean markedComplete) { this.markedComplete = markedComplete; }
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    public Goal() {
        this.currentAmount = 0.0;
        this.achieved      = false;
        this.milestones    = new ArrayList<>();
        this.dailyProgress = new ArrayList<>();
    }

    // ── Computed helpers ─────────────────────────────────────────────────────

    public Double getRemainingAmount() {
        if (targetAmount == null) return 0.0;
        double remaining = targetAmount - (currentAmount != null ? currentAmount : 0.0);
        return remaining < 0 ? 0.0 : remaining;
    }

    public Double getProgressPercentage() {
        if (targetAmount == null || targetAmount == 0) return 0.0;
        double pct = ((currentAmount != null ? currentAmount : 0.0) / targetAmount) * 100;
        return pct > 100 ? 100.0 : pct;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(Double targetAmount) { this.targetAmount = targetAmount; }

    public Double getCurrentAmount() { return currentAmount != null ? currentAmount : 0.0; }
    public void setCurrentAmount(Double currentAmount) { this.currentAmount = currentAmount; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isAchieved() { return achieved; }
    public void setAchieved(boolean achieved) { this.achieved = achieved; }

    public LocalDateTime getAchievedDate() { return achievedDate; }
    public void setAchievedDate(LocalDateTime achievedDate) { this.achievedDate = achievedDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Milestone> getMilestones() { return milestones; }
    public void setMilestones(List<Milestone> milestones) { this.milestones = milestones; }

    public List<DailyProgress> getDailyProgress() { return dailyProgress; }
    public void setDailyProgress(List<DailyProgress> dailyProgress) { this.dailyProgress = dailyProgress; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}