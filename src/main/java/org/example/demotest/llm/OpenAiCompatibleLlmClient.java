package org.example.demotest.llm;

import org.example.demotest.config.LlmProperties;
import org.example.demotest.exception.ConfigurationException;
import org.example.demotest.exception.UpstreamServiceException;
import org.example.demotest.llm.dto.LlmChatMessage;
import org.example.demotest.llm.dto.LlmChatRequest;
import org.example.demotest.llm.dto.LlmChatResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

/**
 * Thin adapter over an OpenAI-compatible chat completions API.
 *
 * @author Liang.Xu
 */
@Component
public class OpenAiCompatibleLlmClient {

    private final RestTemplate llmRestTemplate;

    private final LlmProperties llmProperties;

    public OpenAiCompatibleLlmClient(RestTemplate llmRestTemplate, LlmProperties llmProperties) {
        this.llmRestTemplate = llmRestTemplate;
        this.llmProperties = llmProperties;
    }

    /**
     * Calls OpenAI-compatible LLM API to generate entity code from source text.
     * Builds chat request with system prompt and user input, sends HTTP request to LLM,
     * parses and returns the generated code content.
     *
     * @param sourceText Input text containing schema/entity definition
     * @param language Target programming language for code generation
     * @return Generated entity code as plain text string
     * @throws ConfigurationException If LLM configuration is incomplete
     * @throws UpstreamServiceException If LLM API call fails or returns invalid response
     */
    public String generateEntityCode(String sourceText, String language) {
        validateConfiguration();

        String endpoint = normalizeBaseUrl(llmProperties.getBaseUrl()) + "/v1/chat/completions";
        LlmChatRequest request = new LlmChatRequest(
                llmProperties.getModel(),
                Arrays.asList(
                        new LlmChatMessage("system", buildSystemPrompt(language)),
                        new LlmChatMessage("user", sourceText)
                ),
                0.2D
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmProperties.getApiKey().trim());

        try {
            ResponseEntity<LlmChatResponse> response = llmRestTemplate.postForEntity(
                    endpoint,
                    new HttpEntity<LlmChatRequest>(request, headers),
                    LlmChatResponse.class
            );
            LlmChatResponse body = response.getBody();
            if (body == null || body.getChoices() == null || body.getChoices().isEmpty()
                    || body.getChoices().get(0).getMessage() == null
                    || !StringUtils.hasText(body.getChoices().get(0).getMessage().getContent())) {
                throw new UpstreamServiceException("LLM response did not contain generated code");
            }
            return body.getChoices().get(0).getMessage().getContent();
        } catch (RestClientException ex) {
            throw new UpstreamServiceException("Failed to call LLM service: " + ex.getMessage());
        }
    }

    /**
     * Validates that required LLM configuration properties are present.
     * Checks for presence of API key, base URL, and model name.
     *
     * @throws ConfigurationException If any required configuration is missing
     */
    private void validateConfiguration() {
        if (!StringUtils.hasText(llmProperties.getApiKey())) {
            throw new ConfigurationException("Missing LLM API key configuration");
        }
        if (!StringUtils.hasText(llmProperties.getBaseUrl())) {
            throw new ConfigurationException("Missing LLM base URL configuration");
        }
        if (!StringUtils.hasText(llmProperties.getModel())) {
            throw new ConfigurationException("Missing LLM model configuration");
        }
    }

    /**
     * Normalizes LLM API base URL by removing trailing slash if present.
     * Ensures consistent URL formatting for endpoint construction.
     *
     * @param baseUrl Raw base URL string
     * @return Normalized base URL without trailing slash
     */
    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * Builds system prompt for LLM code generation in Chinese.
     * Instructs LLM to generate only valid code for the specified language,
     * no markdown fences or explanations.
     *
     * @param language Target programming language for code generation
     * @return System prompt string in Chinese
     */
    private String buildSystemPrompt(String language) {
        return "你只需生成一个实体源代码文件。"
                + "严格输出有效的 " + language + " 代码文本，不要使用 Markdown 代码块，不要任何解释。";
    }
}
