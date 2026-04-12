package com.master.finance.controller;

import com.master.finance.model.Investment;
import com.master.finance.service.InvestmentService;
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
@RequestMapping("/investments")
public class InvestmentController {
    
    @Autowired
    private InvestmentService investmentService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listInvestments(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        model.addAttribute("investments", investmentService.getUserInvestments(userId));
        model.addAttribute("totalInvested", investmentService.getTotalInvested(userId));
        model.addAttribute("totalCurrentValue", investmentService.getTotalCurrentValue(userId));
        model.addAttribute("totalProfitLoss", investmentService.getTotalProfitLoss(userId));
        return "investments/index";
    }
    
    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("investment")) {
            model.addAttribute("investment", new Investment());
        }
        return "investments/add";
    }
    
    @PostMapping("/add")
    public String addInvestment(@Valid @ModelAttribute Investment investment,
                                BindingResult result,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.investment", result);
            redirectAttributes.addFlashAttribute("investment", investment);
            return "redirect:/investments/add";
        }
        
        String userId = getUserId(authentication);
        investment.setUserId(userId);
        investmentService.saveInvestment(investment);
        redirectAttributes.addFlashAttribute("success", "Investment added successfully!");
        return "redirect:/investments";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        investmentService.getInvestment(id).ifPresent(investment -> {
            if (investment.getUserId().equals(userId)) {
                model.addAttribute("investment", investment);
            }
        });
        return "investments/edit";
    }
    
    @PostMapping("/edit/{id}")
    public String updateInvestment(@PathVariable String id,
                                   @Valid @ModelAttribute Investment investment,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        investment.setId(id);
        investment.setUserId(userId);
        investmentService.saveInvestment(investment);
        redirectAttributes.addFlashAttribute("success", "Investment updated successfully!");
        return "redirect:/investments";
    }
    
    @GetMapping("/delete/{id}")
    public String softDeleteInvestment(@PathVariable String id, RedirectAttributes redirectAttributes) {
        investmentService.softDeleteInvestment(id);
        redirectAttributes.addFlashAttribute("success", "Investment deleted successfully!");
        return "redirect:/investments";
    }
    
    @GetMapping("/update-value/{id}")
    public String showUpdateValueForm(@PathVariable String id, Model model) {
        investmentService.getInvestment(id).ifPresent(investment -> model.addAttribute("investment", investment));
        return "investments/update-value";
    }
    
    @PostMapping("/update-value/{id}")
    public String updateValue(@PathVariable String id,
                              @RequestParam Double currentValue,
                              RedirectAttributes redirectAttributes) {
        investmentService.updateCurrentValue(id, currentValue);
        redirectAttributes.addFlashAttribute("success", "Investment value updated successfully!");
        return "redirect:/investments";
    }
    
    @GetMapping("/add-transaction/{id}")
    public String showAddTransactionForm(@PathVariable String id, Model model) {
        investmentService.getInvestment(id).ifPresent(investment -> model.addAttribute("investment", investment));
        return "investments/add-transaction";
    }
    
    @PostMapping("/add-transaction/{id}")
    public String addTransaction(@PathVariable String id,
                                 @RequestParam String type,
                                 @RequestParam Double amount,
                                 @RequestParam String description,
                                 RedirectAttributes redirectAttributes) {
        investmentService.addTransaction(id, type, amount, description);
        redirectAttributes.addFlashAttribute("success", "Transaction added successfully!");
        return "redirect:/investments";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}