package org.example.greenybackend.modules.promotion;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.Coupons;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponsRepository extends JpaRepository<Coupons, String> {

    Optional<Coupons> findByCodeIgnoreCase(String code);

    List<Coupons> findAllByCodeIgnoreCase(String code);
}
