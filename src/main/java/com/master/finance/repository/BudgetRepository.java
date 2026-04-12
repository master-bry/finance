package com.master.finance.repository;

import com.master.finance.model.Budget;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends MongoRepository<Budget, String> {
    
    @Query("{ 'userId': ?0, 'month': ?1, 'deleted': false }")
    Optional<Budget> findByUserIdAndMonth(String userId, String month);
    
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Budget> findByUserIdOrderByMonthDesc(String userId);
    
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Budget> findAllByUserId(String userId);
    
    @Query("{ 'userId': ?0, 'deleted': false, 'totalExpense': { $gt: '$totalIncome' } }")
    List<Budget> findBudgetsWithDeficit(String userId);
}