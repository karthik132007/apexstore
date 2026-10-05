package com.ecom.product;
import com.ecom.common.*;
import com.ecom.common.StockContracts.*;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class InventoryService {
    private final ProductRepository products; private final ReservationRepository reservations; private final Transactions tx;
    public InventoryService(ProductRepository products,ReservationRepository reservations,Transactions tx) { this.products=products; this.reservations=reservations; this.tx=tx; }
    public Reservation reserve(UUID orderId,Request request) {
        var sorted=request.items().stream().sorted(Comparator.comparing(Item::productId)).toList();
        if(sorted.stream().map(Item::productId).distinct().count()!=sorted.size()) throw ApiException.bad("Duplicate product IDs are not allowed");
        String hash=Hashes.sha256(sorted.toString());
        return tx.run(() -> {
            var existing=reservations.locked(orderId);
            if(existing.isPresent()) { if(!existing.get().requestHash.equals(hash)) throw ApiException.conflict("Reservation request differs from the original"); return view(existing.get()); }
            StockReservation r=new StockReservation(); r.orderId=orderId; r.requestHash=hash;
            for(Item item:sorted) {
                Product p=products.locked(item.productId()).orElseThrow(() -> ApiException.missing("Product"));
                if(!p.active || p.stockOnHand-p.reserved<item.quantity()) throw ApiException.conflict("Product unavailable or insufficient stock: "+p.id);
                p.reserved+=item.quantity(); products.save(p);
                StockReservation.Line line=new StockReservation.Line(); line.productId=p.id; line.sellerId=p.sellerId; line.name=p.name; line.unitPrice=p.price; line.quantity=item.quantity(); r.items.add(line);
            }
            return view(reservations.saveAndFlush(r));
        });
    }
    public Reservation get(UUID id) { return tx.run(() -> view(reservations.findById(id).orElseThrow(() -> ApiException.missing("Reservation")))); }
    public Reservation transition(UUID id,String target) {
        return tx.run(() -> {
            StockReservation r=reservations.locked(id).orElseThrow(() -> ApiException.missing("Reservation"));
            if(r.status.equals(target)) return view(r);
            boolean confirm=target.equals("CONFIRMED") && r.status.equals("RESERVED");
            boolean release=target.equals("RELEASED") && r.status.equals("RESERVED");
            boolean cancel=target.equals("CANCELLED") && r.status.equals("CONFIRMED");
            if(!confirm && !release && !cancel) throw ApiException.conflict("Invalid reservation transition from "+r.status+" to "+target);
            for(var item:r.items.stream().sorted(Comparator.comparing(l -> l.productId)).toList()) {
                Product p=products.locked(item.productId).orElseThrow(() -> ApiException.missing("Product"));
                if(confirm) { p.stockOnHand-=item.quantity; p.reserved-=item.quantity; }
                if(release) p.reserved-=item.quantity;
                if(cancel) p.stockOnHand=Math.addExact(p.stockOnHand,item.quantity);
                products.save(p);
            }
            r.status=target; return view(reservations.saveAndFlush(r));
        });
    }
    private Reservation view(StockReservation r) { return new Reservation(r.orderId,r.status,r.items.stream().map(i -> new Line(i.productId,i.sellerId,i.name,i.unitPrice,i.quantity)).toList()); }
}
