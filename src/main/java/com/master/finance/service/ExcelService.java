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
    
    // Generate Excel template for daily entry (NO dummy data)
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
        entry.calculateTotals();
        
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
        double totalSavings = entries.stream().mapToDouble(e -> e.getTotalIncome() - e.getTotalExpense()).sum();
        
        double averageDailySpending = entries.isEmpty() ? 0 : totalExpense / entries.size();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("totalSavings", totalSavings);
        summary.put("averageDailySpending", averageDailySpending);
        summary.put("daysCompleted", entries.size());
        summary.put("entries", entries);
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
    
    // Delete daily entry (soft delete)
    public void softDeleteDailyEntry(String entryId) {
        dailyEntryRepository.findById(entryId).ifPresent(entry -> {
            entry.setDeleted(true);
            entry.setDeletedAt(LocalDateTime.now());
            dailyEntryRepository.save(entry);
        });
    }
}