package org.example.greenybackend.modules.ai;

import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.ai.dto.AiUsageLimitDTO;

public interface AiProviderService {

    AiGenerationResult generate(UserEntity user, String question, AiContextResult context);

    AiUsageLimitDTO usageLimits(UserEntity user);

    AiUsageLimitDTO usageLimits(String provider, String model);

}
