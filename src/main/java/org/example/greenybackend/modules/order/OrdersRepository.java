package org.example.greenybackend.modules.order;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.Orders;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<Orders, String> {

    @Override
    @EntityGraph(attributePaths = {
            "userEntity",
            "address",
            "payments",
            "shippingMethods",
            "shipments",
            "coupons",
            "orderItemsList",
            "orderItemsList.productVariant",
            "orderItemsList.productVariant.plant"
    })
    List<Orders> findAll();

    @Override
    @EntityGraph(attributePaths = {
            "userEntity",
            "address",
            "payments",
            "shippingMethods",
            "shipments",
            "coupons",
            "orderItemsList",
            "orderItemsList.productVariant",
            "orderItemsList.productVariant.plant"
    })
    Optional<Orders> findById(String orderId);

    List<Orders> findByUserEntityUserIdOrderByCreatedAtDesc(String userId);

    Optional<Orders> findByOrderIdAndUserEntityUserId(String orderId, String userId);
}
