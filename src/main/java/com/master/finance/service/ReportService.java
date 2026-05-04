package com.master.finance.service;

import com.master.finance.model.Debt;
import com.master.finance.model.Investment;
import com.master.finance.model.Transaction;
import com.master.finance.repository.DebtRepository;
import com.master.finance.repository.InvestmentRepository;
import com.master.finance.repository.TransactionRepository;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Comparator;
import java.util.LinkedHashMap;
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

    /**
     * Generate monthly financial report for a user
     * @param userId the user ID
     * @param year the year
     * @param month the month (1-12)
     * @return map containing monthly financial statistics
     */
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
     * Get all transactions for the given month, newest first, limited by limit.
     * @param userId the user ID
     * @param year the year
     * @param month the month (1-12)
     * @param limit maximum number of transactions to return (0 for all)
     * @return list of transactions sorted by date descending
     */
    public List<Transaction> getRecentTransactions(String userId, int year, int month, int limit) {
        List<Transaction> transactions = getMonthTransactions(userId, year, month);
        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .limit(limit == 0 ? Long.MAX_VALUE : limit)
                .collect(Collectors.toList());
    }

    // For backward compatibility (default limit 10)
    public List<Transaction> getRecentTransactions(String userId, int year, int month) {
        return getRecentTransactions(userId, year, month, 5);
    }

    /**
     * Returns a map of { category -> total amount } for expenses in the given month.
     * @param userId the user ID
     * @param year the year
     * @param month the month (1-12)
     * @return map of category names to total expense amounts
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

    /**
     * Generate debt report for a user
     * @param userId the user ID
     * @return map containing debt statistics and totals
     */
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

    /**
     * Generate investment report for a user
     * @param userId the user ID
     * @return map containing investment statistics and performance metrics
     */
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

    /**
     * Generate Excel report for monthly financial data
     * @param userId the user ID
     * @param year the year
     * @param month the month (1-12)
     * @return byte array containing Excel file data
     */
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

    // ─── PDF EXPORT ────────────────────────────────────────────────────────────

    public byte[] generateMonthlyReportPdf(String userId, int year, int month) {
        List<Transaction> transactions = getMonthTransactions(userId, year, month);
        Map<String, Object> summary = generateMonthlyReport(userId, year, month);
        Map<String, Double> expensesByCategory = getExpenseByCategory(userId, year, month);
        Map<String, Object> debtReport = generateDebtReport(userId);
        Map<String, Object> investmentReport = generateInvestmentReport(userId);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(bos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Title
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            String monthName = Month.of(month).name();
            Paragraph title = new Paragraph("Financial Report - " + monthName + " " + year)
                    .setFont(boldFont)
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // Monthly Summary Section
            document.add(new Paragraph("Monthly Summary")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginTop(20)
                    .setMarginBottom(10));

            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth();
            summaryTable.addHeaderCell(createCell("Total Income", boldFont, true));
            summaryTable.addHeaderCell(createCell(formatCurrency(summary.get("totalIncome")), normalFont, false));
            summaryTable.addHeaderCell(createCell("Total Expenses", boldFont, true));
            summaryTable.addHeaderCell(createCell(formatCurrency(summary.get("totalExpense")), normalFont, false));
            summaryTable.addHeaderCell(createCell("Net Savings", boldFont, true));
            summaryTable.addHeaderCell(createCell(formatCurrency(summary.get("balance")), normalFont, false));
            summaryTable.addHeaderCell(createCell("Savings Rate", boldFont, true));
            summaryTable.addHeaderCell(createCell(formatPercent(summary.get("savingsRate")), normalFont, false));
            document.add(summaryTable);

            // Transactions Section
            document.add(new Paragraph("Transactions")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginTop(20)
                    .setMarginBottom(10));

            Table transactionTable = new Table(UnitValue.createPercentArray(new float[]{25, 30, 20, 15, 10}))
                    .useAllAvailableWidth();
            transactionTable.addHeaderCell(createCell("Date", boldFont, true));
            transactionTable.addHeaderCell(createCell("Description", boldFont, true));
            transactionTable.addHeaderCell(createCell("Category", boldFont, true));
            transactionTable.addHeaderCell(createCell("Type", boldFont, true));
            transactionTable.addHeaderCell(createCell("Amount", boldFont, true));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            for (Transaction tx : transactions) {
                transactionTable.addCell(createCell(tx.getDate().format(formatter), normalFont, false));
                transactionTable.addCell(createCell(tx.getDescription(), normalFont, false));
                transactionTable.addCell(createCell(tx.getCategory(), normalFont, false));
                transactionTable.addCell(createCell(tx.getType(), normalFont, false));
                transactionTable.addCell(createCell(formatCurrency(tx.getAmount()), normalFont, false));
            }
            document.add(transactionTable);

            // Expenses by Category Section
            if (!expensesByCategory.isEmpty()) {
                document.add(new Paragraph("Expenses by Category")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setMarginTop(20)
                        .setMarginBottom(10));

                Table categoryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                        .useAllAvailableWidth();
                categoryTable.addHeaderCell(createCell("Category", boldFont, true));
                categoryTable.addHeaderCell(createCell("Amount", boldFont, true));

                for (Map.Entry<String, Double> entry : expensesByCategory.entrySet()) {
                    categoryTable.addCell(createCell(entry.getKey(), normalFont, false));
                    categoryTable.addCell(createCell(formatCurrency(entry.getValue()), normalFont, false));
                }
                document.add(categoryTable);
            }

            // Debt Summary Section
            document.add(new Paragraph("Debt Summary")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginTop(20)
                    .setMarginBottom(10));

            Table debtTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth();
            debtTable.addHeaderCell(createCell("Owed to Me", boldFont, true));
            debtTable.addHeaderCell(createCell(formatCurrency(debtReport.get("totalOwedToMe")), normalFont, false));
            debtTable.addHeaderCell(createCell("I Owe", boldFont, true));
            debtTable.addHeaderCell(createCell(formatCurrency(debtReport.get("totalIOwe")), normalFont, false));
            debtTable.addHeaderCell(createCell("Net Position", boldFont, true));
            debtTable.addHeaderCell(createCell(formatCurrency(debtReport.get("netPosition")), normalFont, false));
            document.add(debtTable);

            // Investment Summary Section
            document.add(new Paragraph("Investment Summary")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginTop(20)
                    .setMarginBottom(10));

            Table investmentTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth();
            investmentTable.addHeaderCell(createCell("Total Invested", boldFont, true));
            investmentTable.addHeaderCell(createCell(formatCurrency(investmentReport.get("totalInvested")), normalFont, false));
            investmentTable.addHeaderCell(createCell("Current Value", boldFont, true));
            investmentTable.addHeaderCell(createCell(formatCurrency(investmentReport.get("totalCurrentValue")), normalFont, false));
            investmentTable.addHeaderCell(createCell("Profit/Loss", boldFont, true));
            investmentTable.addHeaderCell(createCell(formatCurrency(investmentReport.get("totalProfitLoss")), normalFont, false));
            investmentTable.addHeaderCell(createCell("ROI", boldFont, true));
            investmentTable.addHeaderCell(createCell(formatPercent(investmentReport.get("roiPercent")), normalFont, false));
            document.add(investmentTable);

            document.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private Cell createCell(String text, PdfFont font, boolean isHeader) {
        Cell cell = new Cell();
        Paragraph paragraph = new Paragraph(text).setFont(font).setFontSize(10);
        cell.add(paragraph);
        if (isHeader) {
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            cell.setBold();
        }
        return cell;
    }

    private String formatCurrency(Object value) {
        if (value == null) return "0 TZS";
        double amount = ((Number) value).doubleValue();
        return String.format("%,.0f TZS", amount);
    }

    private String formatPercent(Object value) {
        if (value == null) return "0%";
        double percent = ((Number) value).doubleValue();
        return String.format("%.1f%%", percent);
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────────

    private List<Transaction> getMonthTransactions(String userId, int year, int month) {
        LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime end   = start.plusMonths(1);
        return transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, start, end);
    }
}