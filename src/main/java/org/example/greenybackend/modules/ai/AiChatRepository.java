package org.example.greenybackend.modules.ai;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.AiChat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatRepository extends JpaRepository<AiChat, String> {

    Optional<AiChat> findByIdSessionAndUserEntityUserId(String idSession, String userId);

    List<AiChat> findByUserEntityUserIdOrderByUpdatedAtDesc(String userId);
}
