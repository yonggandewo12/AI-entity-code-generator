# AI-Entity-Code-Generator

[English](./README.md) | [中文](./README-zh.md)

## Overview

AI-Entity-Code-Generator is a Spring Boot-based application that leverages Large Language Models (LLM) to automatically generate entity code in various programming languages from uploaded schema files.

### Use Cases

- **Schema to Entity Conversion**: Upload JSON/YAML database schema files and generate corresponding entity classes
- **Cross-language Migration**: Convert entity definitions from one language to another (e.g., Java POJO to Go struct)
- **Rapid Prototyping**: Quickly generate boilerplate entity code for new projects
- **Learning Tool**: Understand entity structures across different languages

### Key Features

- Upload text-based schema files (JSON, YAML, etc.)
- Support for multiple target languages (Java, Go)
- OpenAI-compatible API integration
- RESTful API with file download support
- Simple web UI for easy interaction
- Assembly packaging for easy deployment

## Technical Details

### Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   Frontend  │────▶│   Controller     │────▶│    Service      │
│  (Static    │     │  (Spring MVC)    │     │ (Business       │
│   HTML/JS)  │     │                  │     │  Logic)         │
└─────────────┘     └──────────────────┘     └────────┬────────┘
                                                     │
                                                     ▼
                                            ┌─────────────────┐
                                            │  LLM Client     │
                                            │ (OpenAI         │
                                            │  Compatible)    │
                                            └────────┬────────┘
                                                     │
                                                     ▼
                                            ┌─────────────────┐
                                            │ External LLM    │
                                            │ API             │
                                            └─────────────────┘
```

### Project Structure

```
ai-entity-code-generator/
├── src/
│   ├── main/
│   │   ├── java/org/example/demotest/
│   │   │   ├── config/           # Configuration classes
│   │   │   │   ├── LlmHttpClientConfig.java
│   │   │   │   └── LlmProperties.java
│   │   │   ├── controller/       # REST controllers
│   │   │   │   └── EntityGenerationController.java
│   │   │   ├── dto/             # Data transfer objects
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   └── GeneratedCodeFile.java
│   │   │   ├── exception/       # Exception handling
│   │   │   │   ├── AppException.java
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── ConfigurationException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── UpstreamServiceException.java
│   │   │   ├── llm/             # LLM integration
│   │   │   │   ├── OpenAiCompatibleLlmClient.java
│   │   │   │   └── dto/
│   │   │   │       ├── LlmChatMessage.java
│   │   │   │       ├── LlmChatRequest.java
│   │   │   │       └── LlmChatResponse.java
│   │   │   ├── service/          # Business services
│   │   │   │   └── EntityCodeGenerationService.java
│   │   │   └── DemoTestApplication.java
│   │   └── resources/
│   │       ├── application.yml   # Application configuration
│   │       └── static/           # Static web resources
│   │           └── index.html   # Web UI
│   └── test/
│       └── java/                 # Unit tests
├── pom.xml
└── src/assembly/
    └── assembly.xml              # Assembly packaging descriptor
```

### Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 2.7.18 |
| Language | Java 8 |
| Build Tool | Apache Maven 3.9.x |
| HTTP Client | Spring RestTemplate |
| Validation | Spring Boot Validation |
| Testing | JUnit 5, Spring Boot Test, Mockito |
| Packaging | Maven Assembly Plugin |

## API Specification

### Entity Code Generation

#### POST /api/entities/generate

Generate entity code from uploaded file.

**Content-Type:** `multipart/form-data`

**Request Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| file | MultipartFile | Yes | Text file containing entity/schema definition |
| language | String | Yes | Target language for code generation (java/go) |

**Response:**

- **Success (200 OK):**
  - Content-Type: `text/plain`
  - Content-Disposition: `attachment; filename=GeneratedEntity.java` or `generated_entity.go`
  - Body: Generated entity code as plain text

- **Error (400 Bad Request):**
  ```json
  {
    "error": "Bad Request",
    "message": "Uploaded file is empty",
    "path": "/api/entities/generate",
    "timestamp": 1700000000000
  }
  ```

- **Error (500 Internal Server Error):**
  ```json
  {
    "error": "Internal Server Error",
    "message": "Failed to call LLM service: ...",
    "path": "/api/entities/generate",
    "timestamp": 1700000000000
  }
  ```

### Error Response Schema

| Field | Type | Description |
|-------|------|-------------|
| error | String | HTTP status phrase |
| message | String | Human-readable error message |
| path | String | Request path where error occurred |
| timestamp | Long | Unix timestamp in milliseconds |

## Usage Guide

### Quick Start

1. **Configure LLM API Key:**
   ```bash
   export LLM_API_KEY=your_api_key_here
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Access the web UI:**
   Open browser to: `http://localhost:8080`

4. **Or use the API directly:**
   ```bash
   curl -X POST "http://localhost:8080/api/entities/generate" \
     -F "file=@schema.json" \
     -F "language=java" \
     -o GeneratedEntity.java
   ```

### Using the Web UI

1. Open `http://localhost:8080` in your browser
2. Click "Choose File" to select a text file containing entity schema
3. Select target language from dropdown (Java/Go)
4. Click "Generate" button
5. The generated code will be downloaded automatically

### Example Input

**Input file (schema.json):**
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

**Generated Java Output:**
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

**Generated Go Output:**
```go
package main

type User struct {
    ID        int64  `json:"id"`
    Username  string `json:"username"`
    Email     string `json:"email"`
    CreatedAt string `json:"createdAt"`
}
```

## Configuration

### Application Properties

Configure the application via `src/main/resources/application.yml`:

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

### Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `llm.base-url` | String | `https://api.openai.com` | LLM API base URL |
| `llm.api-key` | String | - | LLM API key (set via `LLM_API_KEY` env var) |
| `llm.model` | String | `gpt-4o-mini` | LLM model name |
| `llm.timeout-seconds` | Integer | `30` | HTTP request timeout in seconds |
| `llm.max-input-chars` | Integer | `50000` | Maximum input characters |
| `spring.servlet.multipart.max-file-size` | String | `256KB` | Maximum upload file size |
| `spring.servlet.multipart.max-request-size` | String | `256KB` | Maximum request size |

### Supported LLM Providers

The application uses OpenAI-compatible API. Supported providers include:

- OpenAI GPT models
- Azure OpenAI Service
- Local LLM deployments (Ollama, LM Studio, etc.)
- Any OpenAI-compatible API endpoint

### Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `LLM_API_KEY` | Yes | API key for LLM service |

## Deployment

### Build

```bash
mvn clean package -DskipTests
```

### Assembly Distribution

The assembly plugin creates a distribution zip:

```bash
# Extract the distribution
unzip target/ai-entity-code-generator-1.0-SNAPSHOT-dist.zip -d deployment/

# Run the application
cd deployment/ai-entity-code-generator-1.0-SNAPSHOT
java -jar ai-entity-code-generator-1.0-SNAPSHOT.jar
```

### Docker Deployment (Future)

```dockerfile
FROM openjdk:8-jre-slim
WORKDIR /app
COPY target/ai-entity-code-generator-1.0-SNAPSHOT.jar app.jar
ENV LLM_API_KEY=your_api_key
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Development

### Prerequisites

- JDK 8 or higher
- Apache Maven 3.6+
- Internet access for Maven dependencies

### Run Tests

```bash
mvn test
```

### Code Style

- All Java files must include `@author Liang.Xu` in Javadoc
- All methods must have proper Javadoc comments
- Follow Spring Boot best practices

## Changelog

### v1.0.0 (2024-01-01)

- **Added**: Initial project setup with Spring Boot 2.7.18
- **Added**: REST API for entity code generation
- **Added**: Support for Java and Go target languages
- **Added**: Web UI for file upload and download
- **Added**: OpenAI-compatible LLM client integration
- **Added**: Assembly packaging for distribution
- **Added**: Comprehensive unit tests

## License

MIT License

## Author

Liang.Xu

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
