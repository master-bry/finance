package com.master.finance.controller;

import com.master.finance.service.ReportService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Controller
@RequestMapping("/reports")
public class ReportsController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String showReports(Authentication authentication,
                              @RequestParam(required = false) Integer year,
                              @RequestParam(required = false) Integer month,
                              Model model) {

        String userId = getUserId(authentication);

        // Default to the current month/year if not supplied via query params
        LocalDate now = LocalDate.now();
        int reportYear  = (year  != null) ? year  : now.getYear();
        int reportMonth = (month != null) ? month : now.getMonthValue();

        String monthName = Month.of(reportMonth)
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        model.addAttribute("monthlyReport",
                reportService.generateMonthlyReport(userId, reportYear, reportMonth));
        model.addAttribute("debtReport",
                reportService.generateDebtReport(userId));
        model.addAttribute("investmentReport",
                reportService.generateInvestmentReport(userId));
        model.addAttribute("recentTransactions",
                reportService.getRecentTransactions(userId, reportYear, reportMonth));
        model.addAttribute("expenseByCategory",
                reportService.getExpenseByCategory(userId, reportYear, reportMonth));

        model.addAttribute("reportYear", reportYear);
        model.addAttribute("reportMonth", reportMonth);
        model.addAttribute("reportMonthName", monthName);

        model.addAttribute("currentPage", "reports");
        model.addAttribute("pageSubtitle", "Financial overview for " + monthName + " " + reportYear);
        model.addAttribute("title", "Reports");

        return "reports/index";
    }

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getId();
    }
}