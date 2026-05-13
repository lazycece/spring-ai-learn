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
 * 手动构建阿里云 DashScope Embedding API 客户端，与 Chat（DeepSeek）完全分离。
 * DashScope 兼容 OpenAI 接口格式，通过 compatible-mode 端点调用。
 */
@Configuration
public class EmbeddingConfig {

    /** DashScope Embedding API 地址（兼容模式） */
    @Value("${spring.ai.rag.embedding.base-url}")
    private String baseUrl;

    /** DashScope API 密钥 */
    @Value("${spring.ai.rag.embedding.api-key}")
    private String apiKey;

    /** Embedding 模型名称（如 text-embedding-v4） */
    @Value("${spring.ai.rag.embedding.model}")
    private String model;

    /** Embedding API 路径 */
    @Value("${spring.ai.rag.embedding.embeddings-path}")
    private String embeddingsPath;

    /**
     * 手动构建 EmbeddingModel Bean
     * <p>
     * 使用独立的 OpenAiApi 实例连接阿里云 DashScope Embedding 服务。
     * 通过 @Primary 覆盖 spring-ai-starter-model-openai 自动装配的 EmbeddingModel。
     *
     * @return DashScope EmbeddingModel 实例
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        // 构建独立的 OpenAiApi 客户端，指向 DashScope 兼容模式端点
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .embeddingsPath(embeddingsPath)
                .build();

        // 配置 Embedding 选项：模型名称
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .build();

        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }
}
