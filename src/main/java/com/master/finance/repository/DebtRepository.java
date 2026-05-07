package com.master.finance.repository;

import com.master.finance.model.Debt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtRepository extends MongoRepository<Debt, String> {

    // Non-paginated queries (used for aggregates, lists)
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Debt> findByUserIdAndDeletedFalse(String userId);

    @Query("{ 'userId': ?0, 'type': ?1, 'deleted': false }")
    List<Debt> findByUserIdAndTypeAndDeletedFalse(String userId, String type);

    @Query("{ 'userId': ?0, 'status': ?1, 'deleted': false }")
    List<Debt> findByUserIdAndStatusAndDeletedFalse(String userId, String status);

    @Query("{ 'userId': ?0, 'type': ?1, 'status': { $ne: 'SETTLED' }, 'deleted': false }")
    List<Debt> findActiveDebtsByUserIdAndTypeAndDeletedFalse(String userId, String type);

    // ========== PAGINATED QUERIES ==========
    Page<Debt> findByUserIdAndDeletedFalse(String userId, Pageable pageable);
    Page<Debt> findByUserIdAndTypeAndDeletedFalse(String userId, String type, Pageable pageable);
    Page<Debt> findByUserIdAndStatusAndDeletedFalse(String userId, String status, Pageable pageable);
    
    // ========== GROUP BY PERSON NAME ==========
    @Query("{ 'userId': ?0, 'deleted': false }")
    List<Debt> findAllDebtsGroupedByPerson(String userId);
}