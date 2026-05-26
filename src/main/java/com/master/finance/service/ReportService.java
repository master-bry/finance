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
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
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
     * Generate Excel report for monthly financial data (async version)
     * @param userId the user ID
     * @param year the year
     * @param month the month (1-12)
     * @return CompletableFuture with byte array containing Excel file data
     */
    @Async("reportExecutor")
    public CompletableFuture<byte[]> generateMonthlyReportExcelAsync(String userId, int year, int month) {
        try {
            byte[] result = generateMonthlyReportExcelSync(userId, year, month);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Generate Excel report for monthly financial data
     * @param userId the user ID
     * @param year the year
     * @param month the month (1-12)
     * @return byte array containing Excel file data
     */
    public byte[] generateMonthlyReportExcel(String userId, int year, int month) {
        return generateMonthlyReportExcelSync(userId, year, month);
    }

    /**
     * Internal sync method for Excel generation with professional formatting and totals
     */
    private byte[] generateMonthlyReportExcelSync(String userId, int year, int month) {
        List<Transaction> transactions = getMonthTransactions(userId, year, month);
        Map<String, Object> summary = generateMonthlyReport(userId, year, month);
        Map<String, Double> expensesByCategory = getExpenseByCategory(userId, year, month);
        Map<String, Object> debtReport = generateDebtReport(userId);
        Map<String, Object> investmentReport = generateInvestmentReport(userId);

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle boldStyle = createBoldStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            // 1. Summary Sheet
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, summary, year, month, titleStyle, boldStyle, currencyStyle);

            // 2. Transactions Sheet
            Sheet transactionsSheet = workbook.createSheet("Transactions");
            createTransactionsSheet(transactionsSheet, transactions, boldStyle, currencyStyle, headerStyle);

            // 3. Expenses by Category Sheet
            Sheet categorySheet = workbook.createSheet("Expenses by Category");
            createCategorySheet(categorySheet, expensesByCategory, boldStyle, currencyStyle, headerStyle);

            // 4. Debt Summary Sheet
            Sheet debtSheet = workbook.createSheet("Debt Summary");
            createDebtSheet(debtSheet, debtReport, titleStyle, boldStyle, currencyStyle);

            // 5. Investment Summary Sheet
            Sheet investmentSheet = workbook.createSheet("Investment Summary");
            createInvestmentSheet(investmentSheet, investmentReport, titleStyle, boldStyle, currencyStyle);

            for (int i = 0; i < 5; i++) {
                workbook.getSheetAt(i).getRow(0).createCell(3).setCellValue("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private void createSummarySheet(Sheet sheet, Map<String, Object> summary, int year, int month,
                                     CellStyle titleStyle, CellStyle boldStyle, CellStyle currencyStyle) {
        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Monthly Financial Report - " + month + "/" + year);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));

        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        rowNum++;
        String[] labels = {"Total Income", "Total Expenses", "Net Savings", "Savings Rate", "Transaction Count"};
        Object[] values = {
            summary.get("totalIncome"),
            summary.get("totalExpense"),
            summary.get("balance"),
            summary.get("savingsRate"),
            summary.get("transactionCount")
        };

        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(rowNum++);
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue(labels[i]);
            labelCell.setCellStyle(boldStyle);
            Cell valueCell = row.createCell(1);
            if (i < 3) {
                valueCell.setCellValue((Double) values[i]);
                valueCell.setCellStyle(currencyStyle);
            } else if (i == 3) {
                valueCell.setCellValue(String.format("%.1f%%", values[i]));
            } else {
                valueCell.setCellValue(values[i].toString());
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    private void createTransactionsSheet(Sheet sheet, List<Transaction> transactions,
                                          CellStyle boldStyle, CellStyle currencyStyle, CellStyle headerStyle) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Date", "Description", "Category", "Type", "Amount (TZS)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        double incomeTotal = 0;
        double expenseTotal = 0;

        for (Transaction tx : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tx.getDate().format(formatter));
            row.createCell(1).setCellValue(tx.getDescription());
            row.createCell(2).setCellValue(tx.getCategory());
            row.createCell(3).setCellValue(tx.getType());
            Cell amountCell = row.createCell(4);
            amountCell.setCellValue(tx.getAmount());
            amountCell.setCellStyle(currencyStyle);

            if ("INCOME".equals(tx.getType())) incomeTotal += tx.getAmount();
            else expenseTotal += tx.getAmount();
        }

        rowNum++;
        Row incomeTotalRow = sheet.createRow(rowNum++);
        Cell incomeLabel = incomeTotalRow.createCell(0);
        incomeLabel.setCellValue("TOTAL INCOME");
        incomeLabel.setCellStyle(boldStyle);
        incomeTotalRow.createCell(1).setCellStyle(boldStyle);
        incomeTotalRow.createCell(2).setCellStyle(boldStyle);
        incomeTotalRow.createCell(3).setCellStyle(boldStyle);
        Cell incomeVal = incomeTotalRow.createCell(4);
        incomeVal.setCellValue(incomeTotal);
        incomeVal.setCellStyle(currencyStyle);

        Row expenseTotalRow = sheet.createRow(rowNum++);
        Cell expenseLabel = expenseTotalRow.createCell(0);
        expenseLabel.setCellValue("TOTAL EXPENSES");
        expenseLabel.setCellStyle(boldStyle);
        expenseTotalRow.createCell(1).setCellStyle(boldStyle);
        expenseTotalRow.createCell(2).setCellStyle(boldStyle);
        expenseTotalRow.createCell(3).setCellStyle(boldStyle);
        Cell expenseVal = expenseTotalRow.createCell(4);
        expenseVal.setCellValue(expenseTotal);
        expenseVal.setCellStyle(currencyStyle);

        Row netRow = sheet.createRow(rowNum++);
        Cell netLabel = netRow.createCell(0);
        netLabel.setCellValue("NET BALANCE");
        netLabel.setCellStyle(boldStyle);
        netRow.createCell(1).setCellStyle(boldStyle);
        netRow.createCell(2).setCellStyle(boldStyle);
        netRow.createCell(3).setCellStyle(boldStyle);
        Cell netVal = netRow.createCell(4);
        netVal.setCellValue(incomeTotal - expenseTotal);
        netVal.setCellStyle(currencyStyle);

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCategorySheet(Sheet sheet, Map<String, Double> expensesByCategory,
                                      CellStyle boldStyle, CellStyle currencyStyle, CellStyle headerStyle) {
        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Category", "Amount (TZS)", "% of Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        double total = expensesByCategory.values().stream().mapToDouble(Double::doubleValue).sum();

        for (Map.Entry<String, Double> entry : expensesByCategory.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            Cell amtCell = row.createCell(1);
            amtCell.setCellValue(entry.getValue());
            amtCell.setCellStyle(currencyStyle);
            double pct = total > 0 ? (entry.getValue() / total) * 100 : 0;
            row.createCell(2).setCellValue(String.format("%.1f%%", pct));
        }

        rowNum++;
        Row totalRow = sheet.createRow(rowNum++);
        Cell totalLabel = totalRow.createCell(0);
        totalLabel.setCellValue("TOTAL EXPENSES");
        totalLabel.setCellStyle(boldStyle);
        Cell totalAmt = totalRow.createCell(1);
        totalAmt.setCellValue(total);
        totalAmt.setCellStyle(currencyStyle);
        totalRow.createCell(2).setCellStyle(boldStyle);

        Row balanceRow = sheet.createRow(rowNum++);
        Cell balanceLabel = balanceRow.createCell(0);
        balanceLabel.setCellValue("NOTE");
        balanceLabel.setCellStyle(boldStyle);
        balanceRow.createCell(1).setCellValue("Expense categories are based on allocated budget categories");

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    private void createDebtSheet(Sheet sheet, Map<String, Object> debtReport,
                                  CellStyle titleStyle, CellStyle boldStyle, CellStyle currencyStyle) {
        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Debt Summary");
        titleCell.setCellStyle(titleStyle);

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
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue(labels[i]);
            labelCell.setCellStyle(boldStyle);
            Cell valueCell = row.createCell(1);
            if (i < 3) {
                valueCell.setCellValue((Double) values[i]);
                valueCell.setCellStyle(currencyStyle);
            } else {
                valueCell.setCellValue(values[i].toString());
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createInvestmentSheet(Sheet sheet, Map<String, Object> investmentReport,
                                        CellStyle titleStyle, CellStyle boldStyle, CellStyle currencyStyle) {
        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Investment Summary");
        titleCell.setCellStyle(titleStyle);

        rowNum++;
        String[] labels = {"Total Invested", "Current Value", "Profit/Loss", "ROI (%)", "Investment Count"};
        Object[] values = {
            investmentReport.get("totalInvested"),
            investmentReport.get("totalCurrentValue"),
            investmentReport.get("totalProfitLoss"),
            investmentReport.get("roiPercent"),
            investmentReport.get("investmentCount")
        };

        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(rowNum++);
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue(labels[i]);
            labelCell.setCellStyle(boldStyle);
            Cell valueCell = row.createCell(1);
            if (i < 3) {
                valueCell.setCellValue((Double) values[i]);
                valueCell.setCellStyle(currencyStyle);
            } else if (i == 3) {
                valueCell.setCellValue(String.format("%.1f%%", values[i]));
            } else {
                valueCell.setCellValue(values[i].toString());
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    // ─── PDF EXPORT ────────────────────────────────────────────────────────────

    /**
     * Generate PDF report for monthly financial data (async version)
     * @param userId the user ID
     * @param year the year
     * @param month the month (1-12)
     * @return CompletableFuture with byte array containing PDF file data
     */
    @Async("reportExecutor")
    public CompletableFuture<byte[]> generateMonthlyReportPdfAsync(String userId, int year, int month) {
        try {
            byte[] result = generateMonthlyReportPdfSync(userId, year, month);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Generate PDF report for monthly financial data
     * @param userId the user ID
     * @param year the year
     * @param month the month (1-12)
     * @return byte array containing PDF file data
     */
    public byte[] generateMonthlyReportPdf(String userId, int year, int month) {
        return generateMonthlyReportPdfSync(userId, year, month);
    }

    /**
     * Internal sync method for PDF generation with professional formatting and totals
     */
    private byte[] generateMonthlyReportPdfSync(String userId, int year, int month) {
        List<Transaction> transactions = getMonthTransactions(userId, year, month);
        Map<String, Object> summary = generateMonthlyReport(userId, year, month);
        Map<String, Double> expensesByCategory = getExpenseByCategory(userId, year, month);
        Map<String, Object> debtReport = generateDebtReport(userId);
        Map<String, Object> investmentReport = generateInvestmentReport(userId);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(bos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont italicFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            String monthName = Month.of(month).name();
            Paragraph title = new Paragraph("Financial Report - " + monthName + " " + year)
                    .setFont(boldFont)
                    .setFontSize(22)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(title);

            Paragraph generatedDate = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")))
                    .setFont(italicFont)
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(generatedDate);

            document.add(new Paragraph("Monthly Summary")
                    .setFont(boldFont)
                    .setFontSize(16)
                    .setMarginTop(15)
                    .setMarginBottom(10));

            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth();
            summaryTable.addHeaderCell(createPdfCell("Total Income", boldFont, true));
            summaryTable.addHeaderCell(createPdfCell(formatCurrency(summary.get("totalIncome")), boldFont, true));
            summaryTable.addHeaderCell(createPdfCell("Total Expenses", boldFont, true));
            summaryTable.addHeaderCell(createPdfCell(formatCurrency(summary.get("totalExpense")), boldFont, true));
            summaryTable.addHeaderCell(createPdfCell("Net Savings", boldFont, true));
            summaryTable.addHeaderCell(createPdfCell(formatCurrency(summary.get("balance")), boldFont, true));
            summaryTable.addHeaderCell(createPdfCell("Savings Rate", boldFont, true));
            summaryTable.addHeaderCell(createPdfCell(formatPercent(summary.get("savingsRate")), boldFont, true));
            document.add(summaryTable);

            document.add(new Paragraph("Transactions")
                    .setFont(boldFont)
                    .setFontSize(16)
                    .setMarginTop(20)
                    .setMarginBottom(10));

            Table transactionTable = new Table(UnitValue.createPercentArray(new float[]{18, 28, 18, 18, 18}))
                    .useAllAvailableWidth();
            transactionTable.addHeaderCell(createPdfCell("Date", boldFont, true));
            transactionTable.addHeaderCell(createPdfCell("Description", boldFont, true));
            transactionTable.addHeaderCell(createPdfCell("Category", boldFont, true));
            transactionTable.addHeaderCell(createPdfCell("Type", boldFont, true));
            transactionTable.addHeaderCell(createPdfCell("Amount", boldFont, true));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            double incomeTotal = 0;
            double expenseTotal = 0;

            for (Transaction tx : transactions) {
                transactionTable.addCell(createPdfCell(tx.getDate().format(formatter), normalFont, false));
                transactionTable.addCell(createPdfCell(tx.getDescription(), normalFont, false));
                transactionTable.addCell(createPdfCell(tx.getCategory(), normalFont, false));
                transactionTable.addCell(createPdfCell(tx.getType(), normalFont, false));
                transactionTable.addCell(createPdfCell(formatCurrency(tx.getAmount()), normalFont, false));
                if ("INCOME".equals(tx.getType())) incomeTotal += tx.getAmount();
                else expenseTotal += tx.getAmount();
            }

            transactionTable.addCell(createPdfCell("", normalFont, false));
            transactionTable.addCell(createPdfCell("", normalFont, false));
            transactionTable.addCell(createPdfCell("", normalFont, false));
            transactionTable.addCell(createPdfCell("TOTAL INCOME", boldFont, true));
            transactionTable.addCell(createPdfCell(formatCurrency(incomeTotal), boldFont, true));

            transactionTable.addCell(createPdfCell("", normalFont, false));
            transactionTable.addCell(createPdfCell("", normalFont, false));
            transactionTable.addCell(createPdfCell("", normalFont, false));
            transactionTable.addCell(createPdfCell("TOTAL EXPENSES", boldFont, true));
            transactionTable.addCell(createPdfCell(formatCurrency(expenseTotal), boldFont, true));

            transactionTable.addCell(createPdfCell("", normalFont, false));
            transactionTable.addCell(createPdfCell("", normalFont, false));
            transactionTable.addCell(createPdfCell("", normalFont, false));
            transactionTable.addCell(createPdfCell("NET BALANCE", boldFont, true));
            transactionTable.addCell(createPdfCell(formatCurrency(incomeTotal - expenseTotal), boldFont, true));

            document.add(transactionTable);

            if (!expensesByCategory.isEmpty()) {
                document.add(new Paragraph("Expenses by Category")
                        .setFont(boldFont)
                        .setFontSize(16)
                        .setMarginTop(20)
                        .setMarginBottom(10));

                Table categoryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                        .useAllAvailableWidth();
                categoryTable.addHeaderCell(createPdfCell("Category", boldFont, true));
                categoryTable.addHeaderCell(createPdfCell("Amount", boldFont, true));

                double catTotal = 0;
                for (Map.Entry<String, Double> entry : expensesByCategory.entrySet()) {
                    categoryTable.addCell(createPdfCell(entry.getKey(), normalFont, false));
                    categoryTable.addCell(createPdfCell(formatCurrency(entry.getValue()), normalFont, false));
                    catTotal += entry.getValue();
                }

                categoryTable.addCell(createPdfCell("TOTAL", boldFont, true));
                categoryTable.addCell(createPdfCell(formatCurrency(catTotal), boldFont, true));
                document.add(categoryTable);
            }

            document.add(new Paragraph("Debt Summary")
                    .setFont(boldFont)
                    .setFontSize(16)
                    .setMarginTop(20)
                    .setMarginBottom(10));

            Table debtTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth();
            debtTable.addHeaderCell(createPdfCell("Owed to Me", boldFont, true));
            debtTable.addHeaderCell(createPdfCell(formatCurrency(debtReport.get("totalOwedToMe")), boldFont, true));
            debtTable.addHeaderCell(createPdfCell("I Owe", boldFont, true));
            debtTable.addHeaderCell(createPdfCell(formatCurrency(debtReport.get("totalIOwe")), boldFont, true));
            debtTable.addHeaderCell(createPdfCell("Net Position", boldFont, true));
            debtTable.addHeaderCell(createPdfCell(formatCurrency(debtReport.get("netPosition")), boldFont, true));
            document.add(debtTable);

            document.add(new Paragraph("Investment Summary")
                    .setFont(boldFont)
                    .setFontSize(16)
                    .setMarginTop(20)
                    .setMarginBottom(10));

            Table investmentTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth();
            investmentTable.addHeaderCell(createPdfCell("Total Invested", boldFont, true));
            investmentTable.addHeaderCell(createPdfCell(formatCurrency(investmentReport.get("totalInvested")), boldFont, true));
            investmentTable.addHeaderCell(createPdfCell("Current Value", boldFont, true));
            investmentTable.addHeaderCell(createPdfCell(formatCurrency(investmentReport.get("totalCurrentValue")), boldFont, true));
            investmentTable.addHeaderCell(createPdfCell("Profit/Loss", boldFont, true));
            investmentTable.addHeaderCell(createPdfCell(formatCurrency(investmentReport.get("totalProfitLoss")), boldFont, true));
            investmentTable.addHeaderCell(createPdfCell("ROI", boldFont, true));
            investmentTable.addHeaderCell(createPdfCell(formatPercent(investmentReport.get("roiPercent")), boldFont, true));
            document.add(investmentTable);

            Paragraph footer = new Paragraph("\n\nGenerated by Finance Tracker")
                    .setFont(italicFont)
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(footer);

            document.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private com.itextpdf.layout.element.Cell createPdfCell(String text, PdfFont font, boolean isHeader) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell();
        Paragraph paragraph = new Paragraph(text).setFont(font).setFontSize(9);
        cell.add(paragraph);
        if (isHeader) {
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
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