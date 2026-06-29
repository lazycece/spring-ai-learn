package com.lazycece.springaitoolcalling.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lazycece
 * @date 2026/6/29
 */
@Configuration
public class ChatConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {

        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

}
