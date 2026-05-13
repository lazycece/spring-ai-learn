package com.lazycece.springairag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 核心组件配置类
 * <p>
 * Chat 客户端由 spring-ai-starter-model-openai 自动装配（DeepSeek API），
 * Embedding 客户端由 {@link EmbeddingConfig} 手动构建（阿里云 DashScope API），
 * 二者完全独立、互不影响。此处仅配置向量存储、文档分块器和聊天外观 Bean。
 */
@Configuration
public class RagConfig {

    /**
     * 向量存储 Bean
     * <p>
     * 使用内存向量存储（SimpleVectorStore），注入 EmbeddingConfig 手动构建的
     * 阿里云 DashScope EmbeddingModel，启动时自动加载持久化文件，重启后数据不丢失。
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        // 启动时尝试加载已有的向量数据，实现持久化恢复
        java.io.File file = new java.io.File("vector-store.json");
        if (file.exists()) {
            store.load(file);
        }
        return store;
    }

    /**
     * 文档分块器 Bean
     * <p>
     * 按 Token 数量将长文档切分为小块，便于向量化和精准检索。
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder().build();
    }

    /**
     * 聊天客户端 Bean
     * <p>
     * ChatModel 由 spring-ai-starter-model-openai 自动装配（基于 spring.ai.openai.* 配置），
     * ChatClient 在此手动构建，对自动装配的 ChatModel 进行包装，添加日志 Advisor。
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
