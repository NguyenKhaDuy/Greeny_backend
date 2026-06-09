package org.example.greenybackend.modules.user;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByReToken(String reToken);

    boolean existsByEmail(String email);

    List<UserEntity> findByStatus(Integer status);

    List<UserEntity> findByRoleAndStatus(Integer role, Integer status);
}
