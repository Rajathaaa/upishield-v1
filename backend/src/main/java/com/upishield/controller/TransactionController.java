package com.upishield.controller;

import com.upishield.dto.FraudResponse;
import com.upishield.dto.TransactionRequest;
import com.upishield.entity.FraudPrediction;
import com.upishield.entity.Transaction;
import com.upishield.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@CrossOrigin(origins = "http://localhost:4200")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public FraudResponse createAndAnalyze(
            @Valid @RequestBody TransactionRequest request) {
        return service.analyze(request);
    }

    @GetMapping
    public List<Transaction> latest() {
        return service.latestTransactions();
    }

    @GetMapping("/{id}/prediction")
    public FraudPrediction prediction(@PathVariable Long id) {
        return service.getPrediction(id);
    }
}
