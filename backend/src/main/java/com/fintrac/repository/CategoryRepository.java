package com.fintrac.repository;

import com.fintrac.model.Category;
import com.fintrac.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrIsDefaultTrue(Long userId);
    List<Category> findByUserId(Long userId);
    List<Category> findByType(TransactionType type);

    @Query("SELECT c FROM Category c WHERE (c.user.id = :userId OR c.isDefault = true) AND c.type = :type")
    List<Category> findByUserIdOrDefaultAndType(@Param("userId") Long userId, @Param("type") TransactionType type);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.user.id = :userId OR c.isDefault = true)")
    Optional<Category> findAccessibleById(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.user.id = :userId")
    Optional<Category> findOwnedById(@Param("id") Long id, @Param("userId") Long userId);
}
