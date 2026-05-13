# Spring AI RAG Demo 设计文档

## 概述

基于 Spring AI 1.1.4 实现一个 RAG（Retrieval Augmented Generation，检索增强生成）应用 Demo。通过将文档向量化存入向量数据库，在用户提问时检索相关上下文并增强 Prompt，使 LLM 能够基于特定知识库回答问题。

## 整体架构

```
文档上传 → ETL Pipeline（分块→向量化→存储）→ VectorStore
                                                        ↓
用户提问 → 检索相关上下文 → 增强 Prompt → LLM → 生成回答
```

## 技术选型

| 组件 | 选型 | 理由 |
|------|------|------|
| **向量存储** | `SimpleVectorStore`（内存 + 文件持久化） | 零外部依赖，适合 Demo，支持 JSON 文件持久化 |
| **Embedding 模型** | DeepSeek Embedding API | 复用已有的 DeepSeek API Key，兼容 OpenAI 接口 |
| **LLM** | DeepSeek Chat（deepseek-v4-pro） | 与现有 chat 模块保持一致 |
| **文档读取** | `spring-ai-document-reader-text` | 支持 txt/md 等文本文件 |
| **分块策略** | `TokenTextSplitter` | 按 Token 数量分块，保持语义完整性 |
| **ETL 管道** | Spring AI RAG ETL Pipeline | 内置的分块、向量化、存储流程 |

## 模块结构

```
spring-ai-rag/
└── src/main/
    ├── java/com/lazycece/springairag/
    │   ├── SpringAiRagApplication.java          # 启动类
    │   ├── controller/
    │   │   └── RagController.java               # REST API 控制器
    │   ├── config/
    │   │   └── RagConfig.java                   # RAG 相关 Bean 配置
    │   └── dto/
    │       ├── IngestRequest.java               # 入库请求 DTO
    │       └── QueryRequest.java                # 查询请求 DTO
    └── resources/
        ├── application.properties               # 应用配置
        └── documents/                           # 示例文档目录
            └── spring-ai-faq.md                 # 示例知识文档
```

## 依赖

需在 `spring-ai-rag/pom.xml` 中添加：

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring AI OpenAI Starter（提供 Embedding 和 Chat 能力） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>

    <!-- Spring AI Simple Vector Store（内存向量存储） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-vector-store-simple</artifactId>
    </dependency>

    <!-- Spring AI RAG（提供 ETL Pipeline 和检索增强） -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-rag</artifactId>
    </dependency>
</dependencies>
```

## 配置

`application.properties` 关键配置：

```properties
spring.application.name=spring-ai-rag
server.port=8082

# DeepSeek API 配置（OpenAI 兼容接口）
spring.ai.openai.api-key=${DEEPSEEK_API_KEY}
spring.ai.openai.base-url=https://api.deepseek.com
spring.ai.openai.chat.options.model=deepseek-v4-pro
spring.ai.openai.chat.options.extra-body.thinking.type=disabled

# Embedding 模型配置
spring.ai.openai.embedding.options.model=deepseek-embedding

# 日志
logging.level.org.springframework.ai.rag=DEBUG
```

## REST API 设计

### 1. 文本入库

```http
POST /rag/ingest/text
Content-Type: application/json

{
    "content": "要被检索的文档内容...",
    "metadata": {
        "source": "spring-ai-docs",
        "title": "Spring AI 入门指南"
    }
}
```

### 2. 文件上传入库

```http
POST /rag/ingest/file
Content-Type: multipart/form-data

file: spring-ai-overview.md
```

### 3. RAG 问答

```http
POST /rag/query
Content-Type: application/json

{
    "question": "Spring AI 如何配置向量存储？",
    "topK": 4
}
```

响应示例：

```json
{
    "question": "Spring AI 如何配置向量存储？",
    "answer": "Spring AI 支持多种向量存储...",
    "sources": [
        {
            "content": "相关文档片段...",
            "metadata": {"source": "spring-ai-docs"}
        }
    ]
}
```

### 4. 查看已入库文档

```http
GET /rag/documents
```

## 核心流程

### Ingest（入库流程）

```
Text/Document → DocumentReader → List<Document>
    → TokenTextSplitter（分块）
    → EmbeddingModel（向量化）
    → VectorStore（存储）
```

**代码示意：**

```java
@PostMapping("/ingest/text")
public String ingestText(@RequestBody IngestRequest request) {
    Document document = new Document(request.getContent(), request.getMetadata());
    // ETL Pipeline：分块 → 向量化 → 存储
    List<Document> chunks = tokenTextSplitter.split(document);
    vectorStore.add(chunks);
    return "Ingested " + chunks.size() + " chunks";
}
```

### Query（问答流程）

```
用户问题 → QuestionAnswerAdvisor（检索+增强）→ ChatClient → 回答
```

**代码示意：**

```java
@PostMapping("/query")
public QueryResponse query(@RequestBody QueryRequest request) {
    // 使用 QuestionAnswerAdvisor 自动完成：向量检索 → 上下文拼接 → 增强 Prompt → 生成回答
    String answer = chatClient.prompt()
            .user(request.getQuestion())
            .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults()
                    .withTopK(request.getTopK())))
            .call()
            .content();
    // ...
}
```

## 数据流图

```
┌─────────────┐     ┌─────────────────┐     ┌──────────────┐     ┌───────────┐
│  Document   │ ──▶ │ TokenTextSplitter │ ──▶ │ EmbeddingModel│ ──▶ │VectorStore│
└─────────────┘     └─────────────────┘     └──────────────┘     └───────────┘
                                                                        │
                                                                        │ similaritySearch
                                                                        ▼
┌─────────────┐     ┌─────────────────┐     ┌──────────────────────────────┐
│   Answer    │ ◀── │   ChatClient    │ ◀── │ 系统Prompt + 上下文 + 用户问题  │
└─────────────┘     └─────────────────┘     └──────────────────────────────┘
```

## 应用场景

做一个**技术文档问答助手**：预先导入 Spring AI 相关的技术文档或 FAQ，然后可以针对这些文档内容进行提问，LLM 会基于文档内容回答（而非仅靠训练数据），同时可以给出引用来源。

## 后续扩展方向

- 切换到持久化向量数据库（PgVector、Redis、Milvus 等）
- 支持更多文档格式（PDF、HTML、Json）
- 多轮对话支持（带历史记录的 RAG）
- 混合检索（关键词 + 向量）
- 检索结果重排序（Re-ranking）
- 集成 MCP Server，让其他 Agent 也能查询知识库
