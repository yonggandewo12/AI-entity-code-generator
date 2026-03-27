package org.example.demotest.controller;

import org.example.demotest.dto.GeneratedCodeFile;
import org.example.demotest.service.EntityCodeGenerationService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;

/**
 * 用于生成实体代码文件的REST端点。
 *
 * @author Liang.Xu
 */
@Validated
@RestController
@RequestMapping("/api/entities")
public class EntityGenerationController {

    private final EntityCodeGenerationService generationService;

    public EntityGenerationController(EntityCodeGenerationService generationService) {
        this.generationService = generationService;
    }

    /**
     * Handles file upload and generates entity code in the specified language.
     *
     * @param file Uploaded text file containing schema or entity definition
     * @param language Target programming language for generated entity (java/go)
     * @return ResponseEntity with generated code as plain text attachment
     */
    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("language") @NotBlank String language) {
        GeneratedCodeFile generatedFile = generationService.generateEntityFile(file, language);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(generatedFile.getFileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(generatedFile.getContent().getBytes(StandardCharsets.UTF_8));
    }
}
