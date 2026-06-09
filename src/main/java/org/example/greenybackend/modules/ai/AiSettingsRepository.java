package org.example.greenybackend.modules.ai;

import java.util.Optional;
import org.example.greenybackend.domain.entity.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSettingsRepository extends JpaRepository<AiSettings, String> {

    Optional<AiSettings> findFirstByUserEntityUserIdAndIsActiveTrueOrderByUpdatedAtDescCreatedAtDesc(String userId);
}
