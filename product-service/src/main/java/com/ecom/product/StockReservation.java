package com.ecom.product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="stock_reservations")
public class StockReservation {
    @Id @Column(name="order_id") public UUID orderId;
    @Column(nullable=false,length=20) public String status="RESERVED";
    @Column(name="request_hash",nullable=false,length=64) public String requestHash;
    @Column(name="created_at",nullable=false) public Instant createdAt=Instant.now();
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="stock_reservation_items",joinColumns=@JoinColumn(name="order_id")) @OrderColumn(name="line_no")
    public List<Line> items=new ArrayList<>();
    @Embeddable public static class Line {
        @Column(name="product_id",nullable=false) public UUID productId;
        @Column(name="seller_id",nullable=false) public UUID sellerId;
        @Column(nullable=false,length=200) public String name;
        @Column(name="unit_price",nullable=false,precision=19,scale=2) public BigDecimal unitPrice;
        @Column(nullable=false) public int quantity;
    }
}
