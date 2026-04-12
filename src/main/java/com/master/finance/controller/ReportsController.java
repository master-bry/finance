package com.master.finance.controller;

import com.master.finance.service.ReportService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
public class ReportsController {
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private UserService userService;
    
    @GetMapping
    public String showReports(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        
        model.addAttribute("monthlyReport", reportService.generateMonthlyReport(userId, 2024, 4));
        model.addAttribute("debtReport", reportService.generateDebtReport(userId));
        model.addAttribute("investmentReport", reportService.generateInvestmentReport(userId));
        model.addAttribute("currentPage", "reports");
        model.addAttribute("title", "Reports");
        
        return "reports/index";
    }
    
    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName()).get().getId();
    }
}