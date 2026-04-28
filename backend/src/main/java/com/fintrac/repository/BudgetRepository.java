package com.fintrac.repository;

import com.fintrac.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserIdAndMonth(Long userId, String month);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId ORDER BY b.month DESC")
    Optional<Budget> findLatestByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(b.totalSpent), 0) FROM Budget b WHERE b.user.id = :userId AND b.month LIKE :yearMonth%")
    BigDecimal sumTotalSpentByUserIdAndMonthStartingWith(
            @Param("userId") Long userId,
            @Param("yearMonth") String yearMonth);
}
