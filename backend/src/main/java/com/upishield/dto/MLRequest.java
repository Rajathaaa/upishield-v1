package com.upishield.dto;

public record MLRequest(
    double amount,
    int hour,
    int transactionsLast5Min,
    double avgUserAmount,
    boolean isNewDevice,
    boolean isNewBeneficiary,
    int accountAgeDays
) {}
