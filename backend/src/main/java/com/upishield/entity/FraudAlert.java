package com.upishield.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
public class FraudAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "transaction_id", unique = true)
    private Transaction transaction;

    private String severity;
    private String reason;
    private String status;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public String getSeverity() { return severity; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setTransaction(Transaction value) { this.transaction = value; }
    public void setSeverity(String value) { this.severity = value; }
    public void setReason(String value) { this.reason = value; }
    public void setStatus(String value) { this.status = value; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
}
