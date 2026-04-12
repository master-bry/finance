package com.master.finance.controller;

import com.master.finance.model.Investment;
import com.master.finance.repository.InvestmentRepository;
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
@RequestMapping("/investments")
public class InvestmentController {
    
    @Autowired
    private InvestmentRepository investmentRepository;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String listInvestments(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        List<Investment> investments = investmentRepository.findByUserIdAndDeletedFalse(userId);
        
        double totalInvested = investments.stream().mapToDouble(Investment::getAmountInvested).sum();
        double totalCurrentValue = investments.stream().mapToDouble(Investment::getCurrentValue).sum();
        double totalProfitLoss = totalCurrentValue - totalInvested;
        
        model.addAttribute("investments", investments);
        model.addAttribute("totalInvested", totalInvested);
        model.addAttribute("totalCurrentValue", totalCurrentValue);
        model.addAttribute("totalProfitLoss", totalProfitLoss);
        
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
        investment.setStartDate(LocalDateTime.now());
        investment.setStatus("ACTIVE");
        investment.setDeleted(false);
        
        if (investment.getCurrentValue() == null) {
            investment.setCurrentValue(investment.getAmountInvested());
        }
        
        investmentRepository.save(investment);
        redirectAttributes.addFlashAttribute("success", "Investment added successfully!");
        return "redirect:/investments";
    }
    
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        investmentRepository.findById(id).ifPresent(investment -> {
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
        investmentRepository.save(investment);
        redirectAttributes.addFlashAttribute("success", "Investment updated successfully!");
        return "redirect:/investments";
    }
    
    @GetMapping("/delete/{id}")
    public String deleteInvestment(@PathVariable String id, RedirectAttributes redirectAttributes) {
        investmentRepository.findById(id).ifPresent(investment -> {
            investment.setDeleted(true);
            investment.setDeletedAt(LocalDateTime.now());
            investmentRepository.save(investment);
        });
        redirectAttributes.addFlashAttribute("success", "Investment deleted successfully!");
        return "redirect:/investments";
    }
    
    @GetMapping("/update-value/{id}")
    public String showUpdateValueForm(@PathVariable String id, Model model) {
        investmentRepository.findById(id).ifPresent(investment -> model.addAttribute("investment", investment));
        return "investments/update-value";
    }
    
    @PostMapping("/update-value/{id}")
    public String updateValue(@PathVariable String id,
                              @RequestParam Double currentValue,
                              RedirectAttributes redirectAttributes) {
        investmentRepository.findById(id).ifPresent(investment -> {
            investment.setCurrentValue(currentValue);
            investmentRepository.save(investment);
        });
        redirectAttributes.addFlashAttribute("success", "Investment value updated!");
        return "redirect:/investments";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}