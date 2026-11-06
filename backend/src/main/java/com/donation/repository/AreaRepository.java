package com.donation.repository;

import com.donation.entity.Area;
import com.donation.enums.AreaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Area> findByStatus(AreaStatus status);

    @Query("""
        SELECT a FROM Area a
        WHERE (:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR a.status = :status)
        ORDER BY a.createdAt DESC
        """)
    Page<Area> findAllWithFilters(
        @Param("search") String search,
        @Param("status") AreaStatus status,
        Pageable pageable
    );
}
