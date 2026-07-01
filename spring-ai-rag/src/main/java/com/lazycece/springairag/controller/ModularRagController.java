package com.lazycece.springairag.controller;

import com.lazycece.springairag.dto.QueryRequest;
import com.lazycece.springairag.dto.QueryResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
 * 与原有的 {@link RagController} 并存，复用同一个 ChatClient Bean，
 * 通过每次请求时动态挂载 RetrievalAugmentationAdvisor 实现模块化 RAG。
 */
@RestController
@RequestMapping("/rag")
public class ModularRagController {

    /** 聊天客户端（复用 RagConfig 中的 ChatClient Bean） */
    private final ChatClient chatClient;

    /** 模块化 RAG Advisor，按请求动态挂载 */
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    /** 向量存储（用于回退时手动获取来源文档） */
    private final VectorStore vectorStore;

    public ModularRagController(ChatClient chatClient,
                                RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
                                VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.vectorStore = vectorStore;
    }

    /**
     * 模块化 RAG 问答接口
     * <p>
     * 通过 {@code .advisors(retrievalAugmentationAdvisor)} 在每次请求时
     * 动态挂载 RAG 管道，不影响原有 /rag/query 等接口的行为。
     * <p>
     * 完整的 RAG 管道流程：
     * <ol>
     *   <li>Pre-Retrieval：RewriteQueryTransformer 改写查询 + MultiQueryExpander 扩展查询</li>
     *   <li>Retrieval：VectorStoreDocumentRetriever 检索 + ConcatenationDocumentJoiner 拼接</li>
     *   <li>Post-Retrieval：DocumentPostProcessor 去重、截断、过滤</li>
     *   <li>Generation：ContextualQueryAugmenter 注入上下文 → LLM 生成回答</li>
     * </ol>
     *
     * @param request 包含用户问题
     * @return 回答及引用的来源文档
     */
    @PostMapping("/modular-query")
    public QueryResponse modularQuery(@RequestBody QueryRequest request) {
        // 复用全局 ChatClient，按请求动态挂载模块化 RAG Advisor
        ChatClientResponse response = chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)  // 仅本次请求启用 RAG 管道
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
