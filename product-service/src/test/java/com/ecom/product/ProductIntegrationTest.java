package com.ecom.product;
import com.ecom.common.*;
import com.ecom.common.StockContracts.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ProductIntegrationTest {
    @Autowired InventoryService inventory; @Autowired ProductRepository products; @Autowired Transactions tx; @Autowired MockMvc mvc;
    Product product(int stock) { return tx.run(() -> { Product p=new Product(); p.sellerId=UUID.randomUUID(); p.sku=UUID.randomUUID().toString(); p.name="Keyboard"; p.description="Test"; p.category="Electronics"; p.price=new BigDecimal("500.00"); p.stockOnHand=stock; return products.saveAndFlush(p); }); }
    @Test void retriesDoNotDeductOrRestoreTwice() {
        Product p=product(10); UUID order=UUID.randomUUID(); Request r=new Request(List.of(new Item(p.id,3)));
        inventory.reserve(order,r); inventory.reserve(order,r); assertThat(products.findById(p.id).orElseThrow().reserved).isEqualTo(3);
        inventory.transition(order,"CONFIRMED"); inventory.transition(order,"CONFIRMED"); assertThat(products.findById(p.id).orElseThrow().stockOnHand).isEqualTo(7);
        inventory.transition(order,"CANCELLED"); inventory.transition(order,"CANCELLED"); assertThat(products.findById(p.id).orElseThrow().stockOnHand).isEqualTo(10);
    }
    @Test void failedMultiItemReservationRollsBack() {
        Product first=product(10),second=product(0);
        assertThatThrownBy(() -> inventory.reserve(UUID.randomUUID(),new Request(List.of(new Item(first.id,2),new Item(second.id,1))))).isInstanceOf(ApiException.class);
        assertThat(products.findById(first.id).orElseThrow().reserved).isZero();
    }
    @Test void concurrentOrdersCannotOversell() throws Exception {
        Product p=product(1); var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            Callable<Boolean> buy=() -> { start.await(); try { inventory.reserve(UUID.randomUUID(),new Request(List.of(new Item(p.id,1)))); return true; } catch(RuntimeException e) { return false; } };
            var a=pool.submit(buy); var b=pool.submit(buy); start.countDown();
            assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(products.findById(p.id).orElseThrow().reserved).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void sellerCannotEditAnotherSellersProductOrUseAdminRoute() throws Exception {
        Product p=product(4); String body="{\"sku\":\"edit-test\",\"name\":\"Changed\",\"description\":\"Test\",\"category\":\"Test\",\"price\":100}";
        var stranger=jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("ROLE_SELLER"));
        mvc.perform(put("/api/seller/products/"+p.id).with(stranger).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/products").with(stranger).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/internal/reservations/"+UUID.randomUUID()).with(stranger).contentType("application/json").content("{\"items\":[]}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/products?size=1000")).andExpect(status().isBadRequest());
    }
    @Test void adminCanCreateProducts() throws Exception {
        mvc.perform(post("/api/admin/products").with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
          .contentType("application/json").content("{\"sku\":\"admin-test\",\"name\":\"Admin Product\",\"description\":\"Test\",\"category\":\"Test\",\"price\":10}"))
          .andExpect(status().isCreated());
    }
}
