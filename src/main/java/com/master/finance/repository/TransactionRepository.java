package com.master.finance.repository;

import com.master.finance.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Transaction> findByUserIdAndDeletedFalseOrderByDateDesc(String userId);
    
    @Query("{ 'userId': ?0, 'type': ?1, 'deleted': false }")
    List<Transaction> findByUserIdAndTypeAndDeletedFalse(String userId, String type);
    
    @Query("{ 'userId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'deleted': false }")
    List<Transaction> findByUserIdAndDateBetweenAndDeletedFalse(String userId, LocalDateTime start, LocalDateTime end);
    
    // Add this method
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Transaction> findAllByUserIdAndDeletedFalse(String userId);
}