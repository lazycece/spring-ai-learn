package com.lazycece.springairag.controller;

import com.lazycece.springairag.dto.QueryRequest;
import com.lazycece.springairag.dto.QueryResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模块化 RAG 控制器
 * <p>
 * 使用 Spring AI 2.0.0 模块化 RAG 架构（RetrievalAugmentationAdvisor）
 * 实现完整的 Pre-Retrieval → Retrieval → Post-Retrieval → Generation 管道。
 * <p>
 * 与原有的 {@link RagController} 并存，提供独立的问答端点。
 */
@RestController
@RequestMapping("/rag")
public class ModularRagController {

    /** 模块化 RAG 聊天客户端，携带完整 RAG Advisor 管道 */
    private final ChatClient modularChatClient;

    /** 向量存储（用于回退时手动获取来源文档） */
    private final VectorStore vectorStore;

    public ModularRagController(
            @Qualifier("modularChatClient") ChatClient modularChatClient,
            VectorStore vectorStore) {
        this.modularChatClient = modularChatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 模块化 RAG 问答接口
     * <p>
     * 完整的 RAG 管道流程：
     * <ol>
     *   <li>Pre-Retrieval：RewriteQueryTransformer 改写查询 + MultiQueryExpander 扩展查询</li>
     *   <li>Retrieval：VectorStoreDocumentRetriever 检索 + ConcatenationDocumentJoiner 拼接</li>
     *   <li>Post-Retrieval：DocumentPostProcessor 去重、截断、过滤</li>
     *   <li>Generation：ContextualQueryAugmenter 注入上下文 → LLM 生成回答</li>
     * </ol>
     * <p>
     * 与 /rag/query 的区别：本接口使用 Spring AI 模块化 RAG 架构，
     * 由 RetrievalAugmentationAdvisor 自动编排管道，不需要手动检索和拼接上下文。
     *
     * @param request 包含用户问题
     * @return 回答及引用的来源文档
     */
    @PostMapping("/modular-query")
    public QueryResponse modularQuery(@RequestBody QueryRequest request) {
        // 通过模块化 RAG 管道生成回答
        ChatClientResponse response = modularChatClient.prompt()
                .user(request.getQuestion())
                .call()
                .chatClientResponse();

        String answer = response.chatResponse()
                .getResult()
                .getOutput()
                .getText();

        // 尝试从 Advisor 上下文中获取检索到的来源文档
        @SuppressWarnings("unchecked")
        List<Document> documents = (List<Document>) response.context()
                .get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);

        // 回退：如果 Advisor 上下文中没有文档，手动执行向量检索获取来源
        if (documents == null || documents.isEmpty()) {
            documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(request.getQuestion())
                            .topK(request.getTopK())
                            .build());
        }

        return new QueryResponse(request.getQuestion(), answer, documents);
    }
}
