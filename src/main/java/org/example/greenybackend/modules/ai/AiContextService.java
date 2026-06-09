package org.example.greenybackend.modules.ai;

import org.example.greenybackend.domain.entity.UserEntity;

public interface AiContextService {

    AiContextResult buildContext(UserEntity user, String question);

}
