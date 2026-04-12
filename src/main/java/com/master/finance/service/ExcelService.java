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
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private DailyEntryRepository dailyEntryRepository;
    
    @Autowired
    private TransactionService transactionService;
    
    // Process uploaded Excel file and convert to transactions
    public List<Transaction> processExcelFile(MultipartFile file, String userId) {
        List<Transaction> transactions = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header row
                
                Transaction transaction = new Transaction();
                transaction.setUserId(userId);
                
                // Column A: Date
                Cell dateCell = row.getCell(0);
                if (dateCell != null) {
                    if (dateCell.getCellType() == CellType.NUMERIC) {
                        transaction.setDate(dateCell.getLocalDateTimeCellValue());
                    } else if (dateCell.getCellType() == CellType.STRING) {
                        try {
                            LocalDateTime date = LocalDateTime.parse(dateCell.getStringCellValue(), dateFormatter);
                            transaction.setDate(date);
                        } catch (Exception e) {
                            transaction.setDate(LocalDateTime.now());
                        }
                    }
                }
                
                // Column B: Description
                Cell descCell = row.getCell(1);
                if (descCell != null) {
                    transaction.setDescription(descCell.getStringCellValue());
                }
                
                // Column C: Category
                Cell catCell = row.getCell(2);
                if (catCell != null) {
                    transaction.setCategory(catCell.getStringCellValue());
                }
                
                // Column D: Amount
                Cell amountCell = row.getCell(3);
                if (amountCell != null) {
                    if (amountCell.getCellType() == CellType.NUMERIC) {
                        transaction.setAmount(amountCell.getNumericCellValue());
                    }
                }
                
                // Column E: Type (INCOME/EXPENSE)
                Cell typeCell = row.getCell(4);
                if (typeCell != null) {
                    transaction.setType(typeCell.getStringCellValue());
                }
                
                // Column F: Notes
                Cell notesCell = row.getCell(5);
                if (notesCell != null) {
                    transaction.setNotes(notesCell.getStringCellValue());
                }
                
                if (transaction.getDescription() != null && transaction.getAmount() != null && transaction.getType() != null) {
                    transactions.add(transaction);
                }
            }
            
            // Save all valid transactions
            if (!transactions.isEmpty()) {
                transactionRepository.saveAll(transactions);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage());
        }
        
        return transactions;
    }
    
    // Generate Excel template for daily entry
    public byte[] generateDailyEntryTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Finance Entry");
            
            // Create header row with styling
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Date (YYYY-MM-DD)", "Description", "Category", "Amount", "Type (INCOME/EXPENSE)", "Notes"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
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
            
            // Add example row
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue("2024-01-15");
            exampleRow.createCell(1).setCellValue("Grocery shopping");
            exampleRow.createCell(2).setCellValue("Food");
            exampleRow.createCell(3).setCellValue(50000);
            exampleRow.createCell(4).setCellValue("EXPENSE");
            exampleRow.createCell(5).setCellValue("Bought vegetables and fruits");
            
            Row exampleRow2 = sheet.createRow(2);
            exampleRow2.createCell(0).setCellValue("2024-01-15");
            exampleRow2.createCell(1).setCellValue("Salary payment");
            exampleRow2.createCell(2).setCellValue("Income");
            exampleRow2.createCell(3).setCellValue(500000);
            exampleRow2.createCell(4).setCellValue("INCOME");
            exampleRow2.createCell(5).setCellValue("Monthly salary");
            
            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
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
    
    // Save daily entry from form
    public DailyEntry saveDailyEntry(DailyEntry entry, String userId) {
        entry.setUserId(userId);
        entry.setDate(LocalDateTime.now());
        entry.setCompleted(true);
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setDeleted(false);
        
        // Calculate savings
        entry.setSavings(entry.getTotalIncome() - entry.getTotalExpense());
        
        return dailyEntryRepository.save(entry);
    }
    
    // Get daily entries for a month
    public List<DailyEntry> getMonthlyEntries(String userId, int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1);
        return dailyEntryRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }
    
    // Check if daily entry is completed for today
    public boolean isTodayEntryCompleted(String userId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        Optional<DailyEntry> entry = dailyEntryRepository.findByUserIdAndDate(userId, today);
        return entry.isPresent() && entry.get().isCompleted();
    }
    
    // Get daily entry for specific date
    public Optional<DailyEntry> getDailyEntryByDate(String userId, LocalDateTime date) {
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        return dailyEntryRepository.findByUserIdAndDate(userId, startOfDay);
    }
    
    // Get monthly summary
    public Map<String, Object> getMonthlySummary(String userId, int year, int month) {
        List<DailyEntry> entries = getMonthlyEntries(userId, year, month);
        
        double totalIncome = entries.stream().mapToDouble(DailyEntry::getTotalIncome).sum();
        double totalExpense = entries.stream().mapToDouble(DailyEntry::getTotalExpense).sum();
        double totalSavings = entries.stream().mapToDouble(DailyEntry::getSavings).sum();
        
        // Calculate average daily spending
        double averageDailySpending = entries.isEmpty() ? 0 : totalExpense / entries.size();
        
        // Get mood distribution
        Map<String, Long> moodDistribution = new HashMap<>();
        for (DailyEntry entry : entries) {
            if (entry.getMood() != null) {
                moodDistribution.merge(entry.getMood(), 1L, Long::sum);
            }
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("totalSavings", totalSavings);
        summary.put("averageDailySpending", averageDailySpending);
        summary.put("daysCompleted", entries.size());
        summary.put("entries", entries);
        summary.put("moodDistribution", moodDistribution);
        summary.put("year", year);
        summary.put("month", month);
        
        return summary;
    }
    
    // Export transactions to Excel
    public byte[] exportTransactionsToExcel(String userId, int year, int month) {
        LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime endDate = startDate.plusMonths(1);
        
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, startDate, endDate);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");
            
            // Create header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Date", "Description", "Category", "Amount", "Type", "Notes"};
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
            
            // Add data rows
            int rowNum = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Transaction t : transactions) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.getDate().format(formatter));
                row.createCell(1).setCellValue(t.getDescription());
                row.createCell(2).setCellValue(t.getCategory());
                row.createCell(3).setCellValue(t.getAmount());
                row.createCell(4).setCellValue(t.getType());
                row.createCell(5).setCellValue(t.getNotes() != null ? t.getNotes() : "");
            }
            
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Create daily entry from today's transactions
    public DailyEntry createDailyEntryFromTransactions(String userId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);
        
        List<Transaction> todayTransactions = transactionRepository.findByUserIdAndDateBetweenAndDeletedFalse(userId, today, tomorrow);
        
        DailyEntry entry = new DailyEntry();
        entry.setUserId(userId);
        entry.setDate(today);
        
        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> expensesByCategory = new HashMap<>();
        
        for (Transaction t : todayTransactions) {
            if ("INCOME".equals(t.getType())) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
                expensesByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }
        
        entry.setTotalIncome(totalIncome);
        entry.setTotalExpense(totalExpense);
        entry.setExpensesByCategory(expensesByCategory);
        entry.setSavings(totalIncome - totalExpense);
        entry.setCompleted(true);
        
        return dailyEntryRepository.save(entry);
    }
    
    // Update daily entry
    public DailyEntry updateDailyEntry(String entryId, DailyEntry updatedEntry) {
        return dailyEntryRepository.findById(entryId).map(entry -> {
            entry.setTotalIncome(updatedEntry.getTotalIncome());
            entry.setTotalExpense(updatedEntry.getTotalExpense());
            entry.setExpensesByCategory(updatedEntry.getExpensesByCategory());
            entry.setGoalsCompleted(updatedEntry.getGoalsCompleted());
            entry.setNotes(updatedEntry.getNotes());
            entry.setMood(updatedEntry.getMood());
            entry.setCompleted(updatedEntry.isCompleted());
            entry.setSavings(entry.getTotalIncome() - entry.getTotalExpense());
            entry.setUpdatedAt(LocalDateTime.now());
            return dailyEntryRepository.save(entry);
        }).orElseThrow(() -> new RuntimeException("Daily entry not found"));
    }
    
    // Delete daily entry (soft delete)
    public void softDeleteDailyEntry(String entryId) {
        dailyEntryRepository.findById(entryId).ifPresent(entry -> {
            entry.setDeleted(true);
            entry.setDeletedAt(LocalDateTime.now());
            dailyEntryRepository.save(entry);
        });
    }
}