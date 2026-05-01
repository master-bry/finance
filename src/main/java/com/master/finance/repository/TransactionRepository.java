package com.master.finance.repository;

import com.master.finance.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    @Query(value = "{ 'userId': ?0, 'deleted': false }", sort = "{ 'date': -1 }")
    List<Transaction> findByUserIdAndDeletedFalse(String userId);

    @Query(value = "{ 'userId': ?0, 'deleted': false }", sort = "{ 'date': -1 }")
    List<Transaction> findByUserIdAndDeletedFalseOrderByDateDesc(String userId);

    @Query(value = "{ 'userId': ?0, 'deleted': false }", sort = "{ 'date': -1 }")
    List<Transaction> findRecentTransactionsByUserId(String userId);

    @Query("{ 'userId': ?0, 'type': ?1, 'deleted': false }")
    List<Transaction> findByUserIdAndTypeAndDeletedFalse(String userId, String type);

    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<Transaction> findByUserIdAndDateBetweenAndDeletedFalse(String userId, LocalDateTime start, LocalDateTime end);

    @Query(value = "{ 'userId': ?0, 'type': 'EXPENSE', 'deleted': false }", fields = "{ 'category': 1, 'amount': 1 }")
    List<Transaction> findExpensesByUserIdAndDeletedFalse(String userId);

    // For balance calculations: get transactions before a specific date
    @Query("{ 'userId': ?0, 'date': { $lt: ?1 }, 'deleted': false }")
    List<Transaction> findByUserIdAndDateBeforeAndDeletedFalse(String userId, LocalDateTime date);
}