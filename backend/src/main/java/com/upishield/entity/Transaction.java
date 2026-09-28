package com.upishield.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Double amount;

    private String merchantId;
    private String deviceId;
    private Integer hour;
    private Integer transactionsLast5Min;
    private Double avgUserAmount;
    private Boolean newDevice;
    private Boolean newBeneficiary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Transaction() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Double getAmount() { return amount; }
    public String getMerchantId() { return merchantId; }
    public String getDeviceId() { return deviceId; }
    public Integer getHour() { return hour; }
    public Integer getTransactionsLast5Min() { return transactionsLast5Min; }
    public Double getAvgUserAmount() { return avgUserAmount; }
    public Boolean getNewDevice() { return newDevice; }
    public Boolean getNewBeneficiary() { return newBeneficiary; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUser(User user) { this.user = user; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setHour(Integer hour) { this.hour = hour; }
    public void setTransactionsLast5Min(Integer value) { this.transactionsLast5Min = value; }
    public void setAvgUserAmount(Double value) { this.avgUserAmount = value; }
    public void setNewDevice(Boolean value) { this.newDevice = value; }
    public void setNewBeneficiary(Boolean value) { this.newBeneficiary = value; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
}
