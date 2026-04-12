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

@Controller
@RequestMapping("/goals")
public class GoalController {

    @Autowired
    private GoalService goalService;

    @Autowired
    private UserService userService;

    // ─── LIST ────────────────────────────────────────────────────────────────

    @GetMapping
    public String listGoals(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        model.addAttribute("goals",         goalService.getUserGoals(userId));
        model.addAttribute("activeGoals",   goalService.getActiveGoals(userId));
        model.addAttribute("completedGoals",goalService.getCompletedGoals(userId)); // ← WAS MISSING
        model.addAttribute("summary",       goalService.getGoalsSummary(userId));
        model.addAttribute("currentPage",   "goals");
        model.addAttribute("pageSubtitle",  "Track your financial goals");
        return "goals/index";
    }

    // ─── ADD ─────────────────────────────────────────────────────────────────

    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("goal")) {
            model.addAttribute("goal", new Goal());
        }
        model.addAttribute("currentPage", "goals");
        return "goals/add";
    }

    @PostMapping("/add")
    public String addGoal(@ModelAttribute Goal goal,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        if (goal.getName() == null || goal.getName().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Goal name is required.");
            return "redirect:/goals/add";
        }
        if (goal.getTargetAmount() == null || goal.getTargetAmount() <= 0) {
            redirectAttributes.addFlashAttribute("error", "A positive target amount is required.");
            return "redirect:/goals/add";
        }
        String userId = getUserId(authentication);
        goal.setUserId(userId);
        goalService.saveGoal(goal);
        redirectAttributes.addFlashAttribute("success", "Goal created successfully!");
        return "redirect:/goals";
    }

    // ─── EDIT ────────────────────────────────────────────────────────────────

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        boolean found = goalService.getGoal(id)
                .filter(g -> userId.equals(g.getUserId()))
                .map(g -> { model.addAttribute("goal", g); return true; })
                .orElse(false);

        if (!found) {
            redirectAttributes.addFlashAttribute("error", "Goal not found.");
            return "redirect:/goals";
        }
        model.addAttribute("currentPage", "goals");
        return "goals/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateGoal(@PathVariable String id,
                             @ModelAttribute Goal goal,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        goal.setId(id);
        goal.setUserId(userId);
        goalService.saveGoal(goal);
        redirectAttributes.addFlashAttribute("success", "Goal updated successfully!");
        return "redirect:/goals";
    }

    // ─── MARK COMPLETE ───────────────────────────────────────────────────────

    @GetMapping("/mark/{id}")
    public String markGoalComplete(@PathVariable String id,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        goalService.getGoal(id)
                .filter(g -> userId.equals(g.getUserId()))
                .ifPresent(g -> goalService.markGoalComplete(g.getId()));
        redirectAttributes.addFlashAttribute("success", "Goal marked as complete! Congratulations! 🎉");
        return "redirect:/goals";
    }

    // ─── PROGRESS ────────────────────────────────────────────────────────────

    @GetMapping("/progress/{id}")
    public String showProgressForm(@PathVariable String id,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        boolean found = goalService.getGoal(id)
                .filter(g -> userId.equals(g.getUserId()))
                .map(g -> { model.addAttribute("goal", g); return true; })
                .orElse(false);

        if (!found) {
            redirectAttributes.addFlashAttribute("error", "Goal not found.");
            return "redirect:/goals";
        }
        return "goals/progress";
    }

    @PostMapping("/progress/{id}")
    public String updateProgress(@PathVariable String id,
                                 @RequestParam Double amount,
                                 @RequestParam(required = false, defaultValue = "") String notes,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        goalService.getGoal(id)
                .filter(g -> userId.equals(g.getUserId()))
                .ifPresent(g -> goalService.updateProgress(g.getId(), amount, notes));
        redirectAttributes.addFlashAttribute("success", "Progress updated successfully!");
        return "redirect:/goals";
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    @GetMapping("/delete/{id}")
    public String deleteGoal(@PathVariable String id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        goalService.getGoal(id)
                .filter(g -> userId.equals(g.getUserId()))
                .ifPresent(g -> goalService.deleteGoal(g.getId()));
        redirectAttributes.addFlashAttribute("success", "Goal deleted successfully!");
        return "redirect:/goals";
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}