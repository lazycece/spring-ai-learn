package com.lazycece.springairag.dto;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * RAG 问答响应 DTO
 * <p>
 * 包含用户的原始问题、LLM 生成的回答，以及作为依据的检索来源文档。
 */
public class QueryResponse {

    /** 用户原始问题 */
    private String question;

    /** LLM 基于上下文生成的回答 */
    private String answer;

    /** 检索到的来源文档列表 */
    private List<SourceInfo> sources;

    public QueryResponse(String question, String answer, List<Document> documents) {
        this.question = question;
        this.answer = answer;
        // 将 Spring AI Document 转换为 SourceInfo，对外暴露文本内容和元数据
        this.sources = documents.stream()
                .map(doc -> new SourceInfo(doc.getText(), doc.getMetadata()))
                .toList();
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public List<SourceInfo> getSources() {
        return sources;
    }

    /**
     * 来源文档信息
     *
     * @param content  文档片段内容
     * @param metadata 文档元数据（如来源、标题等）
     */
    public record SourceInfo(String content, java.util.Map<String, Object> metadata) {}
}
