package com.lazycece.springairag.dto;

/**
 * RAG 问答请求 DTO
 * <p>
 * 用户提问时传入的问题及检索参数。
 */
public class QueryRequest {

    /** 用户问题 */
    private String question;

    /** 检索返回的最大文档片���数，默认 4 */
    private int topK = 4;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }
}
