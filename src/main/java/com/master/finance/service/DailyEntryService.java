package com.master.finance.service;

import com.master.finance.model.DailyEntry;
import com.master.finance.model.Transaction;
import com.master.finance.repository.DailyEntryRepository;
import com.master.finance.repository.TransactionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DailyEntryService {
    
    @Autowired
    private DailyEntryRepository dailyEntryRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    public DailyEntry saveDailyEntry(DailyEntry entry, String userId) {
        entry.setUserId(userId);
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setCompleted(true);
        entry.calculateTotals();
        return dailyEntryRepository.save(entry);
    }
    
    public Optional<DailyEntry> getTodayEntry(String userId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return dailyEntryRepository.findByUserIdAndDate(userId, today);
    }
    
    public List<DailyEntry> getUserEntries(String userId) {
        return dailyEntryRepository.findByUserIdOrderByDateDesc(userId);
    }
    
    public void deleteEntry(String id) {
        dailyEntryRepository.deleteById(id);
    }
    
    public Double getCurrentBalance(String userId) {
        Optional<DailyEntry> latestEntry = dailyEntryRepository.findLatestByUserId(userId);
        if (latestEntry.isPresent()) {
            return latestEntry.get().getClosingBalance();
        }
        // Get from transactions if no daily entry
        List<Transaction> transactions = transactionRepository.findByUserIdAndDeletedFalseOrderByDateDesc(userId);
        double totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        double totalExpense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();
        return totalIncome - totalExpense;
    }
    
    // Process Excel file
    public DailyEntry processExcelFile(MultipartFile file, String userId, Double openingBalance) {
        DailyEntry entry = new DailyEntry();
        entry.setUserId(userId);
        entry.setDate(LocalDateTime.now());
        entry.setOpeningBalance(openingBalance);
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                
                // Check if row is empty
                if (isRowEmpty(row)) continue;
                
                String type = getCellValueAsString(row.getCell(0));
                String description = getCellValueAsString(row.getCell(1));
                Double amount = getCellValueAsDouble(row.getCell(2));
                String category = getCellValueAsString(row.getCell(3));
                
                if (description != null && !description.isEmpty() && amount != null && amount > 0) {
                    if ("EXPENSE".equalsIgnoreCase(type)) {
                        DailyEntry.ExpenseItem expense = new DailyEntry.ExpenseItem();
                        expense.setDescription(description);
                        expense.setAmount(amount);
                        expense.setCategory(category != null && !category.isEmpty() ? category : "Other");
                        entry.getExpenses().add(expense);
                        
                        // Also save as transaction
                        Transaction transaction = new Transaction();
                        transaction.setUserId(userId);
                        transaction.setDescription(description);
                        transaction.setAmount(amount);
                        transaction.setType("EXPENSE");
                        transaction.setCategory(category != null && !category.isEmpty() ? category : "Other");
                        transaction.setDate(LocalDateTime.now());
                        transaction.setDeleted(false);
                        transactionRepository.save(transaction);
                        
                    } else if ("INCOME".equalsIgnoreCase(type)) {
                        DailyEntry.IncomeItem income = new DailyEntry.IncomeItem();
                        income.setDescription(description);
                        income.setAmount(amount);
                        income.setSource(category != null && !category.isEmpty() ? category : "Other");
                        entry.getIncomes().add(income);
                        
                        // Also save as transaction
                        Transaction transaction = new Transaction();
                        transaction.setUserId(userId);
                        transaction.setDescription(description);
                        transaction.setAmount(amount);
                        transaction.setType("INCOME");
                        transaction.setCategory(category != null && !category.isEmpty() ? category : "Other");
                        transaction.setDate(LocalDateTime.now());
                        transaction.setDeleted(false);
                        transactionRepository.save(transaction);
                    }
                }
            }
            
            entry.calculateTotals();
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage());
        }
        
        return dailyEntryRepository.save(entry);
    }
    
    // Generate Excel template
    public byte[] generateExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Entry");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Type (INCOME/EXPENSE)", "Description", "Amount (TZS)", "Category/Source"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }
            
            // Write to byte array
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Helper method to check if row is empty
    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int i = 0; i < 4; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    // Helper method to get cell value as string
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    // Helper method to get cell value as double
    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            default:
                return 0.0;
        }
    }
}