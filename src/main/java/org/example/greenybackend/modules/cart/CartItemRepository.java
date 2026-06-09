package org.example.greenybackend.modules.cart;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, String> {

    List<CartItem> findByUserEntityUserIdOrderByCreateAtDesc(String userId);

    Optional<CartItem> findByIdCartItemAndUserEntityUserId(String cartItemId, String userId);

    Optional<CartItem> findByUserEntityUserIdAndProductVariantVariantId(String userId, String variantId);

    void deleteByUserEntityUserId(String userId);
}
