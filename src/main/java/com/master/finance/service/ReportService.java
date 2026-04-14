package com.master.finance.service;

import com.master.finance.model.Debt;
import com.master.finance.model.Investment;
import com.master.finance.model.Transaction;
import com.master.finance.repository.DebtRepository;
import com.master.finance.repository.InvestmentRepository;
import com.master.finance.repository.TransactionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DebtRepository debtRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    // ─── MONTHLY REPORT ───────────────────────────────────────────────────────

    public Map<String, Object> generateMonthlyReport(String userId, int year, int month) {
        List<Transaction> transactions = getMonthTransactions(userId, year, month);

        double totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();

        double totalExpense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount).sum();

        double balance = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (balance / totalIncome) * 100 : 0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalIncome", totalIncome);
        report.put("totalExpense", totalExpense);
        report.put("balance", balance);
        report.put("savingsRate", savingsRate);
        report.put("transactionCount", transactions.size());
        return report;
    }

    // ─── TRANSACTIONS FOR REPORT PAGE ─────────────────────────────────────────

    /**
     * Returns all transactions for the given month, newest first, limited by limit.
     */
    public List<Transaction> getRecentTransactions(String userId, int year, int month, int limit) {
        List<Transaction> transactions = getMonthTransactions(userId, year, month);
        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // For backward compatibility (default limit 10)
    public List<Transaction> getRecentTransactions(String userId, int year, int month) {
        return getRecentTransactions(userId, year, month, 5);
    }

    /**
     * Returns a map of { category -> total amount } for expenses in the given month.
     */
    public Map<String, Double> getExpenseByCategory(String userId, int year, int month) {
        return getMonthTransactions(userId, year, month).stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));
    }

    // ─── DEBT REPORT ──────────────────────────────────────────────────────────

    public Map<String, Object> generateDebtReport(String userId) {
        List<Debt> debts = debtRepository.findByUserIdAndDeletedFalse(userId);

        double totalOwedToMe = debts.stream()
                .filter(d -> "OWED_TO_ME".equals(d.getType()))
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount).sum();

        double totalIOwe = debts.stream()
                .filter(d -> "I_OWE".equals(d.getType()))
                .filter(d -> !"SETTLED".equals(d.getStatus()))
                .mapToDouble(Debt::getRemainingAmount).sum();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalOwedToMe", totalOwedToMe);
        report.put("totalIOwe", totalIOwe);
        report.put("netPosition", totalOwedToMe - totalIOwe);
        report.put("activeDebtsCount", debts.stream()
                .filter(d -> !"SETTLED".equals(d.getStatus())).count());
        return report;
    }

    // ─── INVESTMENT REPORT ────────────────────────────────────────────────────

    public Map<String, Object> generateInvestmentReport(String userId) {
        List<Investment> investments = investmentRepository.findByUserIdAndDeletedFalse(userId);

        double totalInvested     = investments.stream().mapToDouble(Investment::getAmountInvested).sum();
        double totalCurrentValue = investments.stream().mapToDouble(Investment::getCurrentValue).sum();
        double totalProfitLoss   = totalCurrentValue - totalInvested;
        double roiPercent        = totalInvested > 0 ? (totalProfitLoss / totalInvested) * 100 : 0;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalInvested", totalInvested);
        report.put("totalCurrentValue", totalCurrentValue);
        report.put("totalProfitLoss", totalProfitLoss);
        report.put("roiPercent", roiPercent);
        report.put("investmentCount", investments.size());
        return report;
    }

    // ─── EXCEL EXPORT ─────────────────────────────────────────────────────────

    public byte[] generateMonthlyReportExcel(String userId, int year, int month) {
        List<Transaction> transactions = getMonthTransactions(userId, year, month);
        Map<String, Object> summary = generateMonthlyReport(userId, year, month);
        Map<String, Double> expensesByCategory = getExpenseByCategory(userId, year, month);
        Map<String, Object> debtReport = generateDebtReport(userId);
        Map<String, Object> investmentReport = generateInvestmentReport(userId);

        try (Workbook workbook = new XSSFWorkbook()) {
            // 1. Summary Sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, summary, year, month);

            // 2. Transactions Sheet
            Sheet transactionsSheet = workbook.createSheet("Transactions");
            createTransactionsSheet(transactionsSheet, transactions);

            // 3. Expenses by Category Sheet
            Sheet categorySheet = workbook.createSheet("Expenses by Category");
            createCategorySheet(categorySheet, expensesByCategory);

            // 4. Debt Summary Sheet
            Sheet debtSheet = workbook.createSheet("Debt Summary");
            createDebtSheet(debtSheet, debtReport);

            // 5. Investment Summary Sheet
            Sheet investmentSheet = workbook.createSheet("Investment Summary");
            createInvestmentSheet(investmentSheet, investmentReport);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private void createSummarySheet(Sheet sheet, Map<String, Object> summary, int year, int month) {
        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("Monthly Financial Report - " + month + "/" + year);
        
        rowNum++;
        String[] labels = {"Total Income", "Total Expenses", "Net Savings", "Savings Rate", "Transaction Count"};
        Object[] values = {
            summary.get("totalIncome"),
            summary.get("totalExpense"),
            summary.get("balance"),
            summary.get("savingsRate") + "%",
            summary.get("transactionCount")
        };
        
        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(labels[i]);
            row.createCell(1).setCellValue(values[i].toString());
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createTransactionsSheet(Sheet sheet, List<Transaction> transactions) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Description", "Category", "Type", "Amount (TZS)"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Transaction tx : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tx.getDate().format(formatter));
            row.createCell(1).setCellValue(tx.getDescription());
            row.createCell(2).setCellValue(tx.getCategory());
            row.createCell(3).setCellValue(tx.getType());
            row.createCell(4).setCellValue(tx.getAmount());
        }
        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCategorySheet(Sheet sheet, Map<String, Double> expensesByCategory) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Category");
        headerRow.createCell(1).setCellValue("Amount (TZS)");
        
        for (Map.Entry<String, Double> entry : expensesByCategory.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createDebtSheet(Sheet sheet, Map<String, Object> debtReport) {
        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("Debt Summary");
        
        rowNum++;
        String[] labels = {"Owed to Me", "I Owe", "Net Position", "Active Debts"};
        Object[] values = {
            debtReport.get("totalOwedToMe"),
            debtReport.get("totalIOwe"),
            debtReport.get("netPosition"),
            debtReport.get("activeDebtsCount")
        };
        
        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(labels[i]);
            row.createCell(1).setCellValue(values[i].toString());
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createInvestmentSheet(Sheet sheet, Map<String, Object> investmentReport) {
        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("Investment Summary");
        
        rowNum++;
        String[] labels = {"Total Invested", "Current Value", "Profit/Loss", "ROI (%)", "Investment Count"};
        Object[] values = {
            investmentReport.get("totalInvested"),
            investmentReport.get("totalCurrentValue"),
            investmentReport.get("totalProfitLoss"),
            investmentReport.get("roiPercent") + "%",
            investmentReport.get("investmentCount")
        };
        
        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(labels[i]);
            row.createCell(1).setCellValue(values[i].toString());
        }
        
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private List<Transaction> getMonthTransactions(String userId, int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end   = start.plusMonths(1);
        return transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, start, end);
    }
}