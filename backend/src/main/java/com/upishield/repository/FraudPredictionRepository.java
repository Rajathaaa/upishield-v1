package com.upishield.repository;

import com.upishield.entity.FraudPrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FraudPredictionRepository extends JpaRepository<FraudPrediction, Long> {
    Optional<FraudPrediction> findByTransactionId(Long transactionId);
    List<FraudPrediction> findTop20ByOrderByCreatedAtDesc();
    long countByRiskLevel(String riskLevel);
}
