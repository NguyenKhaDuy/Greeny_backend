package org.example.greenybackend.modules.payment;

import org.example.greenybackend.domain.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentsRepository extends JpaRepository<Payments, String> {
}
