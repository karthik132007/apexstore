package com.ecom.product;
import com.ecom.common.*;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ProductService {
    public record Input(@NotBlank @Size(max=64) String sku,@NotBlank @Size(max=200) String name,@NotNull @Size(max=4000) String description,
        @NotBlank @Size(max=100) String category,@NotNull @DecimalMin("0.01") @Digits(integer=12,fraction=2) BigDecimal price,
        @Size(max=2048) @Pattern(regexp="^$|https?://[^\\s]+",message="must be an HTTP(S) URL") String imageUrl) {}
    public record Stock(@Min(0) @Max(100000000) int stockOnHand) {}
    public record View(UUID id,UUID sellerId,String sku,String name,String description,String category,BigDecimal price,String currency,String imageUrl,boolean active,int stockOnHand,int reserved,int availableStock) {}
    private final ProductRepository products; private final Transactions tx;
    public ProductService(ProductRepository products,Transactions tx) { this.products=products; this.tx=tx; }
    public View create(Input input) { return tx.run(() -> { Product p=new Product(); p.sellerId=Actor.id(); assign(p,input); return view(products.saveAndFlush(p)); }); }
    public View update(UUID id,Input input) { return tx.run(() -> { Product p=locked(id); Actor.owner(p.sellerId); assign(p,input); return view(products.saveAndFlush(p)); }); }
    public View stock(UUID id,Stock input) { return tx.run(() -> { Product p=locked(id); Actor.owner(p.sellerId); if(input.stockOnHand()<p.reserved) throw ApiException.conflict("Stock cannot be lower than reserved quantity"); p.stockOnHand=input.stockOnHand(); return view(products.saveAndFlush(p)); }); }
    public void deactivate(UUID id) { tx.run(() -> { Product p=locked(id); Actor.owner(p.sellerId); p.active=false; products.saveAndFlush(p); return null; }); }
    public View get(UUID id) { Product p=products.findById(id).orElseThrow(() -> ApiException.missing("Product")); if(!p.active) throw ApiException.missing("Product"); return view(p); }
    public Pages.Result<View> list(Pageable page,String search,String category,UUID seller,boolean all) {
        Specification<Product> spec=(root,q,b) -> {
            var terms=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if(!all) terms.add(b.isTrue(root.get("active")));
            if(seller!=null) terms.add(b.equal(root.get("sellerId"),seller));
            if(search!=null && !search.isBlank()) { String escaped=search.toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_"); terms.add(b.like(b.lower(root.get("name")),"%"+escaped+"%",'\\')); }
            if(category!=null && !category.isBlank()) terms.add(b.equal(b.lower(root.get("category")),category.toLowerCase(Locale.ROOT)));
            return b.and(terms.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return Pages.Result.from(products.findAll(spec,page).map(ProductService::view));
    }
    private Product locked(UUID id) { return products.locked(id).orElseThrow(() -> ApiException.missing("Product")); }
    private void assign(Product p,Input i) { p.sku=i.sku().trim(); p.name=i.name().trim(); p.description=i.description(); p.category=i.category().trim(); p.price=i.price(); p.imageUrl=i.imageUrl(); }
    static View view(Product p) { return new View(p.id,p.sellerId,p.sku,p.name,p.description,p.category,p.price,p.currency,p.imageUrl,p.active,p.stockOnHand,p.reserved,p.stockOnHand-p.reserved); }
}
