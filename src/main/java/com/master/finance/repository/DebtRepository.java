package com.master.finance.repository;

import com.master.finance.model.Debt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DebtRepository extends MongoRepository<Debt, String> {
    
    // Get all non-deleted debts for a user
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Debt> findByUserIdAndDeletedFalse(String userId);
    
    // Get debts by type (OWED_TO_ME or I_OWE)
    @Query("{ 'userId': ?0, 'type': ?1, 'deleted': false }")
    List<Debt> findByUserIdAndTypeAndDeletedFalse(String userId, String type);
    
    // Get debts by status
    @Query("{ 'userId': ?0, 'status': ?1, 'deleted': false }")
    List<Debt> findByUserIdAndStatusAndDeletedFalse(String userId, String status);
    
    // Get active debts (not settled) by type - THIS IS THE MISSING METHOD
    @Query("{ 'userId': ?0, 'type': ?1, 'status': { $ne: 'SETTLED' }, 'deleted': false }")
    List<Debt> findActiveDebtsByUserIdAndTypeAndDeletedFalse(String userId, String type);
    
    // Get debts by person name
    @Query("{ 'userId': ?0, 'personName': { $regex: ?1, $options: 'i' }, 'deleted': false }")
    List<Debt> findByUserIdAndPersonNameContainingIgnoreCaseAndDeletedFalse(String userId, String personName);
    
    // Get overdue debts (due date passed and not settled)
    @Query("{ 'userId': ?0, 'dueDate': { $lt: ?1 }, 'status': { $ne: 'SETTLED' }, 'deleted': false }")
    List<Debt> findOverdueDebts(String userId, java.time.LocalDateTime now);
}