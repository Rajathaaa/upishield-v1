package com.upishield.repository;

import com.upishield.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findTop20ByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
