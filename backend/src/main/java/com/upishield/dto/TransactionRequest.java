package com.upishield.dto;

import jakarta.validation.constraints.*;

public class TransactionRequest {
    @NotNull
    private Long userId;

    @NotNull
    @Positive
    private Double amount;

    @NotBlank
    private String merchantId;

    @NotBlank
    private String deviceId;

    @Min(0)
    @Max(23)
    private Integer hour = 12;

    @Min(0)
    private Integer transactionsLast5Min = 0;

    @Positive
    private Double avgUserAmount = 1000.0;

    private Boolean newDevice = false;
    private Boolean newBeneficiary = false;

    public Long getUserId() { return userId; }
    public Double getAmount() { return amount; }
    public String getMerchantId() { return merchantId; }
    public String getDeviceId() { return deviceId; }
    public Integer getHour() { return hour; }
    public Integer getTransactionsLast5Min() { return transactionsLast5Min; }
    public Double getAvgUserAmount() { return avgUserAmount; }
    public Boolean getNewDevice() { return newDevice; }
    public Boolean getNewBeneficiary() { return newBeneficiary; }

    public void setUserId(Long v) { userId = v; }
    public void setAmount(Double v) { amount = v; }
    public void setMerchantId(String v) { merchantId = v; }
    public void setDeviceId(String v) { deviceId = v; }
    public void setHour(Integer v) { hour = v; }
    public void setTransactionsLast5Min(Integer v) { transactionsLast5Min = v; }
    public void setAvgUserAmount(Double v) { avgUserAmount = v; }
    public void setNewDevice(Boolean v) { newDevice = v; }
    public void setNewBeneficiary(Boolean v) { newBeneficiary = v; }
}
