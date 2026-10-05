package com.ecom.user;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
@Entity @Table(name="users")
public class User {
    @Id public UUID id=UUID.randomUUID();
    @Column(nullable=false,unique=true,length=254) public String email;
    @Column(nullable=false,length=100) public String name;
    @Column(name="password_hash",nullable=false) public String passwordHash;
    @Column(nullable=false) public boolean enabled=true;
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="user_roles",joinColumns=@JoinColumn(name="user_id")) @Column(name="role",length=20)
    public Set<String> roles=new HashSet<>();
    @Column(name="created_at",nullable=false) public Instant createdAt=Instant.now();
}
