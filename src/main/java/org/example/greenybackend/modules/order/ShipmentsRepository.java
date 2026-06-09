package org.example.greenybackend.modules.order;

import org.example.greenybackend.domain.entity.Shipments;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentsRepository extends JpaRepository<Shipments, String> {
}
