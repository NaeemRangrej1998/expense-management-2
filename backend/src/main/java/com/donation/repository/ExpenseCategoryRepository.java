package com.donation.repository;

import com.donation.entity.ExpenseCategory;
import com.donation.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {
    List<ExpenseCategory> findByStatusOrderByNameAsc(CategoryStatus status);
    List<ExpenseCategory> findAllByOrderByNameAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
