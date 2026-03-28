# AI-Entity-Code-Generator

[English](./README.md) | [中文](./README-zh.md)

## 概述

AI-Entity-Code-Generator 是一个基于 Spring Boot 的应用程序，利用大语言模型（LLM）从上传的 schema 文件自动生成各种编程语言的实体代码。

### 使用场景

- **Schema 转 Entity**：上传 JSON/YAML/Excel/CSV 格式的数据库 schema 文件，生成对应的实体类
- **跨语言迁移**：将实体定义从一种语言转换为另一种语言（例如 Java POJO 转 Go struct）
- **快速原型**：为新项目快速生成实体代码模板
- **学习工具**：了解不同语言的实体结构

### 核心功能

- 支持上传多种类型文件：纯文本（JSON、YAML 等）、Excel（.xls、.xlsx）、CSV
- 支持多种目标语言（Java、Go）
- OpenAI 兼容 API 集成
- 支持文件下载的 RESTful API
- 简洁的 Web UI 界面
- Assembly 打包便于部署

## 技术详情

### 架构设计

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   前端      │────▶│   控制器          │────▶│    服务层        │
│  (静态      │     │  (Spring MVC)    │     │ (业务            │
│   HTML/JS)  │     │                  │     │  逻辑)          │
└─────────────┘     └──────────────────┘     └────────┬────────┘
                                                     │
                                                     ▼
                                            ┌─────────────────┐
                                            │  LLM 客户端     │
                                            │ (OpenAI        │
                                            │  兼容)         │
                                            └────────┬────────┘
                                                     │
                                                     ▼
                                            ┌─────────────────┐
                                            │ 外部 LLM        │
                                            │ API            │
                                            └─────────────────┘
```

### 项目结构

```
ai-entity-code-generator/
├── src/
│   ├── main/
│   │   ├── java/org/example/demotest/
│   │   │   ├── config/           # 配置类
│   │   │   │   ├── LlmHttpClientConfig.java
│   │   │   │   └── LlmProperties.java
│   │   │   ├── controller/       # REST 控制器
│   │   │   │   └── EntityGenerationController.java
│   │   │   ├── dto/             # 数据传输对象
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   └── GeneratedCodeFile.java
│   │   │   ├── exception/       # 异常处理
│   │   │   │   ├── AppException.java
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── ConfigurationException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── UpstreamServiceException.java
│   │   │   ├── llm/             # LLM 集成
│   │   │   │   ├── OpenAiCompatibleLlmClient.java
│   │   │   │   └── dto/
│   │   │   │       ├── LlmChatMessage.java
│   │   │   │       ├── LlmChatRequest.java
│   │   │   │       └── LlmChatResponse.java
│   │   │   ├── service/          # 业务服务
│   │   │   │   └── EntityCodeGenerationService.java
│   │   │   └── DemoTestApplication.java
│   │   └── resources/
│   │       ├── application.yml   # 应用配置
│   │       └── static/           # 静态 Web 资源
│   │           └── index.html   # Web UI
│   └── test/
│       └── java/                 # 单元测试
├── pom.xml
└── src/assembly/
    └── assembly.xml              # Assembly 打包描述符
```

### 技术栈

| 组件 | 技术 |
|-----------|------------|
| 框架 | Spring Boot 2.7.18 |
| 语言 | Java 8 |
| 构建工具 | Apache Maven 3.9.x |
| HTTP 客户端 | Spring RestTemplate |
| 验证 | Spring Boot Validation |
| 测试 | JUnit 5, Spring Boot Test, Mockito |
| 打包 | Maven Assembly Plugin |

## API 规范

### 实体代码生成

#### POST /api/entities/generate

从上传的文件生成实体代码。

**Content-Type:** `multipart/form-data`

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----------|------|------|-------------|
| file | MultipartFile | 是 | 包含实体/schema 定义的文本文件 |
| language | String | 是 | 代码生成的目标语言（java/go） |

**响应：**

- **成功 (200 OK):**
  - Content-Type: `text/plain`
  - Content-Disposition: `attachment; filename=GeneratedEntity.java` 或 `generated_entity.go`
  - Body: 生成的实体代码（纯文本）

- **错误 (400 Bad Request):**
  ```json
  {
    "error": "Bad Request",
    "message": "上传文件为空",
    "path": "/api/entities/generate",
    "timestamp": 1700000000000
  }
  ```

- **错误 (500 Internal Server Error):**
  ```json
  {
    "error": "Internal Server Error",
    "message": "调用LLM服务失败: ...",
    "path": "/api/entities/generate",
    "timestamp": 1700000000000
  }
  ```

### 错误响应结构

| 字段 | 类型 | 说明 |
|-------|------|-------------|
| error | String | HTTP 状态描述 |
| message | String | 人类可读的错误消息 |
| path | String | 发生错误的请求路径 |
| timestamp | Long | Unix 时间戳（毫秒） |

## 使用指南

### 快速开始

1. **配置 LLM API Key：**
   ```bash
   export LLM_API_KEY=your_api_key_here
   ```

2. **运行应用：**
   ```bash
   mvn spring-boot:run
   ```

3. **访问 Web UI：**
   浏览器打开：`http://localhost:8080`

4. **或直接使用 API：**
   ```bash
   curl -X POST "http://localhost:8080/api/entities/generate" \
     -F "file=@schema.json" \
     -F "language=java" \
     -o GeneratedEntity.java
   ```

### 使用 Web UI

1. 在浏览器中打开 `http://localhost:8080`
2. 点击"选择文件"按钮，选择包含实体 schema 的文本文件
3. 从下拉菜单选择目标语言（Java/Go）
4. 点击"生成"按钮
5. 生成的代码将自动下载

### 示例输入

**输入文件 (schema.json):**
```json
{
  "entities": [
    {
      "name": "User",
      "fields": [
        {"name": "id", "type": "long"},
        {"name": "username", "type": "string"},
        {"name": "email", "type": "string"},
        {"name": "createdAt", "type": "datetime"}
      ]
    }
  ]
}
```

**生成的 Java 输出：**
```java
public class User {
    private Long id;
    private String username;
    private String email;
    private java.time.LocalDateTime createdAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // ... other getters and setters
}
```

**生成的 Go 输出：**
```go
package main

type User struct {
    ID        int64  `json:"id"`
    Username  string `json:"username"`
    Email     string `json:"email"`
    CreatedAt string `json:"createdAt"`
}
```

## 配置

### 应用属性

通过 `src/main/resources/application.yml` 配置应用：

```yaml
spring:
  application:
    name: ai-entity-code-generator
  servlet:
    multipart:
      enabled: true
      max-file-size: 256KB
      max-request-size: 256KB

llm:
  base-url: https://api.openai.com
  api-key: ${LLM_API_KEY}
  model: gpt-4o-mini
  timeout-seconds: 30
  max-input-chars: 50000
```

### 配置选项

| 属性 | 类型 | 默认值 | 说明 |
|----------|------|---------|-------------|
| `llm.base-url` | String | `https://api.openai.com` | LLM API 基础 URL |
| `llm.api-key` | String | - | LLM API 密钥（通过 `LLM_API_KEY` 环境变量设置）|
| `llm.model` | String | `gpt-4o-mini` | LLM 模型名称 |
| `llm.timeout-seconds` | Integer | `30` | HTTP 请求超时时间（秒）|
| `llm.max-input-chars` | Integer | `50000` | 最大输入字符数 |
| `spring.servlet.multipart.max-file-size` | String | `256KB` | 最大上传文件大小 |
| `spring.servlet.multipart.max-request-size` | String | `256KB` | 最大请求大小 |

### 支持的 LLM 提供商

应用使用 OpenAI 兼容 API，支持以下提供商：

- OpenAI GPT 模型
- Azure OpenAI Service
- 本地 LLM 部署（Ollama、LM Studio 等）
- 任何 OpenAI 兼容 API 端点

### 环境变量

| 变量 | 必填 | 说明 |
|----------|----------|-------------|
| `LLM_API_KEY` | 是 | LLM 服务的 API 密钥 |

## 部署

### 构建

```bash
mvn clean package -DskipTests
```

### Assembly 分发包

Assembly 插件会创建分发 zip 包：

```bash
# 解压分发包
unzip target/ai-entity-code-generator-1.0-SNAPSHOT-dist.zip -d deployment/

# 运行应用
cd deployment/ai-entity-code-generator-1.0-SNAPSHOT
java -jar ai-entity-code-generator-1.0-SNAPSHOT.jar
```

### Docker 部署（未来计划）

```dockerfile
FROM openjdk:8-jre-slim
WORKDIR /app
COPY target/ai-entity-code-generator-1.0-SNAPSHOT.jar app.jar
ENV LLM_API_KEY=your_api_key
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 开发

### 前置条件

- JDK 8 或更高版本
- Apache Maven 3.6+
- 网络连接（Maven 依赖）

### 运行测试

```bash
mvn test
```

### 代码规范

- 所有 Java 文件必须在 Javadoc 中包含 `@author Liang.Xu`
- 所有方法必须有规范的 Javadoc 注释
- 遵循 Spring Boot 最佳实践

## 变更日志

### v1.0.0 (2024-01-01)

- **新增**: 初始化项目，Spring Boot 2.7.18
- **新增**: REST API 实体代码生成功能
- **新增**: 支持 Java 和 Go 目标语言
- **新增**: Web UI 文件上传下载界面
- **新增**: OpenAI 兼容 LLM 客户端集成
- **新增**: Assembly 打包分发
- **新增**: 完整的单元测试

## 许可证

MIT License

## 作者

Liang.Xu

## 贡献

欢迎贡献！请随时提交 Pull Request。
