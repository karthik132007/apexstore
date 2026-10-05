package com.ecom.product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="products")
public class Product {
    @Id public UUID id=UUID.randomUUID();
    @Column(name="seller_id",nullable=false) public UUID sellerId;
    @Column(nullable=false,unique=true,length=64) public String sku;
    @Column(nullable=false,length=200) public String name;
    @Column(nullable=false,length=4000) public String description;
    @Column(nullable=false,length=100) public String category;
    @Column(nullable=false,precision=19,scale=2) public BigDecimal price;
    @Column(nullable=false,length=3) public String currency="INR";
    @Column(name="image_url",length=2048) public String imageUrl;
    @Column(nullable=false) public boolean active=true;
    @Column(name="stock_on_hand",nullable=false) public int stockOnHand;
    @Column(nullable=false) public int reserved;
    @Version public long version;
    @Column(name="created_at",nullable=false) public Instant createdAt=Instant.now();
}
