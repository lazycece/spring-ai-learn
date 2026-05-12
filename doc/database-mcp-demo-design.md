# 数据库查询 MCP Demo 设计方案

## 一、背景与目标

本项目实现一个"自然语言查询数据库"的完整闭环 Demo，用户通过自然语言提问，AI 自动调用数据库工具查询并返回答案。

---

## 二、整体架构

```
用户: "最近30天销售额最高的商品是哪些？"
        │
        ▼
┌──────────────────────────────┐
│  spring-ai-chat (MCP Client) │
│  POST /chat/ai               │
│  port: 8080                  │
│                              │
│  ChatClient                  │
│  ├─ SimpleLoggerAdvisor       │
│  ├─ MessageChatMemoryAdvisor  │
│  └─ MCP ToolCallbacks ←──┐   │
└──────────────────────────│───┘
                           │ MCP 协议 (SSE/HTTP)
                           │
┌──────────────────────────│───┐
│  spring-ai-mcp (MCP Server)  │
│  port: 8081                  │
│                              │
│  @Tool 暴露:                  │
│  ├─ listTables()              │
│  ├─ describeTable(name)       │
│  ├─ executeQuery(sql)         │
│  └─ searchRecords(table, col) │
│                              │
│  H2 内存数据库                │
│  ├─ users (用户表)            │
│  ├─ products (商品表)         │
│  └─ orders (订单表)           │
└──────────────────────────────┘
```

---

## 三、模块分工

| 模块 | 角色 | 端口 | 核心能力 |
|------|------|------|----------|
| `spring-ai-mcp` | MCP Server | 8081 | 向 AI 暴露数据库查询工具，执行 SQL 并返回结构化数据 |
| `spring-ai-chat` | MCP Client | 8080 | 通过 MCP 协议调用工具，结合 DeepSeek V4 Pro 做自然语言理解与生成 |

---

## 四、技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.13 | 应用框架 |
| Spring AI BOM | 1.1.4 | AI 集成框架 |
| DeepSeek V4 Pro | - | 大语言模型 |
| spring-ai-starter-mcp-server-webmvc | 1.1.4 | MCP Server（SSE 传输） |
| H2 Database | - | 内存数据库 |
| JDBC Template | - | 数据库访问 |

---

## 五、核心依赖

### spring-ai-mcp 模块新增依赖

```xml
<!-- MCP Server WebMVC (SSE 传输) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
<!-- H2 内存数据库 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- Spring JDBC -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

### spring-ai-chat 模块新增依赖

```xml
<!-- MCP Client -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>
```

---

## 六、数据库设计（电商场景）

### users（用户表）

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 用户名 |
| email | VARCHAR(200) | 邮箱 |
| city | VARCHAR(50) | 城市 |

### products（商品表）

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(200) | 商品名 |
| category | VARCHAR(50) | 分类 |
| price | DECIMAL(10,2) | 单价 |
| stock | INT | 库存量 |

### orders（订单表）

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| product_id | BIGINT | 商品ID |
| quantity | INT | 数量 |
| amount | DECIMAL(10,2) | 金额 |
| order_date | TIMESTAMP | 下单时间 |

---

## 七、MCP Server 暴露的工具

```java
@Tool(description = "列出数据库中所有表的名称")
public List<String> listTables() { ... }

@Tool(description = "查看指定表的结构，包含列名、类型和注释")
public List<ColumnInfo> describeTable(String tableName) { ... }

@Tool(description = "执行只读 SELECT 查询，返回结果行列表。仅允许 SELECT 语句")
public List<Map<String, Object>> executeQuery(String sql) { ... }
```

---

## 八、核心交互流程

```
1. 用户通过 POST /chat/ai 发送问题：
   {"conversationId": "s1", "userInput": "销售额最高的 5 个商品是什么？"}

2. ChatClient 将问题发送给 DeepSeek V4 Pro

3. DeepSeek 分析问题，判断需要查询数据库
   → 返回 tool call: executeQuery("SELECT p.name, SUM(o.amount) as total_sales ... GROUP BY ... ORDER BY ... LIMIT 5")

4. ChatClient 通过 MCP 协议调用 spring-ai-mcp 的 executeQuery 工具

5. spring-ai-mcp 校验 SQL（仅允许 SELECT），执行查询，返回 JSON 结果

6. ChatClient 将查询结果作为上下文再次发送给 DeepSeek

7. DeepSeek 将结构化数据整理成自然语言回答返回给用户：
   "销售额最高的 5 个商品是：1. iPhone 15 - ¥52,380 ..."

8. 对话记忆自动保存，用户可继续追问：
   "第二个商品的库存够吗？" → 无需重新指定上下文
```

---

## 九、配置项

### spring-ai-mcp application.properties

```properties
spring.application.name=spring-ai-mcp
server.port=8081

# H2 数据源
spring.datasource.url=jdbc:h2:mem:ecommerce
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

# MCP Server
spring.ai.mcp.server.name=ecommerce-db-mcp
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.type=SYNC

# 初始化脚本
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql
spring.sql.init.data-locations=classpath:data.sql
```

### spring-ai-chat application.properties (新增)

```properties
# MCP Client 连接到 MCP Server
spring.ai.mcp.client.sse.connections.ecommerce.url=http://localhost:8081
```

---

## 十、文件清单

```
spring-ai-mcp/
├── pom.xml
├── src/main/resources/
│   ├── application.properties
│   ├── schema.sql                    # 建表 DDL
│   └── data.sql                      # 示例数据 INSERT
└── src/main/java/com/lazycece/springaimcp/
    ├── SpringAiMcpApplication.java    # 入口
    ├── DatabaseToolService.java      # @Tool 注解暴露工具
    ├── ColumnInfo.java               # 列信息 DTO
    └── DataInitializer.java          # 可选：编程方式初始化

spring-ai-chat/
├── pom.xml                            # 新增 mcp-client 依赖
├── src/main/resources/
│   └── application.properties        # 新增 MCP Client 连接配置
└── src/main/java/com/lazycece/springaichat/
    └── ChatController.java           # ChatClient 注册 MCP ToolCallbacks
```

---

## 十一、测试验证

### 启动顺序

```bash
# 1. 先启动 MCP Server
cd spring-ai-mcp && mvn spring-boot:run

# 2. 再启动 Chat Client
cd spring-ai-chat && mvn spring-boot:run
```

### 对话示例

```
用户: 数据库里有哪些表？
AI:   有 users、products、orders 三张表。

用户: products 表有哪些字段？
AI:   id(产品ID)、name(名称)、category(分类)、price(价格)、stock(库存)...

用户: 帮我查一下库存不足 50 的商品有哪些？
AI:   查询到库存不足 50 的商品共 3 个：
     1. 无线耳机 - 库存 15 件
     2. 键盘 - 库存 32 件
     3. 充电宝 - 库存 8 件
```

---

## 十二、后续扩展方向

| 方向 | 说明 |
|------|------|
| 写操作支持 | 增加 insert/update tool（需权限控制） |
| 持久化数据库 | 替换 H2 为 MySQL/PostgreSQL |
| 持久化记忆 | `InMemoryChatMemory` → Redis `ChatMemory` |
| 权限控制 | 限制 SELECT 表范围、禁止敏感列 |
| 向量搜索 | 接入 spring-ai-rag 模块做语义检索 |
| 多数据源 | 支持同时连接多个数据库 |

---

> 设计日期：2026-05-13
