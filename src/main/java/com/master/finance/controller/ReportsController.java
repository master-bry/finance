package com.master.finance.controller;

import com.master.finance.service.ReportService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
                              @RequestParam(defaultValue = "10") int limit,  // NEW: transaction limit
                              Model model) {

        String userId = getUserId(authentication);

        LocalDate now = LocalDate.now();
        int reportYear  = (year  != null) ? year  : now.getYear();
        int reportMonth = (month != null) ? month : now.getMonthValue();

        String monthName = Month.of(reportMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        model.addAttribute("monthlyReport", reportService.generateMonthlyReport(userId, reportYear, reportMonth));
        model.addAttribute("debtReport", reportService.generateDebtReport(userId));
        model.addAttribute("investmentReport", reportService.generateInvestmentReport(userId));
        model.addAttribute("recentTransactions", reportService.getRecentTransactions(userId, reportYear, reportMonth, limit));
        model.addAttribute("expenseByCategory", reportService.getExpenseByCategory(userId, reportYear, reportMonth));

        model.addAttribute("reportYear", reportYear);
        model.addAttribute("reportMonth", reportMonth);
        model.addAttribute("reportMonthName", monthName);
        model.addAttribute("limit", limit);  // current selected limit

        model.addAttribute("currentPage", "reports");
        model.addAttribute("pageSubtitle", "Financial overview for " + monthName + " " + reportYear);
        model.addAttribute("title", "Reports");

        return "reports/index";
    }

    @GetMapping("/download")
    public ResponseEntity<ByteArrayResource> downloadReport(Authentication authentication,
                                                            @RequestParam int year,
                                                            @RequestParam int month) {
        String userId = getUserId(authentication);
        byte[] excelData = reportService.generateMonthlyReportExcel(userId, year, month);

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String filename = "Finance_Report_" + monthName + "_" + year + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(excelData));
    }

    @GetMapping("/download-pdf")
    public ResponseEntity<ByteArrayResource> downloadPdfReport(Authentication authentication,
                                                               @RequestParam int year,
                                                               @RequestParam int month) {
        String userId = getUserId(authentication);
        byte[] pdfData = reportService.generateMonthlyReportPdf(userId, year, month);

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String filename = "Finance_Report_" + monthName + "_" + year + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdfData));
    }

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getId();
    }
}