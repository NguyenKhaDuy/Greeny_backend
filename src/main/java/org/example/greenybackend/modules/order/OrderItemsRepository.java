package org.example.greenybackend.modules.order;

import java.util.List;
import org.example.greenybackend.domain.entity.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemsRepository extends JpaRepository<OrderItems, String> {

    List<OrderItems> findByProductVariantPlantPlantId(String plantId);
}
