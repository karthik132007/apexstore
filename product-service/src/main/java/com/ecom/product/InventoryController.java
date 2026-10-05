package com.ecom.product;
import com.ecom.common.StockContracts.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/internal/reservations")
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service=service; }
    @PostMapping("/{id}") public Reservation reserve(@PathVariable UUID id,@Valid @RequestBody Request request) { return service.reserve(id,request); }
    @GetMapping("/{id}") public Reservation get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping("/{id}/confirm") public Reservation confirm(@PathVariable UUID id) { return service.transition(id,"CONFIRMED"); }
    @PostMapping("/{id}/release") public Reservation release(@PathVariable UUID id) { return service.transition(id,"RELEASED"); }
    @PostMapping("/{id}/cancel") public Reservation cancel(@PathVariable UUID id) { return service.transition(id,"CANCELLED"); }
}
