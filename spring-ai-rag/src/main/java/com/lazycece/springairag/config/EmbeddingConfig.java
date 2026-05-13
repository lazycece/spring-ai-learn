package com.lazycece.springairag.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Embedding 客户端独立配置
 * <p>
 * 手动构建 Embedding API 客户端，与 Chat 客户端的自动装配完全分离。
 * 可独立配置 Embedding 的 base-url、api-key、model、embeddings-path，
 * 无需依赖 spring.ai.openai 的全局配置。
 */
@Configuration
public class EmbeddingConfig {

    /** Embedding API 地址 */
    @Value("${spring.ai.rag.embedding.base-url}")
    private String baseUrl;

    /** Embedding API 密钥 */
    @Value("${spring.ai.rag.embedding.api-key}")
    private String apiKey;

    /** Embedding 模型名称 */
    @Value("${spring.ai.rag.embedding.model}")
    private String model;

    /** Embedding API 路径（如 /v1/embeddings） */
    @Value("${spring.ai.rag.embedding.embeddings-path}")
    private String embeddingsPath;

    /**
     * 手动构建 EmbeddingModel Bean
     * <p>
     * 使用独立的 OpenAiApi 实例构建 Embedding 模型客户端，
     * 配置与 Chat 客户端完全解耦，可指向不同的 API 地址或路径。
     *
     * @return 手动构建的 EmbeddingModel 实例
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        // 1. 构建独立的 OpenAiApi 客户端
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .embeddingsPath(embeddingsPath)
                .build();

        // 2. 配置 Embedding 选项（模型名称等）
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .build();

        // 3. 构建 EmbeddingModel
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }
}
