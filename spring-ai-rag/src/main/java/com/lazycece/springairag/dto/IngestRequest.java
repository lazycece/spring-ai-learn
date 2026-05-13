package com.lazycece.springairag.dto;

import java.util.Map;

/**
 * 文档入库请求 DTO
 * <p>
 * 用于接收需要存入向量库的文本内容及其元数据信息。
 */
public class IngestRequest {

    /** 文档文本内容 */
    private String content;

    /** 文档元数据（如来源、标题、作者等），可选 */
    private Map<String, Object> metadata;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
