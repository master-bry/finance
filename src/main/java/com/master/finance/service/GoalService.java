package com.master.finance.service;

import com.master.finance.model.Goal;
import com.master.finance.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
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
    
    public Optional<Goal> getGoal(String id) {
        return goalRepository.findById(id);
    }
    
    public Goal saveGoal(Goal goal) {
        goal.setDeleted(false);
        return goalRepository.save(goal);
    }
    
    public void deleteGoal(String id) {
        goalRepository.findById(id).ifPresent(goal -> {
            goal.setDeleted(true);
            goal.setDeletedAt(LocalDateTime.now());
            goalRepository.save(goal);
        });
    }
    
    public Goal markGoalComplete(String goalId) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        goal.setAchieved(true);
        goal.setAchievedDate(LocalDateTime.now());
        return goalRepository.save(goal);
    }
    
    public Goal updateProgress(String goalId, Double amount) {
        Goal goal = goalRepository.findById(goalId).orElseThrow();
        goal.setCurrentAmount(goal.getCurrentAmount() + amount);
        
        if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
            goal.setAchieved(true);
            goal.setAchievedDate(LocalDateTime.now());
        }
        return goalRepository.save(goal);
    }
}