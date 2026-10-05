package com.ecom.product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface ReservationRepository extends JpaRepository<StockReservation,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from StockReservation r where r.orderId=:id") Optional<StockReservation> locked(@Param("id") UUID id);
}
