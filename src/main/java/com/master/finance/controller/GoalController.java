package com.master.finance.controller;

import com.master.finance.model.Goal;
import com.master.finance.repository.GoalRepository;
import com.master.finance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/goals")
public class GoalController {
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listGoals(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        List<Goal> goals = goalRepository.findByUserIdAndDeletedFalse(userId);
        
        model.addAttribute("goals", goals);
        model.addAttribute("activeGoals", goals.stream().filter(g -> !g.isAchieved()).toList());
        model.addAttribute("completedGoals", goals.stream().filter(Goal::isAchieved).toList());
        
        return "goals/index";
    }
    
    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("goal")) {
            model.addAttribute("goal", new Goal());
        }
        return "goals/add";
    }
    
    @PostMapping("/add")
    public String addGoal(@Valid @ModelAttribute Goal goal,
                          BindingResult result,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.goal", result);
            redirectAttributes.addFlashAttribute("goal", goal);
            return "redirect:/goals/add";
        }
        
        String userId = getUserId(authentication);
        goal.setUserId(userId);
        goal.setAchieved(false);
        goal.setDeleted(false);
        goal.setCurrentAmount(0.0);
        
        goalRepository.save(goal);
        redirectAttributes.addFlashAttribute("success", "Goal created successfully!");
        return "redirect:/goals";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        goalRepository.findById(id).ifPresent(goal -> {
            if (goal.getUserId().equals(userId)) {
                model.addAttribute("goal", goal);
            }
        });
        return "goals/edit";
    }
    
    @PostMapping("/edit/{id}")
    public String updateGoal(@PathVariable String id,
                             @Valid @ModelAttribute Goal goal,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        goal.setId(id);
        goal.setUserId(userId);
        goalRepository.save(goal);
        redirectAttributes.addFlashAttribute("success", "Goal updated successfully!");
        return "redirect:/goals";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteGoal(@PathVariable String id, RedirectAttributes redirectAttributes) {
        goalRepository.findById(id).ifPresent(goal -> {
            goal.setDeleted(true);
            goal.setDeletedAt(LocalDateTime.now());
            goalRepository.save(goal);
        });
        redirectAttributes.addFlashAttribute("success", "Goal deleted successfully!");
        return "redirect:/goals";
    }
    
    @GetMapping("/mark/{id}")
    public String markComplete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        goalRepository.findById(id).ifPresent(goal -> {
            goal.setAchieved(true);
            goal.setAchievedDate(LocalDateTime.now());
            goalRepository.save(goal);
        });
        redirectAttributes.addFlashAttribute("success", "🎉 Congratulations! Goal achieved!");
        return "redirect:/goals";
    }
    
    @GetMapping("/progress/{id}")
    public String showProgressForm(@PathVariable String id, Model model) {
        goalRepository.findById(id).ifPresent(goal -> model.addAttribute("goal", goal));
        return "goals/progress";
    }
    
    @PostMapping("/progress/{id}")
    public String updateProgress(@PathVariable String id,
                                 @RequestParam Double amount,
                                 @RequestParam String notes,
                                 RedirectAttributes redirectAttributes) {
        goalRepository.findById(id).ifPresent(goal -> {
            goal.setCurrentAmount(goal.getCurrentAmount() + amount);
            
            Goal.DailyProgress progress = new Goal.DailyProgress();
            progress.setDate(LocalDateTime.now());
            progress.setAmount(amount);
            progress.setNotes(notes);
            progress.setMarkedComplete(true);
            goal.getDailyProgress().add(progress);
            
            if (goal.getCurrentAmount() >= goal.getTargetAmount()) {
                goal.setAchieved(true);
                goal.setAchievedDate(LocalDateTime.now());
            }
            
            goalRepository.save(goal);
        });
        redirectAttributes.addFlashAttribute("success", "Progress updated!");
        return "redirect:/goals";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}