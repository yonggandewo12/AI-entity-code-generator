package org.example.demotest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.demotest.llm.OpenAiCompatibleLlmClient;
import org.example.demotest.utils.FileParserUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "llm.base-url=https://example.invalid",
        "llm.api-key=test-api-key",
        "llm.model=test-model",
        "llm.timeout-seconds=1"
})
class FileParserUtilsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenAiCompatibleLlmClient llmClient;

    @Test
    void parseCsvShouldReturnStructuredText() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "schema.csv",
                "text/csv",
                "name,type\nid,long\nusername,string\n".getBytes(StandardCharsets.UTF_8)
        );

        String content = FileParserUtils.parseCsv(file);

        assertTrue(content.contains("name\ttype"));
        assertTrue(content.contains("id\tlong"));
        assertTrue(content.contains("username\tstring"));
    }

    @Test
    void parseExcelShouldReturnStructuredText() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("name");
        header.createCell(1).setCellValue("type");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("id");
        row.createCell(1).setCellValue("long");
        workbook.write(outputStream);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "schema.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray()
        );

        String content = FileParserUtils.parseExcel(file);

        assertTrue(content.contains("name\ttype"));
        assertTrue(content.contains("id\tlong"));
    }

    @Test
    void uploadCsvShouldStillGenerateEntity() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "schema.csv",
                "text/csv",
                "name,type\nid,long\nusername,string\n".getBytes(StandardCharsets.UTF_8)
        );

        when(llmClient.generateEntityCode(contains("name\ttype"), eq("java")))
                .thenReturn("public class CsvEntity { private Long id; }");

        mockMvc.perform(multipart("/api/entities/generate")
                        .file(file)
                        .param("language", "java"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CsvEntity")));
    }

    @Test
    void uploadXlsxShouldStillGenerateEntity() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("name");
        header.createCell(1).setCellValue("type");
        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("id");
        row.createCell(1).setCellValue("long");
        workbook.write(outputStream);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "schema.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray()
        );

        when(llmClient.generateEntityCode(contains("name\ttype"), eq("java")))
                .thenReturn("public class ExcelEntity { private Long id; }");

        mockMvc.perform(multipart("/api/entities/generate")
                        .file(file)
                        .param("language", "java"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ExcelEntity")));
    }
}
