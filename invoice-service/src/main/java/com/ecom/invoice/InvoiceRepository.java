package com.ecom.invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface InvoiceRepository extends JpaRepository<Invoice,UUID> { Optional<Invoice> findByOrderId(UUID orderId); }
