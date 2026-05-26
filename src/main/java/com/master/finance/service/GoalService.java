package com.master.finance.service;

import com.master.finance.model.Goal;
import com.master.finance.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GoalService {
    
    @Autowired
    private GoalRepository goalRepository;
    
    public List<Goal> getUserGoals(String userId) {
        return goalRepository.findByUserIdAndDeletedFalse(userId);
    }
    
    public List<Goal> getActiveGoals(String userId) {
        return goalRepository.findByUserIdAndAchievedFalseAndDeletedFalse(userId);
    }
    
    public List<Goal> getCompletedGoals(String userId) {
        return goalRepository.findByUserIdAndDeletedFalse(userId)
                .stream()
                .filter(Goal::isAchieved)
                .toList();
    }
    
    public Optional<Goal> getGoal(String id) {
        return goalRepository.findById(id);
    }
    
    public Goal saveGoal(Goal goal) {
        goal.setDeleted(false);
        return goalRepository.save(goal);
    }
    
    // Soft delete - marks as deleted but keeps in database
    public void deleteGoal(String id) {
        goalRepository.findById(id).ifPresent(goal -> {
            goal.setDeleted(true);
            goal.setDeletedAt(LocalDateTime.now());
            goalRepository.save(goal);
        });
    }
    
    // Permanent delete - removes from database completely
    public void permanentDeleteGoal(String id) {
        goalRepository.deleteById(id);
    }
    
    public Goal markGoalComplete(String goalId) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        goal.setAchieved(true);
        goal.setAchievedDate(LocalDateTime.now());
        return goalRepository.save(goal);
    }
    
    public Goal updateProgress(String goalId, Double amount, String notes) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        
        Goal.DailyProgress progress = new Goal.DailyProgress();
        progress.setDate(LocalDateTime.now());
        progress.setAmount(amount);
        progress.setNotes(notes);
        progress.setMarkedComplete(true);
        goal.getDailyProgress().add(progress);
        
        goal.setCurrentAmount(goal.getCurrentAmount() + amount);
        
        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setAchieved(true);
            goal.setAchievedDate(LocalDateTime.now());
        }
        
        return goalRepository.save(goal);
    }
    
    public Goal addMilestone(String goalId, String name, Double targetAmount) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        
        Goal.Milestone milestone = new Goal.Milestone();
        milestone.setName(name);
        milestone.setTargetAmount(targetAmount);
        milestone.setCompleted(false);
        goal.getMilestones().add(milestone);
        
        return goalRepository.save(goal);
    }
    
    public Goal completeMilestone(String goalId, String milestoneName) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        
        goal.getMilestones().stream()
                .filter(m -> m.getName().equals(milestoneName))
                .findFirst()
                .ifPresent(milestone -> {
                    milestone.setCompleted(true);
                    milestone.setCompletedDate(LocalDateTime.now());
                });
        
        return goalRepository.save(goal);
    }
    
    public Map<String, Object> getGoalsSummary(String userId) {
        List<Goal> goals = getUserGoals(userId);
        
        double totalTarget = goals.stream().mapToDouble(Goal::getTargetAmount).sum();
        double totalProgress = goals.stream().mapToDouble(Goal::getCurrentAmount).sum();
        long achievedCount = goals.stream().filter(Goal::isAchieved).count();
        long activeCount = goals.stream().filter(g -> !g.isAchieved()).count();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalGoals", goals.size());
        summary.put("achievedGoals", achievedCount);
        summary.put("activeGoals", activeCount);
        summary.put("totalTarget", totalTarget);
        summary.put("totalProgress", totalProgress);
        summary.put("overallProgress", totalTarget > 0 ? (totalProgress / totalTarget) * 100 : 0);
        
        return summary;
    }
    
    public List<Goal> getHighPriorityGoals(String userId) {
        return goalRepository.findByUserIdAndPriorityAndDeletedFalse(userId, "HIGH");
    }
    
    public List<Goal> getGoalsByCategory(String userId, String category) {
        return getUserGoals(userId).stream()
                .filter(g -> category.equals(g.getCategory()))
                .toList();
    }

    public Map<String, Object> getSavingsProjection(String goalId) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("goalName", goal.getName());
        projection.put("targetAmount", goal.getTargetAmount());
        projection.put("currentAmount", goal.getCurrentAmount());
        projection.put("remainingAmount", goal.getRemainingAmount());
        projection.put("progressPercentage", goal.getProgressPercentage());

        if (goal.getTargetDate() != null) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
            projection.put("daysLeft", Math.max(0, daysLeft));

            double remaining = goal.getRemainingAmount();
            double dailyRate = daysLeft > 0 ? remaining / daysLeft : remaining;
            double monthlyRate = daysLeft > 0 ? remaining / (daysLeft / 30.0) : remaining;
            projection.put("recommendedDaily", Math.max(0, Math.round(dailyRate * 100.0) / 100.0));
            projection.put("recommendedMonthly", Math.max(0, Math.round(monthlyRate * 100.0) / 100.0));

            List<Map<String, Object>> forecast = new java.util.ArrayList<>();
            double projectedAmount = goal.getCurrentAmount();
            double avgDaily = 0;
            if (!goal.getDailyProgress().isEmpty()) {
                avgDaily = goal.getDailyProgress().stream()
                        .mapToDouble(Goal.DailyProgress::getAmount)
                        .average().orElse(0);
            }
            for (int i = 1; i <= Math.min(daysLeft, 12); i++) {
                projectedAmount += avgDaily > 0 ? avgDaily : dailyRate;
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("month", i);
                point.put("projectedAmount", Math.round(projectedAmount * 100.0) / 100.0);
                point.put("onTrack", projectedAmount >= goal.getTargetAmount());
                forecast.add(point);
            }
            projection.put("forecast", forecast);
        }
        return projection;
    }

    public List<Map<String, Object>> getAllSavingsProjections(String userId) {
        List<Goal> active = getActiveGoals(userId);
        return active.stream().map(g -> {
            Map<String, Object> proj = new LinkedHashMap<>();
            proj.put("id", g.getId());
            proj.put("name", g.getName());
            proj.put("targetAmount", g.getTargetAmount());
            proj.put("currentAmount", g.getCurrentAmount());
            proj.put("remainingAmount", g.getRemainingAmount());
            proj.put("progressPercentage", g.getProgressPercentage());
            if (g.getTargetDate() != null) {
                long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), g.getTargetDate());
                proj.put("daysLeft", Math.max(0, daysLeft));
                double remaining = g.getRemainingAmount();
                double monthlyRate = daysLeft > 0 ? remaining / (daysLeft / 30.0) : remaining;
                proj.put("recommendedMonthly", Math.max(0, Math.round(monthlyRate * 100.0) / 100.0));
            }
            return proj;
        }).toList();
    }
}