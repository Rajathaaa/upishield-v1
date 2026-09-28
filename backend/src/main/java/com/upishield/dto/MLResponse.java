package com.upishield.dto;

public record MLResponse(
    double fraudProbability,
    double anomalyScore
) {}
