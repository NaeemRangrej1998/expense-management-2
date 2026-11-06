package com.donation.repository;

import com.donation.entity.IncomeCategory;
import com.donation.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, Long> {
    List<IncomeCategory> findByStatusOrderByNameAsc(CategoryStatus status);
    List<IncomeCategory> findAllByOrderByNameAsc();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
