package com.master.finance.repository;

import com.master.finance.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    /**
     * All non-deleted transactions for a user, newest first.
     * Primary method — used by ReportService, ReportsController, etc.
     */
    @Query(value = "{ 'userId': ?0, 'deleted': false }", sort = "{ 'date': -1 }")
    List<Transaction> findByUserIdAndDeletedFalse(String userId);

    /**
     * Alias kept for backward compatibility.
     * DailyEntryService and TransactionService reference this name.
     */
    @Query(value = "{ 'userId': ?0, 'deleted': false }", sort = "{ 'date': -1 }")
    List<Transaction> findByUserIdAndDeletedFalseOrderByDateDesc(String userId);

    /** Non-deleted transactions filtered by type (INCOME / EXPENSE). */
    @Query("{ 'userId': ?0, 'type': ?1, 'deleted': false }")
    List<Transaction> findByUserIdAndTypeAndDeletedFalse(String userId, String type);

    /** Non-deleted transactions within a date range — used for monthly reports. */
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<Transaction> findByUserIdAndDateBetweenAndDeletedFalse(String userId,
                                                                 LocalDateTime start,
                                                                 LocalDateTime end);

    /** Expense-only projection for category breakdowns. */
    @Query(value = "{ 'userId': ?0, 'type': 'EXPENSE', 'deleted': false }",
           fields = "{ 'category': 1, 'amount': 1 }")
    List<Transaction> findExpensesByUserIdAndDeletedFalse(String userId);
}