package com.fintrac.repository;

import com.fintrac.model.Alert;
import com.fintrac.model.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Alert> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, AlertType type);

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.user.id = :userId AND a.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.user.id = :userId AND a.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);
}
