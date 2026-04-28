package com.fintrac.repository;

import com.fintrac.model.Category;
import com.fintrac.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrIsDefaultTrue(Long userId);
    List<Category> findByUserId(Long userId);
    List<Category> findByType(TransactionType type);

    @Query("SELECT c FROM Category c WHERE (c.user.id = :userId OR c.isDefault = true) AND c.type = :type")
    List<Category> findByUserIdOrDefaultAndType(@Param("userId") Long userId, @Param("type") TransactionType type);
}
