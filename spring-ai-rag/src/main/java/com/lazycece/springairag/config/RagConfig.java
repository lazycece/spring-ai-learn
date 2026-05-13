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
 * 配置向量存储、文档分块器和聊天客户端三个核心 Bean。
 */
@Configuration
public class RagConfig {

    /**
     * 向量存储 Bean
     * <p>
     * 使用内存向量存储（SimpleVectorStore），启动时自动加载本地持久化文件，
     * 实现重启后数据不丢失。
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
     * 使用 Fluent API 构建，添加日志 Advisor 用于 DEBUG 级别输出请求详情。
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, VectorStore vectorStore) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
