package com.upishield.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_predictions")
public class FraudPrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "transaction_id", unique = true)
    private Transaction transaction;

    private Double fraudProbability;
    private Double anomalyScore;
    private Double riskScore;
    private String riskLevel;
    private String decision;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public Double getFraudProbability() { return fraudProbability; }
    public Double getAnomalyScore() { return anomalyScore; }
    public Double getRiskScore() { return riskScore; }
    public String getRiskLevel() { return riskLevel; }
    public String getDecision() { return decision; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setTransaction(Transaction value) { this.transaction = value; }
    public void setFraudProbability(Double value) { this.fraudProbability = value; }
    public void setAnomalyScore(Double value) { this.anomalyScore = value; }
    public void setRiskScore(Double value) { this.riskScore = value; }
    public void setRiskLevel(String value) { this.riskLevel = value; }
    public void setDecision(String value) { this.decision = value; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
}
