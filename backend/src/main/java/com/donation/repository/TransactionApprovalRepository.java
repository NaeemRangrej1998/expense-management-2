package com.donation.repository;

import com.donation.entity.TransactionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionApprovalRepository extends JpaRepository<TransactionApproval, Long> {
}
