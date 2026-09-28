package com.upishield.dto;

public record FraudResponse(
    Long transactionId,
    double fraudProbability,
    double anomalyScore,
    double riskScore,
    String riskLevel,
    String decision
) {}
