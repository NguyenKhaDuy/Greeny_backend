package org.example.greenybackend.modules.user;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, String> {

    List<Address> findByUserEntityUserIdOrderByIsDefaultDescCreatedAtDesc(String userId);

    Optional<Address> findByAddressIdAndUserEntityUserId(String addressId, String userId);

    long countByUserEntityUserId(String userId);
}
