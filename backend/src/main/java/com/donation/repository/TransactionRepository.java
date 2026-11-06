package com.donation.repository;

import com.donation.entity.Transaction;
import com.donation.enums.TransactionStatus;
import com.donation.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    boolean existsByTransactionNumber(String transactionNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdWithLock(@Param("id") Long id);

    // ----- Dashboard Aggregates -----

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.type = 'INCOME'
          AND t.status = 'APPROVED'
          AND (:areaIds IS NULL OR t.area.id IN :areaIds)
        """)
    BigDecimal sumApprovedIncome(@Param("areaIds") List<Long> areaIds);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.type = 'EXPENSE'
          AND t.status = 'APPROVED'
          AND (:areaIds IS NULL OR t.area.id IN :areaIds)
        """)
    BigDecimal sumApprovedExpenses(@Param("areaIds") List<Long> areaIds);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.type = 'INCOME'
          AND t.status = 'APPROVED'
          AND t.transactionDate BETWEEN :startDate AND :endDate
          AND (:areaIds IS NULL OR t.area.id IN :areaIds)
        """)
    BigDecimal sumApprovedIncomeByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("areaIds") List<Long> areaIds
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.type = 'EXPENSE'
          AND t.status = 'APPROVED'
          AND t.transactionDate BETWEEN :startDate AND :endDate
          AND (:areaIds IS NULL OR t.area.id IN :areaIds)
        """)
    BigDecimal sumApprovedExpensesByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("areaIds") List<Long> areaIds
    );

    @Query("""
        SELECT ec.name as category, COALESCE(SUM(t.amount), 0) as total
        FROM Transaction t
        JOIN t.expenseCategory ec
        WHERE t.type = 'EXPENSE'
          AND t.status = 'APPROVED'
          AND t.transactionDate BETWEEN :startDate AND :endDate
          AND (:areaIds IS NULL OR t.area.id IN :areaIds)
        GROUP BY ec.id, ec.name
        ORDER BY total DESC
        """)
    List<Object[]> sumExpensesByCategory(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("areaIds") List<Long> areaIds
    );

    // ----- Daily Income Report -----
    @Query("""
        SELECT t FROM Transaction t
        JOIN FETCH t.createdBy
        LEFT JOIN FETCH t.area
        LEFT JOIN FETCH t.incomeCategory
        WHERE t.type = 'INCOME'
          AND t.transactionDate = :date
          AND (:areaIds IS NULL OR t.area.id IN :areaIds)
          AND (:userId IS NULL OR t.createdBy.id = :userId)
          AND (:status IS NULL OR t.status = :status)
        """)
    Page<Transaction> findDailyIncomeReport(
        @Param("date") LocalDate date,
        @Param("areaIds") List<Long> areaIds,
        @Param("userId") Long userId,
        @Param("status") TransactionStatus status,
        Pageable pageable
    );

    // ----- Transaction Listing -----
    @Query("""
        SELECT t FROM Transaction t
        JOIN FETCH t.createdBy
        LEFT JOIN FETCH t.area
        LEFT JOIN FETCH t.incomeCategory
        LEFT JOIN FETCH t.expenseCategory
        WHERE t.type = :type
          AND (:areaIds IS NULL OR t.area.id IN :areaIds)
          AND (:status IS NULL OR t.status = :status)
          AND (:startDate IS NULL OR t.transactionDate >= :startDate)
          AND (:endDate IS NULL OR t.transactionDate <= :endDate)
          AND (:search IS NULL OR
               LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(COALESCE(t.referenceNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:createdById IS NULL OR t.createdBy.id = :createdById)
        """)
    Page<Transaction> findAllWithFilters(
        @Param("type") TransactionType type,
        @Param("areaIds") List<Long> areaIds,
        @Param("status") TransactionStatus status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("search") String search,
        @Param("createdById") Long createdById,
        Pageable pageable
    );

    // ----- Transaction History -----
    @Query("""
        SELECT t FROM Transaction t
        JOIN FETCH t.createdBy
        LEFT JOIN FETCH t.area
        LEFT JOIN FETCH t.incomeCategory
        LEFT JOIN FETCH t.expenseCategory
        WHERE t.status = 'APPROVED'
          AND (:type IS NULL OR t.type = :type)
          AND (:areaIds IS NULL OR t.area.id IN :areaIds)
          AND (:startDate IS NULL OR t.transactionDate >= :startDate)
          AND (:endDate IS NULL OR t.transactionDate <= :endDate)
          AND (:search IS NULL OR
               LOWER(t.transactionNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(COALESCE(t.referenceNumber, '')) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Transaction> findHistory(
        @Param("type") TransactionType type,
        @Param("areaIds") List<Long> areaIds,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("search") String search,
        Pageable pageable
    );

    // ----- Sequence for transaction number -----
    @Query("SELECT COUNT(t) + 1 FROM Transaction t WHERE YEAR(t.createdAt) = :year")
    long countByYear(@Param("year") int year);

    @Query("""
        SELECT t FROM Transaction t
        JOIN FETCH t.createdBy
        LEFT JOIN FETCH t.area
        LEFT JOIN FETCH t.incomeCategory
        LEFT JOIN FETCH t.expenseCategory
        WHERE (:type IS NULL OR t.type = :type)
          AND (:areaIds IS NULL OR t.area.id IN :areaIds)
          AND (:status IS NULL OR t.status = :status)
          AND (:startDate IS NULL OR t.transactionDate >= :startDate)
          AND (:endDate IS NULL OR t.transactionDate <= :endDate)
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findAllWithFiltersList(
        @Param("type") TransactionType type,
        @Param("areaIds") List<Long> areaIds,
        @Param("status") TransactionStatus status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
