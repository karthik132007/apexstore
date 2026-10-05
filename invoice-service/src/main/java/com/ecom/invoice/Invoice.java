package com.ecom.invoice;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="invoices")
public class Invoice {
    @Id public UUID id=UUID.randomUUID();
    @Column(name="invoice_number",nullable=false,unique=true,length=50) public String invoiceNumber="INV-"+id;
    @Column(name="order_id",nullable=false,unique=true) public UUID orderId;
    @Column(name="request_hash",nullable=false,length=64) public String requestHash;
    @Column(name="customer_name",nullable=false,length=100) public String customerName;
    @Column(name="customer_email",nullable=false,length=254) public String customerEmail;
    @Column(nullable=false,length=1000) public String address;
    @Column(nullable=false,length=3) public String currency;
    @Column(nullable=false,precision=24,scale=2) public BigDecimal total;
    @Column(name="issued_at",nullable=false) public Instant issuedAt=Instant.now();
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="invoice_items",joinColumns=@JoinColumn(name="invoice_id")) @OrderColumn(name="line_no") public List<Item> items=new ArrayList<>();
    @Embeddable public static class Item {
        @Column(name="product_id",nullable=false) public UUID productId;
        @Column(name="seller_id",nullable=false) public UUID sellerId;
        @Column(nullable=false,length=200) public String name;
        @Column(nullable=false) public int quantity;
        @Column(name="unit_price",nullable=false,precision=19,scale=2) public BigDecimal unitPrice;
    }
}
