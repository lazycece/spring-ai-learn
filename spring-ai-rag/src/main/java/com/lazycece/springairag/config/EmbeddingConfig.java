package com.lazycece.springairag.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Embedding 客户端独立配置
 * <p>
 * 手动构建阿里云 DashScope Embedding API 客户端，与 Chat（DeepSeek）完全分离。
 * DashScope 兼容 OpenAI 接口格式，通过 compatible-mode 端点调用。
 * <p>
 * Spring AI 2.0 使用官方 OpenAI Java SDK（openai-java-core），
 * OpenAiApi 已移除，改为通过 OpenAiEmbeddingOptions 携带连接信息构建 OpenAiEmbeddingModel。
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

    /**
     * 手动构建 EmbeddingModel Bean
     * <p>
     * 使用独立的 API 端点连接阿里云 DashScope Embedding 服务。
     * 通过 @Primary 覆盖 spring-ai-starter-model-openai 自动装配的 EmbeddingModel。
     * <p>
     * Spring AI 2.0 中 OpenAiAi 已移除，改为通过 OpenAiEmbeddingOptions 携带
     * baseUrl / apiKey / model，由 OpenAiEmbeddingModel.Builder 内部创建 OpenAIClient。
     *
     * @return DashScope EmbeddingModel 实例
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        // Spring AI 2.0：OpenAiEmbeddingOptions 自带 baseUrl、apiKey 等连接信息
        // 不再需要单独构建 OpenAiApi，官方 SDK 自动处理 /embeddings 路径
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .model(model)
                .build();

        return OpenAiEmbeddingModel.builder()
                .options(options)
                .metadataMode(MetadataMode.EMBED)
                .build();
    }
}
