package com.master.finance.controller;

import com.master.finance.model.Goal;
import com.master.finance.service.GoalService;
import com.master.finance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        model.addAttribute("goals", goalService.getUserGoals(userId));
        model.addAttribute("activeGoals", goalService.getActiveGoals(userId));
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
        goalService.saveGoal(goal);
        redirectAttributes.addFlashAttribute("success", "Goal created successfully!");
        return "redirect:/goals";
    }
    
    @GetMapping("/mark/{id}")
    public String markGoalComplete(@PathVariable String id, RedirectAttributes redirectAttributes) {
        goalService.markGoalComplete(id);
        redirectAttributes.addFlashAttribute("success", "Goal marked as complete! Congratulations!");
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
                                 @RequestParam String notes,
                                 RedirectAttributes redirectAttributes) {
        goalService.updateProgress(id, amount, notes);
        redirectAttributes.addFlashAttribute("success", "Progress updated successfully!");
        return "redirect:/goals";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteGoal(@PathVariable String id, RedirectAttributes redirectAttributes) {
        goalService.deleteGoal(id);
        redirectAttributes.addFlashAttribute("success", "Goal deleted successfully!");
        return "redirect:/goals";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}