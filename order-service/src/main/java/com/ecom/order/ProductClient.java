package com.ecom.order;
import com.ecom.common.StockContracts.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.util.UUID;
@Component
public class ProductClient {
    private final RestClient client;
    public ProductClient(RestClient.Builder builder,@Value("${app.product-url}") String url) { client=builder.clone().baseUrl(url).build(); }
    public Reservation reserve(UUID id,Request request) { return client.post().uri("/internal/reservations/{id}",id).body(request).retrieve().body(Reservation.class); }
    public Reservation get(UUID id) { return client.get().uri("/internal/reservations/{id}",id).retrieve().body(Reservation.class); }
    public Reservation transition(UUID id,String action) { return client.post().uri("/internal/reservations/{id}/{action}",id,action).retrieve().body(Reservation.class); }
}
