package com.master.finance.controller;

import com.master.finance.model.Investment;
import com.master.finance.service.InvestmentService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        List<Investment> investments = investmentService.getUserInvestments(userId);
        
        double totalInvested = investmentService.getTotalInvested(userId);
        double totalCurrentValue = investmentService.getTotalCurrentValue(userId);
        double totalProfitLoss = investmentService.getTotalProfitLoss(userId);
        
        model.addAttribute("investments", investments);
        model.addAttribute("totalInvested", totalInvested);
        model.addAttribute("totalCurrentValue", totalCurrentValue);
        model.addAttribute("totalProfitLoss", totalProfitLoss);
        
        return "investments/index";
    }
    
    @GetMapping("/add")
    public String showAddForm() {
        return "investments/add";
    }
    
    @PostMapping("/add")
    public String addInvestment(@RequestParam String name,
                                @RequestParam String type,
                                @RequestParam Double amountInvested,
                                @RequestParam(required = false) Double currentValue,
                                @RequestParam(required = false) Double expectedReturn,
                                @RequestParam(required = false) String riskLevel,
                                @RequestParam(required = false) String provider,
                                @RequestParam(required = false) String notes,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            
            Investment investment = new Investment();
            investment.setUserId(userId);
            investment.setName(name);
            investment.setType(type);
            investment.setAmountInvested(amountInvested);
            investment.setCurrentValue(currentValue != null ? currentValue : amountInvested);
            investment.setExpectedReturn(expectedReturn);
            investment.setRiskLevel(riskLevel != null ? riskLevel : "MEDIUM");
            investment.setProvider(provider);
            investment.setNotes(notes);
            investment.setStatus("ACTIVE");
            investment.setStartDate(LocalDateTime.now());
            
            investmentService.saveInvestment(investment);
            redirectAttributes.addFlashAttribute("success", "Investment added successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error saving investment: " + e.getMessage());
        }
        
        return "redirect:/investments";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteInvestment(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            investmentService.deleteInvestment(id);
            redirectAttributes.addFlashAttribute("success", "Investment deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting investment");
        }
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
        try {
            investmentService.updateCurrentValue(id, currentValue);
            redirectAttributes.addFlashAttribute("success", "Investment value updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating value");
        }
        return "redirect:/investments";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}