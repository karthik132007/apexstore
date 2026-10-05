package com.ecom.order;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="order_workflows")
public class OrderWorkflow {
    @Id @Column(name="order_id") public UUID orderId;
    @Column(nullable=false,length=30) public String step="RESERVE";
    @Column(name="retry_count",nullable=false) public int retryCount;
    @Column(name="next_attempt",nullable=false) public Instant nextAttempt=Instant.now();
    @Column(name="lease_until") public Instant leaseUntil;
    @Column(name="lease_token") public UUID leaseToken;
    @Column(name="last_error",length=100) public String lastError;
}
