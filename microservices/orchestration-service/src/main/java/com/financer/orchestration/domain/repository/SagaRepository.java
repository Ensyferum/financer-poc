package com.financer.orchestration.domain.repository;

import com.financer.orchestration.domain.model.Saga;
import com.financer.orchestration.domain.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Saga entities
 * 
 * @author Financer Team
 * @version 1.0.0
 */
@Repository
public interface SagaRepository extends JpaRepository<Saga, String> {

    /**
     * Find saga by business key
     */
    Optional<Saga> findByBusinessKey(String businessKey);

    /**
     * Find saga by correlation ID
     */
    Optional<Saga> findByCorrelationId(String correlationId);

    /**
     * Find sagas by status
     */
    List<Saga> findByStatus(SagaStatus status);

    /**
     * Find sagas by saga type
     */
    List<Saga> findBySagaType(String sagaType);

    /**
     * Find sagas by status and saga type
     */
    List<Saga> findByStatusAndSagaType(SagaStatus status, String sagaType);

    /**
     * Find active sagas (not in terminal state)
     */
    @Query("SELECT s FROM Saga s WHERE s.status NOT IN ('COMPLETED', 'COMPENSATED', 'ABORTED')")
    List<Saga> findActiveSagas();

    /**
     * Find sagas that need compensation
     */
    @Query("SELECT s FROM Saga s WHERE s.status = 'FAILED' AND s.retryCount < s.maxRetryAttempts")
    List<Saga> findSagasNeedingCompensation();

    /**
     * Find sagas started before a specific time
     */
    @Query("SELECT s FROM Saga s WHERE s.startedAt < :beforeTime AND s.status NOT IN ('COMPLETED', 'COMPENSATED', 'ABORTED')")
    List<Saga> findStuckSagas(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Find sagas by status and started after a specific time
     */
    @Query("SELECT s FROM Saga s WHERE s.status = :status AND s.startedAt >= :afterTime")
    List<Saga> findByStatusAndStartedAfter(@Param("status") SagaStatus status, 
                                          @Param("afterTime") LocalDateTime afterTime);

    /**
     * Count sagas by status
     */
    long countByStatus(SagaStatus status);

    /**
     * Count sagas by saga type and status
     */
    long countBySagaTypeAndStatus(String sagaType, SagaStatus status);

    /**
     * Check if saga exists by business key
     */
    boolean existsByBusinessKey(String businessKey);

    /**
     * Find sagas for monitoring (with basic info)
     */
    @Query("SELECT s.id, s.businessKey, s.sagaType, s.status, s.startedAt, s.completedAt " +
           "FROM Saga s ORDER BY s.startedAt DESC")
    List<Object[]> findSagasForMonitoring();
}