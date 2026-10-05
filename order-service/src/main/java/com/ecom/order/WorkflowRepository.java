package com.ecom.order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.*;
public interface WorkflowRepository extends JpaRepository<OrderWorkflow,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from OrderWorkflow w where w.orderId=:id") Optional<OrderWorkflow> locked(@Param("id") UUID id);
    @Query("select w.orderId from OrderWorkflow w where w.step<>'DONE' and w.nextAttempt<=:now and (w.leaseUntil is null or w.leaseUntil<:now) order by w.nextAttempt") List<UUID> due(@Param("now") Instant now,Pageable limit);
}
