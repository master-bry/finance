package com.master.finance.repository;

import com.master.finance.model.Investment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvestmentRepository extends MongoRepository<Investment, String> {
    
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Investment> findByUserIdAndDeletedFalse(String userId);
    
    @Query("{ 'userId': ?0, 'status': ?1, 'deleted': false }")
    List<Investment> findByUserIdAndStatusAndDeletedFalse(String userId, String status);
    
    @Query("{ 'userId': ?0, 'type': ?1, 'deleted': false }")
    List<Investment> findByUserIdAndTypeAndDeletedFalse(String userId, String type);
}