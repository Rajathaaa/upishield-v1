package com.upishield.service;

import com.upishield.repository.FraudAlertRepository;
import com.upishield.repository.FraudPredictionRepository;
import com.upishield.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DashboardService {

    private final TransactionRepository transactions;
    private final FraudPredictionRepository predictions;
    private final FraudAlertRepository alerts;

    public DashboardService(
            TransactionRepository transactions,
            FraudPredictionRepository predictions,
            FraudAlertRepository alerts) {
        this.transactions = transactions;
        this.predictions = predictions;
        this.alerts = alerts;
    }

    public Map<String, Object> summary() {
        return Map.of(
                "totalTransactions", transactions.count(),
                "highRisk", predictions.countByRiskLevel("HIGH"),
                "openAlerts", alerts.countByStatus("OPEN"),
                "mediumRisk", predictions.countByRiskLevel("MEDIUM")
        );
    }
}
