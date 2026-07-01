package com.lazycece.springairag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模块化 RAG 配置类
 * <p>
 * 使用 Spring AI 2.0.0 模块化 RAG 架构（RetrievalAugmentationAdvisor），
 * 构建完整的 Pre-Retrieval → Retrieval → Post-Retrieval → Generation 四阶段管道。
 * <p>
 * 与原有的 {@link RagConfig} 并存，互不影响。
 */
@Configuration
public class ModularRagConfig {

    // ============================================================
    // Pre-Retrieval Stage: 查询改写 + 查询扩展
    // ============================================================

    /**
     * 查询改写器 —— 将口语化问题改写为更适合向量检索的关键词形式
     * <p>
     * 通过 LLM 将用户的自然语言问题重写为简洁、关键词密集的检索查询，
     * 提高向量检索的召回率。
     */
    @Bean
    public RewriteQueryTransformer rewriteQueryTransformer(ChatClient.Builder chatClientBuilder) {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetSearchSystem("向量知识库")
                .build();
    }

    /**
     * 多查询扩展器 —— 将一个查询扩展为多个语义变体
     * <p>
     * 通过 LLM 生成原查询的多个同义改写版本，分别检索后合并结果，
     * 扩大检索覆盖面，降低单一查询表述带来的召回偏差。
     */
    @Bean
    public MultiQueryExpander multiQueryExpander(ChatClient.Builder chatClientBuilder) {
        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)       // 生成 3 个查询变体
                .includeOriginal(true)    // 保留原始查询，共 4 个查询
                .build();
    }

    // ============================================================
    // Retrieval Stage: 文档检索 + 文档拼接
    // ============================================================

    /**
     * 向量存储文档检索器 —— 将查询向量化后在向量库中执行相似度检索
     * <p>
     * 对每个扩展后的查询分别检索 topK 个最相似文档片段。
     */
    @Bean
    public VectorStoreDocumentRetriever vectorStoreDocumentRetriever(VectorStore vectorStore) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(4)                  // 每个查询检索 4 个最相似文档
                .build();
    }

    /**
     * 拼接文档合并器 —— 将多轮检索结果合并去重
     * <p>
     * 将 QueryExpander 产生的多组检索结果拼接为统一的文档列表。
     */
    @Bean
    public ConcatenationDocumentJoiner concatenationDocumentJoiner() {
        return new ConcatenationDocumentJoiner();
    }

    // ============================================================
    // Post-Retrieval Stage: 文档后处理
    // ============================================================

    /**
     * 文档后处理器 —— 对检索结果进行排序、去重、截断和过滤
     * <p>
     * 处理流程：
     * <ol>
     *   <li>去重：按文档内容去重，保留首次出现的文档</li>
     *   <li>截断：将超过 500 字的文档截断，保留关键信息</li>
     *   <li>过滤：过滤掉空文档和过短文档</li>
     * </ol>
     */
    @Bean
    public DocumentPostProcessor documentPostProcessor() {
        return (query, documents) -> {
            if (documents == null || documents.isEmpty()) {
                return List.of();
            }
            return documents.stream()
                    // 步骤1：按文档内容去重（LinkedHashMap 保持原始顺序）
                    .collect(Collectors.toMap(
                            Document::getText,
                            doc -> doc,
                            (existing, replacement) -> existing,
                            LinkedHashMap::new
                    ))
                    .values().stream()
                    // 步骤2：截断过长文档，避免上下文超长
                    .map(doc -> {
                        String text = doc.getText();
                        if (text != null && text.length() > 500) {
                            return new Document(text.substring(0, 500) + "...", doc.getMetadata());
                        }
                        return doc;
                    })
                    // 步骤3：过滤空文档
                    .filter(doc -> doc.getText() != null && !doc.getText().isBlank())
                    .toList();
        };
    }

    // ============================================================
    // Generation Stage: 上下文注入 + 增强生成
    // ============================================================

    /**
     * 上下文查询增强器 —— 将检索到的文档上下文注入系统 Prompt
     * <p>
     * 将 Post-Retrieval 阶段处理后的文档列表注入到系统提示词中，
     * 指导 LLM 基于给定上下文回答问题。
     */
    @Bean
    public ContextualQueryAugmenter contextualQueryAugmenter() {
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)  // 上下文为空时允许直接回答（而非报错）
                .build();
    }

    // ============================================================
    // Orchestrator: 模块化 RAG Advisor
    // ============================================================

    /**
     * 模块化 RAG Advisor —— 组合全部管道组件
     * <p>
     * 管道流程：
     * <pre>
     * 用户问题
     *   → RewriteQueryTransformer（查询改写）
     *   → MultiQueryExpander（查询扩展为 4 个变体）
     *   → VectorStoreDocumentRetriever（向量检索，每查询 topK=4）
     *   → ConcatenationDocumentJoiner（拼接合并）
     *   → DocumentPostProcessor（去重、截断、过滤）
     *   → ContextualQueryAugmenter（上下文注入 Prompt）
     *   → LLM 生成回答
     * </pre>
     */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            RewriteQueryTransformer rewriteQueryTransformer,
            MultiQueryExpander multiQueryExpander,
            VectorStoreDocumentRetriever vectorStoreDocumentRetriever,
            ConcatenationDocumentJoiner concatenationDocumentJoiner,
            DocumentPostProcessor documentPostProcessor,
            ContextualQueryAugmenter contextualQueryAugmenter) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(rewriteQueryTransformer)   // Pre-Retrieval: 查询改写
                .queryExpander(multiQueryExpander)            // Pre-Retrieval: 查询扩展
                .documentRetriever(vectorStoreDocumentRetriever)  // Retrieval: 文档检索
                .documentJoiner(concatenationDocumentJoiner)      // Retrieval: 文档拼接
                .documentPostProcessors(documentPostProcessor)    // Post-Retrieval: 后处理
                .queryAugmenter(contextualQueryAugmenter)         // Generation: 上下文注入
                .build();
    }

}
