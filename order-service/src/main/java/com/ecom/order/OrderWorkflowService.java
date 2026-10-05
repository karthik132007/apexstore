package com.ecom.order;
import com.ecom.common.*;
import com.ecom.common.StockContracts.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Persisted orchestration. Remote operations are idempotent and never run inside local transactions. */
@Service
public class OrderWorkflowService {
    private final OrderRepository orders; private final WorkflowRepository workflows; private final Transactions tx; private final ProductClient products;
    public OrderWorkflowService(OrderRepository orders,WorkflowRepository workflows,Transactions tx,ProductClient products) { this.orders=orders; this.workflows=workflows; this.tx=tx; this.products=products; }
    public void advance(UUID id) {
        UUID token=claim(id); if(token==null) return;
        String error=null;
        try {
            PurchaseOrder order=tx.run(() -> orders.findById(id).orElseThrow());
            if(order.status.equals("CANCEL_PENDING")) { products.transition(id,"cancel"); finish(id,token,"CANCELLED"); return; }
            if(!order.status.equals("PENDING")) return;
            Reservation reservation;
            try { reservation=products.reserve(id,new Request(order.items.stream().map(i -> new Item(i.productId,i.quantity)).toList())); }
            catch(HttpClientErrorException rejected) {
                int code=rejected.getStatusCode().value();
                if(code!=400 && code!=404 && code!=409) throw rejected;
                // A duplicate/ambiguous response may hide a successful reservation. Check before failing.
                try { reservation=products.get(id); }
                catch(HttpClientErrorException.NotFound absent) { finish(id,token,"FAILED"); return; }
            }
            if(!Set.of("RESERVED","CONFIRMED").contains(reservation.status())) { finish(id,token,"FAILED"); return; }
            Reservation snapshot=reservation;
            tx.run(() -> {
                PurchaseOrder o=orders.locked(id).orElseThrow(); OrderWorkflow w=owned(id,token);
                if(!o.status.equals("PENDING")) return null;
                o.total=BigDecimal.ZERO;
                Map<UUID,Line> lines=new HashMap<>(); snapshot.items().forEach(i -> lines.put(i.productId(),i));
                for(var item:o.items) {
                    Line line=lines.get(item.productId);
                    if(line==null || line.quantity()!=item.quantity) throw new IllegalStateException("Reservation differs from order");
                    item.name=line.name(); item.sellerId=line.sellerId(); item.unitPrice=line.unitPrice(); o.total=o.total.add(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
                }
                w.step="CONFIRM"; orders.saveAndFlush(o); workflows.saveAndFlush(w); return null;
            });
            if(!reservation.status().equals("CONFIRMED")) products.transition(id,"confirm");
            finish(id,token,"CONFIRMED");
        } catch(RuntimeException e) {
            error=e instanceof RestClientResponseException http?"HTTP_"+http.getStatusCode().value():e.getClass().getSimpleName();
        } finally {
            String failure=error;
            tx.run(() -> {
                var found=workflows.locked(id); if(found.isEmpty()) return null; OrderWorkflow w=found.get();
                if(!token.equals(w.leaseToken)) return null;
                w.leaseToken=null; w.leaseUntil=null;
                if(failure!=null) { w.retryCount++; w.lastError=failure; w.nextAttempt=Instant.now().plusSeconds(Math.min(300,5L << Math.min(w.retryCount,5))); }
                workflows.saveAndFlush(w); return null;
            });
        }
    }
    private UUID claim(UUID id) {
        return tx.run(() -> {
            var found=workflows.locked(id); if(found.isEmpty()) return null; OrderWorkflow w=found.get();
            if(w.step.equals("DONE") || (w.leaseUntil!=null && w.leaseUntil.isAfter(Instant.now()))) return null;
            w.leaseToken=UUID.randomUUID(); w.leaseUntil=Instant.now().plusSeconds(90); workflows.saveAndFlush(w); return w.leaseToken;
        });
    }
    private OrderWorkflow owned(UUID id,UUID token) { OrderWorkflow w=workflows.locked(id).orElseThrow(); if(!token.equals(w.leaseToken)) throw new IllegalStateException("Workflow lease lost"); return w; }
    private void finish(UUID id,UUID token,String status) {
        tx.run(() -> { PurchaseOrder o=orders.locked(id).orElseThrow(); OrderWorkflow w=owned(id,token); o.status=status; w.step="DONE"; w.lastError=null; orders.saveAndFlush(o); workflows.saveAndFlush(w); return null; });
    }
    @Scheduled(fixedDelayString="${app.recovery-delay:15000}",initialDelayString="${app.recovery-delay:15000}")
    public void recover() { for(UUID id:workflows.due(Instant.now(),PageRequest.of(0,20))) { try { advance(id); } catch(RuntimeException e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Recovery deferred orderId={} type={}",id,e.getClass().getSimpleName()); } } }
}
