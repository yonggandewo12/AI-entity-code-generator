package org.example.demotest.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 文件解析工具类，支持Excel和CSV文件解析。
 *
 * @author Liang.Xu
 */
public class FileParserUtils {

    private FileParserUtils() {
        // 工具类不允许实例化
    }

    /**
     * 解析Excel文件内容，返回结构化文本。
     *
     * @param file 上传的Excel文件
     * @return 结构化文本内容
     * @throws IOException 如果文件读取失败
     */
    public static String parseExcel(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        Workbook workbook;
        try (InputStream inputStream = file.getInputStream()) {
            if (fileName != null && fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else if (fileName != null && fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                throw new IllegalArgumentException("不支持的Excel文件格式");
            }
        }

        StringBuilder content = new StringBuilder();
        Sheet sheet = workbook.getSheetAt(0); // 只读取第一个工作表
        Iterator<Row> rowIterator = sheet.iterator();

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();
            List<String> rowValues = new ArrayList<>();

            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String cellValue = getCellValueAsString(cell);
                rowValues.add(cellValue);
            }

            content.append(String.join("\t", rowValues)).append("\n");
        }

        workbook.close();
        return content.toString();
    }

    /**
     * 解析CSV文件内容，返回结构化文本。
     *
     * @param file 上传的CSV文件
     * @return 结构化文本内容
     * @throws IOException 如果文件读取失败
     */
    public static String parseCsv(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            // 写入表头
            content.append(String.join("\t", csvParser.getHeaderNames())).append("\n");

            // 写入数据行
            for (CSVRecord record : csvParser) {
                List<String> values = new ArrayList<>();
                for (String value : record) {
                    values.add(value);
                }
                content.append(String.join("\t", values)).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 获取单元格的字符串值。
     *
     * @param cell Excel单元格
     * @return 单元格的字符串表示
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
