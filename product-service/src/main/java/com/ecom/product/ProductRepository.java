package com.ecom.product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface ProductRepository extends JpaRepository<Product,UUID>,JpaSpecificationExecutor<Product> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from Product p where p.id=:id") Optional<Product> locked(@Param("id") UUID id);
}
