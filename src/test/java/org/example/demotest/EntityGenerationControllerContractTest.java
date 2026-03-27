package org.example.demotest;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.example.demotest.llm.OpenAiCompatibleLlmClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP contract tests for the planned entity-generation endpoint.
 *
 * @author Liang.Xu
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "llm.base-url=https://example.invalid",
        "llm.api-key=test-api-key",
        "llm.model=test-model",
        "llm.timeout-seconds=1"
})
class EntityGenerationControllerContractTest {

    private static final String GENERATE_ENDPOINT = "/api/entities/generate";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenAiCompatibleLlmClient llmClient;

    @Test
    @DisplayName("Valid multipart request returns a downloadable plain-text attachment")
    void validMultipartRequestReturnsAttachmentMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "entity-schema.json",
                MediaType.APPLICATION_JSON_VALUE,
                sampleSchema().getBytes(StandardCharsets.UTF_8)
        );

        when(llmClient.generateEntityCode(anyString(), eq("java")))
                .thenReturn("public class User { private Long id; }");

        mockMvc.perform(multipart(GENERATE_ENDPOINT)
                        .file(file)
                        .param("language", "java"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        allOf(containsString("attachment"), containsString("filename="))
                ))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(not(isEmptyOrNullString())));
    }

    @Test
    @DisplayName("Unsupported target language returns HTTP 400 with compact JSON")
    void invalidTargetLanguageReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "entity-schema.json",
                MediaType.APPLICATION_JSON_VALUE,
                sampleSchema().getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart(GENERATE_ENDPOINT)
                        .file(file)
                        .param("language", "python"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("language")));
    }

    @Test
    @DisplayName("Empty upload returns HTTP 400 with compact JSON")
    void emptyFileReturnsBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "entity-schema.json",
                MediaType.APPLICATION_JSON_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart(GENERATE_ENDPOINT)
                        .file(emptyFile)
                        .param("language", "java"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("file")));
    }

    private String sampleSchema() {
        return "{\"entities\":[{\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"}]}]}";
    }
}
