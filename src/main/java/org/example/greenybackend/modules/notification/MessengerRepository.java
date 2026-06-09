package org.example.greenybackend.modules.notification;

import java.util.List;
import org.example.greenybackend.domain.entity.Messenger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessengerRepository extends JpaRepository<Messenger, String> {

    List<Messenger> findByUserEntityUserIdAndExtractedDataOrderByCreatedAtAsc(String userId, String extractedData);

    List<Messenger> findTop12ByUserEntityUserIdOrderByCreatedAtDesc(String userId);

    void deleteByUserEntityUserIdAndExtractedData(String userId, String extractedData);
}
