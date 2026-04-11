package com.master.finance.repository;

import com.master.finance.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByUserIdOrderByDateDesc(String userId);
    List<Transaction> findByUserIdAndType(String userId, String type);
    List<Transaction> findByUserIdAndDateBetween(String userId, LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 } }")
    List<Transaction> findTransactionsBetweenDates(String userId, LocalDateTime start, LocalDateTime end);
    
    @Query(value = "{ 'userId': ?0, 'type': 'EXPENSE' }", fields = "{ 'category': 1, 'amount': 1 }")
    List<Transaction> findExpensesByUserId(String userId);
}