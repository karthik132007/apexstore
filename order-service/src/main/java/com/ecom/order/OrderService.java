package com.ecom.order;
import com.ecom.common.*;
import com.ecom.common.StockContracts;
import com.ecom.common.StockContracts.Item;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {
    public record Input(@NotBlank @Size(max=100) String customerName,@NotBlank @Email @Size(max=254) String customerEmail,@NotBlank @Size(max=1000) String address,@NotEmpty @Size(max=50) List<@Valid Item> items) {}
    public record Line(UUID productId,UUID sellerId,String name,int quantity,BigDecimal unitPrice,BigDecimal subtotal) {}
    public record View(UUID id,UUID buyerId,String status,String customerName,String customerEmail,String address,String currency,BigDecimal total,Instant createdAt,boolean invoiceRequested,List<Line> items) {}
    private final OrderRepository orders; private final WorkflowRepository workflows; private final Transactions tx; private final OrderWorkflowService workflow; private final InvoiceClient invoices;
    public OrderService(OrderRepository orders,WorkflowRepository workflows,Transactions tx,OrderWorkflowService workflow,InvoiceClient invoices) { this.orders=orders; this.workflows=workflows; this.tx=tx; this.workflow=workflow; this.invoices=invoices; }
    public View place(String key,Input input) {
        if(key==null || !key.matches("[a-zA-Z0-9_-]{8,100}")) throw ApiException.bad("Idempotency-Key must contain 8–100 letters, digits, hyphens or underscores");
        var sorted=input.items().stream().sorted(Comparator.comparing(StockContracts.Item::productId)).toList();
        if(sorted.stream().map(StockContracts.Item::productId).distinct().count()!=sorted.size()) throw ApiException.bad("Duplicate product IDs are not allowed");
        UUID buyer=Actor.id();
        String hash=Hashes.sha256(input.customerName().length()+":"+input.customerName()+input.customerEmail().length()+":"+input.customerEmail()+input.address().length()+":"+input.address()+sorted);
        UUID id;
        try { id=tx.run(() -> {
            var existing=orders.findByBuyerIdAndIdempotencyKey(buyer,key);
            if(existing.isPresent()) { same(existing.get(),hash); return existing.get().id; }
            PurchaseOrder o=new PurchaseOrder(); o.buyerId=buyer; o.idempotencyKey=key; o.requestHash=hash; o.customerName=input.customerName(); o.customerEmail=input.customerEmail(); o.address=input.address();
            for(var item:sorted) { PurchaseOrder.Item line=new PurchaseOrder.Item(); line.productId=item.productId(); line.quantity=item.quantity(); o.items.add(line); }
            orders.saveAndFlush(o); OrderWorkflow w=new OrderWorkflow(); w.orderId=o.id; workflows.saveAndFlush(w); return o.id;
        }); } catch(DataIntegrityViolationException e) { PurchaseOrder existing=orders.findByBuyerIdAndIdempotencyKey(buyer,key).orElseThrow(() -> e); same(existing,hash); id=existing.id; }
        workflow.advance(id); return get(id);
    }
    private void same(PurchaseOrder o,String hash) { if(!o.requestHash.equals(hash)) throw ApiException.conflict("Idempotency-Key was already used with different order contents"); }
    public View get(UUID id) { return tx.run(() -> { PurchaseOrder o=load(id); Actor.owner(o.buyerId); return view(o); }); }
    public Pages.Result<View> list(int page,int size,String sort) { return tx.run(() -> Pages.Result.from(orders.findByBuyerId(Actor.id(),Pages.of(page,size,sort,Set.of("createdAt","total","status"))).map(OrderService::view))); }
    public View cancel(UUID id) {
        tx.run(() -> {
            PurchaseOrder o=orders.locked(id).orElseThrow(() -> ApiException.missing("Order")); Actor.owner(o.buyerId);
            if(o.status.equals("CANCELLED") || o.status.equals("CANCEL_PENDING")) return null;
            if(!o.status.equals("CONFIRMED")) throw ApiException.conflict("Only confirmed orders can be cancelled");
            if(o.invoiceRequested) throw ApiException.conflict("Cancellation is unavailable once invoice generation has started");
            OrderWorkflow w=workflows.locked(id).orElseThrow();
            if(w.leaseUntil!=null && w.leaseUntil.isAfter(Instant.now())) throw ApiException.conflict("Order processing is finishing; retry shortly");
            o.status="CANCEL_PENDING"; w.step="CANCEL"; w.nextAttempt=Instant.now(); orders.saveAndFlush(o); workflows.saveAndFlush(w); return null;
        }); workflow.advance(id); return get(id);
    }
    public InvoiceContract.Invoice invoice(UUID id,boolean generate) {
        InvoiceContract.Request request=tx.run(() -> {
            PurchaseOrder o=orders.locked(id).orElseThrow(() -> ApiException.missing("Order")); Actor.owner(o.buyerId);
            if(o.invoiceXml!=null) return null;
            if(!generate) throw ApiException.missing("Invoice");
            if(!o.status.equals("CONFIRMED")) throw ApiException.conflict("Invoice requires a confirmed order");
            o.invoiceRequested=true; orders.saveAndFlush(o);
            return new InvoiceContract.Request(o.id,o.customerName,o.customerEmail,o.address,o.currency,o.items.stream().map(i -> new InvoiceContract.Item(i.productId,i.sellerId,i.name,i.quantity,i.unitPrice)).toList());
        });
        if(request==null) return tx.run(() -> InvoiceXml.readResponse(load(id).invoiceXml));
        InvoiceContract.Invoice invoice;
        try { invoice=invoices.generate(request); }
        catch(RestClientException | ApiException e) { throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"INVOICE_UNAVAILABLE","Invoice generation is temporarily unavailable; retry this request"); }
        if(!id.equals(invoice.orderId())) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"INVALID_INVOICE","Invoice service returned an unexpected order");
        return tx.run(() -> { PurchaseOrder o=orders.locked(id).orElseThrow(); o.invoiceXml=InvoiceXml.serialize(InvoiceXml.response(invoice)); orders.saveAndFlush(o); return invoice; });
    }
    private PurchaseOrder load(UUID id) { return orders.findById(id).orElseThrow(() -> ApiException.missing("Order")); }
    static View view(PurchaseOrder o) { return new View(o.id,o.buyerId,o.status,o.customerName,o.customerEmail,o.address,o.currency,o.total,o.createdAt,o.invoiceRequested,o.items.stream().map(i -> new Line(i.productId,i.sellerId,i.name,i.quantity,i.unitPrice,i.unitPrice==null?null:i.unitPrice.multiply(BigDecimal.valueOf(i.quantity)))).toList()); }
}
