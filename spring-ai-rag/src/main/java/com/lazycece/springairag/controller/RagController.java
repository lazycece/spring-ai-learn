package com.lazycece.springairag.controller;

import com.lazycece.springairag.dto.IngestRequest;
import com.lazycece.springairag.dto.QueryRequest;
import com.lazycece.springairag.dto.QueryResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RAG 控制器
 * <p>
 * 提供文档入库和 RAG 问答的 REST API。
 * RAG 完整流程：文档读取 → 文本分块 → 向量化 → 存入向量库 → 检索 → 上下文增强 → LLM 生成回答
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    /** 向量存储，用于文档的向量化存储和相似度检索 */
    private final VectorStore vectorStore;

    /** 文本分块器，将长文档按 Token 切分为小块 */
    private final TokenTextSplitter tokenTextSplitter;

    /** 聊天客户端，负责与 LLM 交互 */
    private final ChatClient chatClient;

    public RagController(VectorStore vectorStore, TokenTextSplitter tokenTextSplitter, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.tokenTextSplitter = tokenTextSplitter;
        this.chatClient = chatClient;
    }

    /**
     * 文本入库接口
     * <p>
     * 接收文本内容，经过 ETL 管道（分块 → 向量化 → 存储）后存入向量库。
     *
     * @param request 包含文本内容和元数据
     * @return 入库的分块数量
     */
    @PostMapping("/ingest/text")
    public String ingestText(@RequestBody IngestRequest request) {
        // 1. 构建 Document 对象
        Document document = new Document(request.getContent(), request.getMetadata());
        // 2. 分块：将长文本切分为适合检索的小块
        List<Document> chunks = tokenTextSplitter.split(document);
        // 3. 存储：向量化后写入向量库
        vectorStore.add(chunks);
        // 4. 持久化到磁盘，防止重启丢失
        persistVectorStore();
        return "OK - ingested " + chunks.size() + " chunks";
    }

    /**
     * 文件上传入库接口
     * <p>
     * 接收上传的文本文件，读取内容后经过 ETL 管道存入向量库。
     *
     * @param file 上传的文件（支持 txt、md 等文本格式）
     * @return 入库的分块数量及文件名
     */
    @PostMapping("/ingest/file")
    public String ingestFile(@RequestParam("file") MultipartFile file) throws IOException {
        // 读取文件内容
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        // 构建 Document，自动记录来源文件名
        Document document = new Document(content,
                java.util.Map.of("source", file.getOriginalFilename()));
        // 分块 → 向量化 → 存储
        List<Document> chunks = tokenTextSplitter.split(document);
        vectorStore.add(chunks);
        // 持久化到磁盘
        persistVectorStore();
        return "OK - ingested " + chunks.size() + " chunks from " + file.getOriginalFilename();
    }

    /**
     * RAG 问答接口
     * <p>
     * 核心 RAG 流程：
     * <ol>
     *   <li>将用户问题向量化，在向量库中检索 topK 个最相似的文档片段</li>
     *   <li>将检索到的文档片段拼接为上下文</li>
     *   <li>将上下文注入系统 Prompt，交给 LLM 生成回答</li>
     * </ol>
     *
     * @param request 包含用户问题和检索数量
     * @return 回答及引用的来源文档
     */
    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        // 步骤1：向量检索 —— 将问题转为向量，在向量库中查找最相似的文档片段
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .topK(request.getTopK())
                        .query(request.getQuestion())
                        .build());

        // 步骤2：上下文拼接 —— 将检索到的文档片段用分隔符拼接
        String context = docs.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n---\n" + b);

        // 步骤3：增强生成 —— 将上下文注入系统 Prompt，由 LLM 基于上下文回答
        String answer = chatClient.prompt()
                .system(s -> s.text("""
                        你是一个知识库问答助手。请基于以下上下文信息回答用户问题。
                        如果上下文中没有相关信息，请如实告知，不要编造答案。

                        上下文信息：
                        {context}""")
                        .param("context", context))
                .user(request.getQuestion())
                .call()
                .content();

        return new QueryResponse(request.getQuestion(), answer, docs);
    }

    /**
     * 查看向量库状态接口
     * <p>
     * 用于验证向量库是否就绪，以及持久化文件位置。
     */
    @GetMapping("/documents")
    public String documents() {
        return "Vector store is ready. File persistence: vector-store.json";
    }

    /**
     * 将向量库数据持久化到磁盘
     * <p>
     * 写入 vector-store.json 文件，下次启动时自动加载恢复。
     */
    private void persistVectorStore() {
        if (vectorStore instanceof org.springframework.ai.vectorstore.SimpleVectorStore store) {
            store.save(new java.io.File("vector-store.json"));
        }
    }
}
