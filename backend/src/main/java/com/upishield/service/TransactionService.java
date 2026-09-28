package com.upishield.service;

import com.upishield.dto.*;
import com.upishield.entity.*;
import com.upishield.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FraudPredictionRepository predictionRepository;
    private final FraudAlertRepository alertRepository;
    private final MLServiceClient mlServiceClient;

    public TransactionService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            FraudPredictionRepository predictionRepository,
            FraudAlertRepository alertRepository,
            MLServiceClient mlServiceClient) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.predictionRepository = predictionRepository;
        this.alertRepository = alertRepository;
        this.mlServiceClient = mlServiceClient;
    }

    @Transactional
    public FraudResponse analyze(TransactionRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setAmount(request.getAmount());
        tx.setMerchantId(request.getMerchantId());
        tx.setDeviceId(request.getDeviceId());
        tx.setHour(request.getHour());
        tx.setTransactionsLast5Min(request.getTransactionsLast5Min());
        tx.setAvgUserAmount(request.getAvgUserAmount());
        tx.setNewDevice(request.getNewDevice());
        tx.setNewBeneficiary(request.getNewBeneficiary());
        tx.setCreatedAt(LocalDateTime.now());

        tx = transactionRepository.save(tx);

        // V1: keep account age simple. Later this can be calculated from account data.
        MLRequest mlRequest = new MLRequest(
                tx.getAmount(),
                tx.getHour(),
                tx.getTransactionsLast5Min(),
                tx.getAvgUserAmount(),
                Boolean.TRUE.equals(tx.getNewDevice()),
                Boolean.TRUE.equals(tx.getNewBeneficiary()),
                365
        );

        MLResponse ml = mlServiceClient.predict(mlRequest);

        double riskScore =
                0.7 * ml.fraudProbability()
                + 0.3 * ml.anomalyScore();

        String riskLevel;
        String decision;

        if (riskScore >= 0.70) {
            riskLevel = "HIGH";
            decision = "FLAG";
        } else if (riskScore >= 0.40) {
            riskLevel = "MEDIUM";
            decision = "REVIEW";
        } else {
            riskLevel = "LOW";
            decision = "ALLOW";
        }

        FraudPrediction prediction = new FraudPrediction();
        prediction.setTransaction(tx);
        prediction.setFraudProbability(ml.fraudProbability());
        prediction.setAnomalyScore(ml.anomalyScore());
        prediction.setRiskScore(riskScore);
        prediction.setRiskLevel(riskLevel);
        prediction.setDecision(decision);
        prediction.setCreatedAt(LocalDateTime.now());
        predictionRepository.save(prediction);

        if ("HIGH".equals(riskLevel)) {
            FraudAlert alert = new FraudAlert();
            alert.setTransaction(tx);
            alert.setSeverity("HIGH");
            alert.setReason(buildReason(tx, ml));
            alert.setStatus("OPEN");
            alert.setCreatedAt(LocalDateTime.now());
            alertRepository.save(alert);
        }

        return new FraudResponse(
                tx.getId(),
                ml.fraudProbability(),
                ml.anomalyScore(),
                riskScore,
                riskLevel,
                decision
        );
    }

    private String buildReason(Transaction tx, MLResponse ml) {
        StringBuilder reason = new StringBuilder();

        if (tx.getAmount() > tx.getAvgUserAmount() * 5) {
            reason.append("Unusually high amount; ");
        }
        if (Boolean.TRUE.equals(tx.getNewDevice())) {
            reason.append("new device; ");
        }
        if (Boolean.TRUE.equals(tx.getNewBeneficiary())) {
            reason.append("new beneficiary; ");
        }
        if (tx.getTransactionsLast5Min() >= 5) {
            reason.append("high transaction velocity; ");
        }
        if (tx.getHour() <= 4 || tx.getHour() >= 23) {
            reason.append("unusual transaction hour; ");
        }
        if (reason.isEmpty()) {
            reason.append("high model risk score; ");
        }

        return reason.toString();
    }

    public List<Transaction> latestTransactions() {
        return transactionRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public List<FraudAlert> latestAlerts() {
        return alertRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public FraudPrediction getPrediction(Long transactionId) {
        return predictionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Prediction not found"));
    }
}
