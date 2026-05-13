# Spring AI 常见问题

## 什么是 Spring AI？

Spring AI 是 Spring 生态的 AI 框架，旨在简化 AI 应用的开发。它提供了与各种 AI 模型（LLM、Embedding、Image Generation 等）和向量数据库的统一抽象接口，让开发者像使用 Spring Data 一样使用 AI 能力。

Spring AI 的核心目标是：让 Java 开发者能够以熟悉的 Spring 方式构建 AI 应用，无需关心底层模型 API 的差异。

## Spring AI 支持哪些模型？

Spring AI 通过统一的 API 抽象支持多种 AI 模型：

- **OpenAI**：GPT-4o、GPT-4、GPT-3.5 等
- **Azure OpenAI**：Azure 托管的 OpenAI 模型
- **DeepSeek**：DeepSeek-V3、DeepSeek-V4 等（兼容 OpenAI 接口）
- **Ollama**：本地部署的开源模型（Llama、Mistral 等）
- **Anthropic**：Claude 系列模型
- **Google Vertex AI**：Gemini 系列模型
- **Amazon Bedrock**：AWS 托管的多种模型
- **HuggingFace**：HuggingFace 上的模型
- **MiniMax**、**Moonshot**、**ZhiPu** 等国产大模型

## Spring AI 如何实现 RAG？

RAG（Retrieval Augmented Generation，检索增强生成）是 Spring AI 的核心能力之一。实现步骤：

1. **文档读取**：使用 DocumentReader 读取各种格式的文档
2. **文档分块**：使用 TextSplitter（如 TokenTextSplitter）将长文档切分为小块
3. **向量化**：使用 EmbeddingModel 将文本块转换为向量
4. **存储**：将向量存入 VectorStore（支持多种向量数据库）
5. **检索**：用户提问时，将问题向量化后在向量库中检索相关文档
6. **增强生成**：检索到的文档作为上下文，与用户问题一起发送给 LLM 生成回答

## 什么是 ETL Pipeline？

ETL Pipeline（Extract, Transform, Load）是 Spring AI 中的文档处理管道：

- **Extract（提取）**：DocumentReader 从不同来源读取文档内容
- **Transform（转换）**：DocumentTransformer 对文档进行分块、清洗等处理
- **Load（加载）**：DocumentWriter 将处理后的文档向量化并存储到向量数据库

Spring AI 提供了 `ETLPipeline` 类来串联这三个步骤。

## Spring AI 支持哪些向量数据库？

- **SimpleVectorStore**：基于内存的向量存储，适合开发和 Demo
- **PgVector**：基于 PostgreSQL 的向量存储
- **Redis**：基于 Redis Stack 的向量存储
- **Milvus**：开源向量数据库
- **Chroma**：开源向量数据库
- **Qdrant**：高性能向量数据库
- **Weaviate**：向量数据库
- **MongoDB Atlas**：MongoDB 的向量搜索
- **Neo4j**：图数据库的向量索引
- **Elasticsearch**：基于 ES 的向量搜索
- **Cassandra**、**CosmosDB**、**Hana** 等

## 如何配置向量存储？

以 SimpleVectorStore 为例：

```java
@Bean
public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    return new SimpleVectorStore(embeddingModel);
}
```

对于生产环境，建议使用持久化的向量数据库，如 PgVector：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-pgvector</artifactId>
</dependency>
```

## 什么是 Embedding？

Embedding（嵌入/向量化）是将文本转换为高维空间中的数值向量的过程。语义相近的文本在向量空间中距离较近。Spring AI 通过 `EmbeddingModel` 接口统一了不同 Embedding 模型的调用方式。

常用的 Embedding 模型包括：
- OpenAI text-embedding-ada-002 / text-embedding-3-small
- DeepSeek Embedding
- BGE（BAAI General Embedding）
- M3E（Moka Massive Mixed Embedding）

## 如何选择分块策略？

分块策略直接影响 RAG 的检索质量：

- **TokenTextSplitter**：按 Token 数量分块，适合大多数场景
- **CharacterTextSplitter**：按字符数分块
- **ParagraphTextSplitter**：按段落分块
- **SentenceTextSplitter**：按句子分块

推荐做法：
- 一般场景：512-1024 tokens 每块
- 技术文档：256-512 tokens 每块
- 保持块间有所重叠（overlap），防止关键信息被切断

## 如何使用 ChatClient？

ChatClient 是 Spring AI 1.1.4 引入的新的 Fluent API，用于构建和发送聊天请求：

```java
String answer = chatClient.prompt()
    .system("你是一个有用的助手")
    .user("你好，请介绍一下 Spring AI")
    .advisors(new QuestionAnswerAdvisor(vectorStore))
    .call()
    .content();
```

ChatClient 支持：
- 系统提示词（system prompt）
- 用户消息（user message）
- 函数调用（function calling）
- Advisor 链（如日志记录、RAG 增强等）

## Spring AI 和 Spring Boot 的版本兼容性

Spring AI 1.1.x 适配 Spring Boot 3.5.x。使用前请确认版本匹配。Spring AI 通过 BOM（Bill of Materials）管理依赖版本，建议在项目中导入 spring-ai-bom。
