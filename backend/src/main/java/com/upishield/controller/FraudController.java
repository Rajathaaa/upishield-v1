package com.upishield.controller;

import com.upishield.entity.FraudAlert;
import com.upishield.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud")
@CrossOrigin(origins = "http://localhost:4200")
public class FraudController {

    private final TransactionService service;

    public FraudController(TransactionService service) {
        this.service = service;
    }

    @GetMapping("/alerts")
    public List<FraudAlert> alerts() {
        return service.latestAlerts();
    }
}
