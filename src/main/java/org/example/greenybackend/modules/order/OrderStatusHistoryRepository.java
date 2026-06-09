package org.example.greenybackend.modules.order;

import org.example.greenybackend.domain.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, String> {
}
