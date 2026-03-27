package org.example.demotest.service;

import org.example.demotest.config.LlmProperties;
import org.example.demotest.dto.GeneratedCodeFile;
import org.example.demotest.exception.BadRequestException;
import org.example.demotest.llm.OpenAiCompatibleLlmClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Core business logic for generating entity code from uploaded text.
 *
 * @author Liang.Xu
 */
@Service
public class EntityCodeGenerationService {

    private static final String JAVA = "java";

    private static final String GO = "go";

    private final OpenAiCompatibleLlmClient llmClient;

    private final LlmProperties llmProperties;

    public EntityCodeGenerationService(OpenAiCompatibleLlmClient llmClient, LlmProperties llmProperties) {
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
    }

    /**
     * Generates entity code file from uploaded file and target language.
     * Performs validation of input, reads file content, calls LLM to generate code,
     * and returns the generated file with appropriate filename.
     *
     * @param file Uploaded multipart file containing entity schema/text
     * @param language Target language for code generation (java/go)
     * @return GeneratedCodeFile containing filename and code content
     * @throws BadRequestException If file is null, empty, blank, binary, oversized, or language is unsupported
     */
    public GeneratedCodeFile generateEntityFile(MultipartFile file, String language) {
        if (file == null) {
            throw new BadRequestException("File is required");
        }
        if (file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }
        String normalizedLanguage = normalizeLanguage(language);

        byte[] bytes = readBytes(file);
        validateTextLike(bytes);

        String content = new String(bytes, StandardCharsets.UTF_8);
        if (!StringUtils.hasText(content)) {
            throw new BadRequestException("Uploaded file content is blank");
        }
        if (content.length() > llmProperties.getMaxInputChars()) {
            throw new BadRequestException("Uploaded content exceeds max-input-chars limit");
        }

        String generated = llmClient.generateEntityCode(content, normalizedLanguage);
        return new GeneratedCodeFile(outputFileName(normalizedLanguage), generated);
    }

    /**
     * Normalizes and validates the target language input.
     * Converts to lowercase, trims whitespace, and verifies it is either "java" or "go".
     *
     * @param language Raw input language string
     * @return Normalized lowercase language string
     * @throws BadRequestException If language is blank or unsupported
     */
    private String normalizeLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            throw new BadRequestException("Language is required");
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (!JAVA.equals(normalized) && !GO.equals(normalized)) {
            throw new BadRequestException("Unsupported language. Allowed values: java, go");
        }
        return normalized;
    }

    /**
     * Reads byte content from uploaded multipart file.
     *
     * @param file Uploaded multipart file
     * @return Byte array of file content
     * @throws BadRequestException If there is an I/O error reading the file
     */
    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read uploaded file");
        }
    }

    /**
     * Validates that byte content appears to be text (not binary).
     * Checks for null bytes and excessive control characters to detect binary files.
     *
     * @param bytes Byte content to validate
     * @throws BadRequestException If content appears to be binary or non-text
     */
    private void validateTextLike(byte[] bytes) {
        int controlCharCount = 0;
        for (byte b : bytes) {
            int value = b & 0xFF;
            if (value == 0) {
                throw new BadRequestException("Uploaded file appears to be binary, text file is required");
            }
            boolean isControl = value < 0x20 && value != '\n' && value != '\r' && value != '\t';
            if (isControl) {
                controlCharCount++;
            }
        }
        if (controlCharCount > (bytes.length / 20)) {
            throw new BadRequestException("Uploaded file appears to be non-text content");
        }
    }

    /**
     * Generates appropriate output filename based on target language.
     *
     * @param language Normalized target language (java/go)
     * @return Filename with correct extension for the language
     */
    private String outputFileName(String language) {
        if (JAVA.equals(language)) {
            return "GeneratedEntity.java";
        }
        return "generated_entity.go";
    }
}
