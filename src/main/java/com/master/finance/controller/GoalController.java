package com.master.finance.controller;

import com.master.finance.model.Goal;
import com.master.finance.service.GoalService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/goals")
public class GoalController {
    
    @Autowired
    private GoalService goalService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listGoals(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        List<Goal> goals = goalService.getUserGoals(userId);
        List<Goal> activeGoals = goalService.getActiveGoals(userId);
        
        model.addAttribute("goals", goals);
        model.addAttribute("activeGoals", activeGoals);
        model.addAttribute("completedGoals", goals.stream().filter(Goal::isAchieved).toList());
        
        return "goals/index";
    }
    
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("goal", new Goal());
        return "goals/add";
    }
    
    @PostMapping("/add")
    public String addGoal(@RequestParam String name,
                          @RequestParam String category,
                          @RequestParam Double targetAmount,
                          @RequestParam(required = false) String targetDate,
                          @RequestParam(required = false, defaultValue = "MEDIUM") String priority,
                          @RequestParam(required = false) String description,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            
            Goal goal = new Goal();
            goal.setUserId(userId);
            goal.setName(name);
            goal.setCategory(category);
            goal.setTargetAmount(targetAmount);
            goal.setCurrentAmount(0.0);
            goal.setPriority(priority);
            goal.setDescription(description);
            goal.setAchieved(false);
            
            if (targetDate != null && !targetDate.isEmpty()) {
                goal.setTargetDate(LocalDateTime.parse(targetDate + "T00:00:00"));
            }
            
            goalService.saveGoal(goal);
            redirectAttributes.addFlashAttribute("success", "Goal created successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating goal: " + e.getMessage());
        }
        
        return "redirect:/goals";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        goalService.getGoal(id).ifPresent(goal -> {
            if (goal.getUserId().equals(userId)) {
                model.addAttribute("goal", goal);
            }
        });
        
        if (!model.containsAttribute("goal")) {
            return "redirect:/goals?error=Goal not found";
        }
        
        return "goals/edit";
    }
    
    @PostMapping("/edit/{id}")
    public String updateGoal(@PathVariable String id,
                             @RequestParam String name,
                             @RequestParam String category,
                             @RequestParam Double targetAmount,
                             @RequestParam(required = false) String targetDate,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) Boolean achieved,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            Goal existingGoal = goalService.getGoal(id).orElse(null);
            
            if (existingGoal == null || !existingGoal.getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Goal not found");
                return "redirect:/goals";
            }
            
            existingGoal.setName(name);
            existingGoal.setCategory(category);
            existingGoal.setTargetAmount(targetAmount);
            existingGoal.setPriority(priority != null ? priority : "MEDIUM");
            existingGoal.setDescription(description);
            
            if (achieved != null && achieved) {
                existingGoal.setAchieved(true);
                existingGoal.setAchievedDate(LocalDateTime.now());
            }
            
            if (targetDate != null && !targetDate.isEmpty()) {
                existingGoal.setTargetDate(LocalDateTime.parse(targetDate + "T00:00:00"));
            }
            
            goalService.saveGoal(existingGoal);
            redirectAttributes.addFlashAttribute("success", "Goal updated successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating goal: " + e.getMessage());
        }
        
        return "redirect:/goals";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteGoal(@PathVariable String id, RedirectAttributes redirectAttributes) {
        goalService.deleteGoal(id);
        redirectAttributes.addFlashAttribute("success", "Goal deleted successfully!");
        return "redirect:/goals";
    }
    
    @GetMapping("/mark/{id}")
    public String markComplete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        goalService.markGoalComplete(id);
        redirectAttributes.addFlashAttribute("success", "🎉 Congratulations! Goal achieved!");
        return "redirect:/goals";
    }
    
    @GetMapping("/progress/{id}")
    public String showProgressForm(@PathVariable String id, Model model) {
        goalService.getGoal(id).ifPresent(goal -> model.addAttribute("goal", goal));
        return "goals/progress";
    }
    
    @PostMapping("/progress/{id}")
    public String updateProgress(@PathVariable String id,
                                 @RequestParam Double amount,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (amount == null || amount <= 0) {
                redirectAttributes.addFlashAttribute("error", "Please enter a valid amount");
                return "redirect:/goals/progress/" + id;
            }
            goalService.updateProgress(id, amount);
            redirectAttributes.addFlashAttribute("success", "Progress updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating progress: " + e.getMessage());
        }
        return "redirect:/goals";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}