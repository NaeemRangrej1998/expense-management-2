package com.donation.repository;

import com.donation.entity.User;
import com.donation.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN u.roles r
        LEFT JOIN u.areas a
        WHERE (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR u.status = :status)
        ORDER BY u.createdAt DESC
        """)
    Page<User> findAllWithFilters(
        @Param("search") String search,
        @Param("status") UserStatus status,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(u) FROM User u
        JOIN u.roles r
        WHERE r.name = 'Super Admin' AND u.status = 'ACTIVE'
        """)
    long countActiveSuperAdmins();
}
