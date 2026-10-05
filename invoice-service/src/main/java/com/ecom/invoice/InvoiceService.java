package com.ecom.invoice;
import com.ecom.common.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
@Service
public class InvoiceService {
    private final InvoiceRepository invoices; private final Transactions tx;
    public InvoiceService(InvoiceRepository invoices,Transactions tx) { this.invoices=invoices; this.tx=tx; }
    public InvoiceContract.Invoice generate(InvoiceContract.Request request) {
        validate(request); String hash=Hashes.sha256(InvoiceXml.request(request));
        return tx.run(() -> {
            var existing=invoices.findByOrderId(request.orderId());
            if(existing.isPresent()) { if(!existing.get().requestHash.equals(hash)) throw ApiException.conflict("Invoice data differs from original order"); return view(existing.get()); }
            Invoice i=new Invoice(); i.orderId=request.orderId(); i.requestHash=hash; i.customerName=request.customerName(); i.customerEmail=request.customerEmail(); i.address=request.address(); i.currency=request.currency(); i.total=BigDecimal.ZERO;
            for(var line:request.items()) { Invoice.Item item=new Invoice.Item(); item.productId=line.productId(); item.sellerId=line.sellerId(); item.name=line.name(); item.quantity=line.quantity(); item.unitPrice=line.unitPrice(); i.items.add(item); i.total=i.total.add(item.unitPrice.multiply(BigDecimal.valueOf(item.quantity))); }
            return view(invoices.saveAndFlush(i));
        });
    }
    private void validate(InvoiceContract.Request r) {
        if(r.orderId()==null || r.customerName()==null || r.customerName().isBlank() || r.customerName().length()>100 || r.customerEmail()==null || r.customerEmail().length()>254 || !r.customerEmail().contains("@") || r.address()==null || r.address().isBlank() || r.address().length()>1000 || !"INR".equals(r.currency()) || r.items()==null || r.items().isEmpty() || r.items().size()>50) throw ApiException.bad("Invalid invoice details");
        for(var i:r.items()) if(i.productId()==null || i.sellerId()==null || i.name()==null || i.name().isBlank() || i.name().length()>200 || i.quantity()<1 || i.quantity()>10000 || i.unitPrice()==null || i.unitPrice().signum()<=0 || i.unitPrice().scale()>2 || i.unitPrice().precision()-i.unitPrice().scale()>12) throw ApiException.bad("Invalid invoice item");
    }
    private InvoiceContract.Invoice view(Invoice i) { return new InvoiceContract.Invoice(i.id,i.invoiceNumber,i.orderId,i.customerName,i.customerEmail,i.address,i.currency,i.total,i.issuedAt,i.items.stream().map(l -> new InvoiceContract.Item(l.productId,l.sellerId,l.name,l.quantity,l.unitPrice)).toList()); }
}
