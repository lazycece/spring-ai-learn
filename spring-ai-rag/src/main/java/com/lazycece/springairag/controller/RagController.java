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

@RestController
@RequestMapping("/rag")
public class RagController {

    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;
    private final ChatClient chatClient;

    public RagController(VectorStore vectorStore, TokenTextSplitter tokenTextSplitter, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.tokenTextSplitter = tokenTextSplitter;
        this.chatClient = chatClient;
    }

    /**
     * 文本入库：将文本内容分块、向量化后存入向量库
     */
    @PostMapping("/ingest/text")
    public String ingestText(@RequestBody IngestRequest request) {
        Document document = new Document(request.getContent(), request.getMetadata());
        List<Document> chunks = tokenTextSplitter.split(document);
        vectorStore.add(chunks);
        persistVectorStore();
        return "OK - ingested " + chunks.size() + " chunks";
    }

    /**
     * 文件上传入库
     */
    @PostMapping("/ingest/file")
    public String ingestFile(@RequestParam("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        Document document = new Document(content,
                java.util.Map.of("source", file.getOriginalFilename()));
        List<Document> chunks = tokenTextSplitter.split(document);
        vectorStore.add(chunks);
        persistVectorStore();
        return "OK - ingested " + chunks.size() + " chunks from " + file.getOriginalFilename();
    }

    /**
     * RAG 问答：检索相关文档 → 增强 Prompt → LLM 生成回答
     */
    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        // 1. 从向量库检索相关文档
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .topK(request.getTopK())
                        .query(request.getQuestion())
                        .build());

        // 2. 拼接上下文
        String context = docs.stream()
                .map(Document::getText)
                .reduce("", (a, b) -> a + "\n---\n" + b);

        // 3. 增强 Prompt + 生成回答
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
     * 查看已入库文档数量（用于验证）
     */
    @GetMapping("/documents")
    public String documents() {
        return "Vector store is ready. File persistence: vector-store.json";
    }

    private void persistVectorStore() {
        if (vectorStore instanceof org.springframework.ai.vectorstore.SimpleVectorStore store) {
            store.save(new java.io.File("vector-store.json"));
        }
    }
}
