package org.example.greenybackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//giữ thread cho ai chạy
@Configuration
public class AiSseConfig implements WebMvcConfigurer {
	
    @Bean(name = "aiChatTaskExecutor")
    public ThreadPoolTaskExecutor aiChatTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(40);
        executor.setThreadNamePrefix("ai-chat-");
        executor.initialize();
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(120_000L);
        configurer.setTaskExecutor(aiChatTaskExecutor());
    }
}
