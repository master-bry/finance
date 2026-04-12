package com.master.finance.repository;

import com.master.finance.model.Debt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DebtRepository extends MongoRepository<Debt, String> {
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Debt> findByUserIdAndDeletedFalse(String userId);
    
    @Query("{ 'userId': ?0, 'type': ?1, 'deleted': false }")
    List<Debt> findByUserIdAndTypeAndDeletedFalse(String userId, String type);
    
    @Query("{ 'userId': ?0, 'status': ?1, 'deleted': false }")
    List<Debt> findByUserIdAndStatusAndDeletedFalse(String userId, String status);
    
    @Query("{ 'userId': ?0, 'type': ?1, 'status': { $ne: 'SETTLED' }, 'deleted': false }")
    List<Debt> findActiveDebtsByUserIdAndTypeAndDeletedFalse(String userId, String type);
}