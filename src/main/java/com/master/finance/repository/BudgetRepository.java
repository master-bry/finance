package com.master.finance.repository;

import com.master.finance.model.Budget;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends MongoRepository<Budget, String> {
    
    // Find budget by userId and month (excluding soft deleted)
    @Query("{ 'userId': ?0, 'month': ?1, 'deleted': false }")
    Optional<Budget> findByUserIdAndMonth(String userId, String month);
    
    // Find all budgets for a user (excluding soft deleted)
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Budget> findByUserIdOrderByMonthDesc(String userId);
    
    // Find all budgets for a user (alternative method name)
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Budget> findAllByUserId(String userId);
    
    // Find budgets by year pattern
    @Query("{ 'userId': ?0, 'month': { $regex: ?1 }, 'deleted': false }")
    List<Budget> findByUserIdAndYear(String userId, String yearPattern);
    
    // Find budgets with deficit (expenses > income)
    @Query("{ 'userId': ?0, 'deleted': false, 'totalExpense': { $gt: '$totalIncome' } }")
    List<Budget> findBudgetsWithDeficit(String userId);
    
    // Find budget by specific month (without soft delete check - for internal use)
    Optional<Budget> findByUserIdAndMonthAndDeletedFalse(String userId, String month);
    
    // Check if budget exists for a month
    @Query(value = "{ 'userId': ?0, 'month': ?1, 'deleted': false }", exists = true)
    boolean existsByUserIdAndMonth(String userId, String month);
    
    // Find latest budget for a user
    @Query(value = "{ 'userId': ?0, 'deleted': false }", sort = "{ 'month': -1 }")
    Optional<Budget> findTopByUserIdOrderByMonthDesc(String userId);
}