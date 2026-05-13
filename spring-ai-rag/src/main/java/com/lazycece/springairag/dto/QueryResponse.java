package com.lazycece.springairag.dto;

import org.springframework.ai.document.Document;

import java.util.List;

public class QueryResponse {

    private String question;
    private String answer;
    private List<SourceInfo> sources;

    public QueryResponse(String question, String answer, List<Document> documents) {
        this.question = question;
        this.answer = answer;
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

    public record SourceInfo(String content, java.util.Map<String, Object> metadata) {}
}
