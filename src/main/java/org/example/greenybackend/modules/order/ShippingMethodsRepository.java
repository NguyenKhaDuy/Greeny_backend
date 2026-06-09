package org.example.greenybackend.modules.order;

import java.util.Optional;
import org.example.greenybackend.domain.entity.ShippingMethods;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingMethodsRepository extends JpaRepository<ShippingMethods, String> {

    Optional<ShippingMethods> findFirstByIsActiveTrueOrderBySortOrderAsc();
}
