package com.master.finance.repository;

import com.master.finance.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    
    // Find all non-deleted transactions for a user, ordered by date descending
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Transaction> findByUserIdAndDeletedFalseOrderByDateDesc(String userId);
    
    // Find transactions by user ID and type (INCOME/EXPENSE)
    @Query("{ 'userId': ?0, 'type': ?1, 'deleted': false }")
    List<Transaction> findByUserIdAndTypeAndDeletedFalse(String userId, String type);
    
    // Find transactions by date range
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<Transaction> findByUserIdAndDateBetweenAndDeletedFalse(String userId, LocalDateTime start, LocalDateTime end);
    
    // Find all non-deleted transactions for a user (for balance calculation)
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Transaction> findTransactionsByUserIdAndDeletedFalse(String userId);
    
    // Alternative method name for the same query
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Transaction> findAllByUserIdAndDeletedFalse(String userId);
    
    // Find transactions by category
    @Query("{ 'userId': ?0, 'category': ?1, 'deleted': false }")
    List<Transaction> findByUserIdAndCategoryAndDeletedFalse(String userId, String category);
    
    // Find transactions by month and year
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<Transaction> findTransactionsByMonth(String userId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Get total income for a user
    @Query(value = "{ 'userId': ?0, 'type': 'INCOME', 'deleted': false }", count = true)
    long countIncomeTransactions(String userId);
    
    // Get total expense for a user
    @Query(value = "{ 'userId': ?0, 'type': 'EXPENSE', 'deleted': false }", count = true)
    long countExpenseTransactions(String userId);
}