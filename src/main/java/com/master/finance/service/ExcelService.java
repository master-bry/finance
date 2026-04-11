package com.master.finance.service;

import com.master.finance.model.DailyEntry;
import com.master.finance.model.Transaction;
import com.master.finance.model.User;
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
    private TransactionService transactionService;
    
    @Autowired
    private DailyEntryRepository dailyEntryRepository;
    
    // Process uploaded Excel file
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
                
                // Read Excel columns
                // Column A: Date
                Cell dateCell = row.getCell(0);
                if (dateCell != null) {
                    LocalDateTime date = dateCell.getLocalDateTimeCellValue();
                    transaction.setDate(date);
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
                    transaction.setAmount(amountCell.getNumericCellValue());
                }
                
                // Column E: Type (INCOME/EXPENSE)
                Cell typeCell = row.getCell(4);
                if (typeCell != null) {
                    transaction.setType(typeCell.getStringCellValue());
                }
                
                transactions.add(transaction);
            }
            
            // Save all transactions
            transactionService.saveAll(transactions);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return transactions;
    }
    
    // Generate Excel template for daily entry
    public byte[] generateDailyEntryTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Finance Entry");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Date (YYYY-MM-DD)", "Description", "Category", "Amount", "Type (INCOME/EXPENSE)", "Notes"};
            
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }
            
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
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        List<DailyEntry> entries = dailyEntryRepository.findByUserIdAndDateBetween(userId, today, today.plusDays(1));
        return !entries.isEmpty() && entries.get(0).isCompleted();
    }
}