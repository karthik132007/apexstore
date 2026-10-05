package com.ecom.common;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
public final class InvoiceContract {
    private InvoiceContract() {}
    public record Item(UUID productId,UUID sellerId,String name,int quantity,BigDecimal unitPrice) {}
    public record Request(UUID orderId,String customerName,String customerEmail,String address,String currency,List<Item> items) {}
    public record Invoice(UUID id,String invoiceNumber,UUID orderId,String customerName,String customerEmail,String address,String currency,BigDecimal total,Instant issuedAt,List<Item> items) {}
}
