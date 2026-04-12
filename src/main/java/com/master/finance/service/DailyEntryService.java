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
    
    // Save daily entry
    public DailyEntry saveDailyEntry(DailyEntry entry, String userId) {
        entry.setUserId(userId);
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setCompleted(true);
        entry.calculateTotals();
        
        // Also save each expense as a transaction
        for (DailyEntry.ExpenseItem expense : entry.getExpenses()) {
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setDescription(expense.getDescription());
            transaction.setAmount(expense.getAmount());
            transaction.setType("EXPENSE");
            transaction.setCategory(expense.getCategory());
            transaction.setDate(LocalDateTime.now());
            transaction.setDeleted(false);
            transactionRepository.save(transaction);
        }
        
        // Also save each income as a transaction
        for (DailyEntry.IncomeItem income : entry.getIncomes()) {
            Transaction transaction = new Transaction();
            transaction.setUserId(userId);
            transaction.setDescription(income.getDescription());
            transaction.setAmount(income.getAmount());
            transaction.setType("INCOME");
            transaction.setCategory(income.getSource());
            transaction.setDate(LocalDateTime.now());
            transaction.setDeleted(false);
            transactionRepository.save(transaction);
        }
        
        return dailyEntryRepository.save(entry);
    }
    
    // Get today's entry
    public Optional<DailyEntry> getTodayEntry(String userId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        Optional<DailyEntry> entry = dailyEntryRepository.findByUserIdAndDate(userId, today);
        
        if (entry.isPresent()) {
            return entry;
        }
        
        // Create new entry for today
        DailyEntry newEntry = new DailyEntry();
        newEntry.setUserId(userId);
        newEntry.setDate(today);
        newEntry.setOpeningBalance(getCurrentBalance(userId));
        return Optional.of(newEntry);
    }
    
    // Get all user entries
    public List<DailyEntry> getUserEntries(String userId) {
        return dailyEntryRepository.findByUserIdOrderByDateDesc(userId);
    }
    
    // Delete entry
    public void deleteEntry(String id) {
        dailyEntryRepository.deleteById(id);
    }
    
    // Get current balance from transactions
    public Double getCurrentBalance(String userId) {
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
                if (row.getRowNum() == 0) continue;
                
                String type = getCellValueAsString(row.getCell(0));
                String description = getCellValueAsString(row.getCell(1));
                Double amount = getCellValueAsDouble(row.getCell(2));
                String category = getCellValueAsString(row.getCell(3));
                
                if (description != null && !description.isEmpty() && amount != null && amount > 0) {
                    if ("EXPENSE".equalsIgnoreCase(type)) {
                        DailyEntry.ExpenseItem expense = new DailyEntry.ExpenseItem();
                        expense.setDescription(description);
                        expense.setAmount(amount);
                        expense.setCategory(category);
                        entry.getExpenses().add(expense);
                    } else if ("INCOME".equalsIgnoreCase(type)) {
                        DailyEntry.IncomeItem income = new DailyEntry.IncomeItem();
                        income.setDescription(description);
                        income.setAmount(amount);
                        income.setSource(category);
                        entry.getIncomes().add(income);
                    }
                }
            }
            
            entry.calculateTotals();
            return saveDailyEntry(entry, userId);
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage());
        }
    }
    
    // Generate Excel template
    public byte[] generateExcelTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Entry");
            
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Type (INCOME/EXPENSE)", "Description", "Amount (TZS)", "Category/Source"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }
            
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            default: return "";
        }
    }
    
    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return 0.0;
        switch (cell.getCellType()) {
            case NUMERIC: return cell.getNumericCellValue();
            case STRING:
                try { return Double.parseDouble(cell.getStringCellValue()); }
                catch (NumberFormatException e) { return 0.0; }
            default: return 0.0;
        }
    }
}