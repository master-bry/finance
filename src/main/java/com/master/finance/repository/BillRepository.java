package com.master.finance.repository;

import com.master.finance.model.Bill;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BillRepository extends MongoRepository<Bill, String> {
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Bill> findByUserIdAndDeletedFalse(String userId);

    @Query("{ 'userId': ?0, 'status': ?1, 'deleted': false }")
    List<Bill> findByUserIdAndStatus(String userId, String status);
}