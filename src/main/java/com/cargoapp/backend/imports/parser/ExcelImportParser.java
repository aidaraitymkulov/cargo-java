package com.cargoapp.backend.imports.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Парсер Excel-файлов для двух форматов импорта.
 */
@Slf4j
@Component
public class ExcelImportParser {

    /**
     * Строка для импорта из Китая.
     * Колонка A = hatch, Колонка B = personalCode
     */
    public record ChinaRow(int rowNumber, String hatch, String personalCode) {
    }

    /**
     * Блок для импорта KG (in-kg / delivered).
     * Один блок = один клиент = один заказ.
     */
    public record KgBlock(
            int summaryRowNumber,
            String personalCode,
            List<String> hatches,
            BigDecimal weight,
            BigDecimal price
    ) {
    }

    /**
     * Парсит файл формата "Китай": каждая строка — отдельный товар.
     * Пропускает строки, где hatch или personalCode пустые.
     */
    public List<ChinaRow> parseChinaFile(MultipartFile file) throws IOException {
        List<ChinaRow> rows = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                // +1 потому что POI использует 0-based индексы, а люди считают с 1
                int rowNum = row.getRowNum() + 1;

                String hatch = getCellString(row, 0);
                String personalCode = getCellString(row, 1);

                if (hatch.isBlank() || personalCode.isBlank()) {
                    continue; // пустые строки молча пропускаем
                }

                rows.add(new ChinaRow(rowNum, hatch.trim(), personalCode.trim()));
            }
        }

        return rows;
    }

    /**
     * Парсит файл блочного формата KG (in-kg и delivered используют одинаковый формат).
     * <p>
     * Формат блока:
     * пустая A | YT12345 |      |
     * пустая A | YT62782 |      |
     * AN0001   | YT39292 | 3.0  | 5000   <-- итоговая строка (personalCode не пустой)
     * <p>
     * Алгоритм: накапливаем hatch пока не встретим строку с personalCode —
     * это сигнал конца блока.
     */
    public List<KgBlock> parseKgFile(MultipartFile file) throws IOException {
        List<KgBlock> blocks = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            List<String> currentBlockHatches = new ArrayList<>();

            for (Row row : sheet) {
                int rowNum = row.getRowNum() + 1;

                String personalCode = getCellString(row, 0);
                String hatch = getCellString(row, 1);
                String weightStr = getCellString(row, 2);
                String priceStr = getCellString(row, 3);

                // hatch должен быть в каждой строке блока (в том числе в итоговой)
                if (!hatch.isBlank()) {
                    currentBlockHatches.add(hatch.trim());
                }

                // Итоговая строка блока — там где personalCode не пустой
                if (!personalCode.isBlank()) {
                    BigDecimal weight = parseBigDecimal(weightStr);
                    BigDecimal price = parseBigDecimal(priceStr);

                    // Копируем список hatch и сбрасываем для следующего блока
                    List<String> blockHatches = new ArrayList<>(currentBlockHatches);
                    currentBlockHatches.clear();

                    if (!blockHatches.isEmpty() && weight != null && price != null) {
                        blocks.add(new KgBlock(
                                rowNum,
                                personalCode.trim(),
                                blockHatches,
                                weight,
                                price
                        ));
                    } else {
                        // Блок невалидный — логируем чтобы было видно в логах сервера.
                        // Причины: пустой weight/price (менеджер не заполнил) или нет ни одного hatch.
                        log.warn("KG parser: invalid block skipped at row={}, personalCode={}, " +
                                        "hatches={}, weight={}, price={}",
                                rowNum, personalCode.trim(), blockHatches.size(), weightStr, priceStr);
                    }
                }
            }
        }

        return blocks;
    }

    // ---- вспомогательные методы ----

    private String getCellString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";

        // POI может хранить числа как NUMERIC, а personalCode как AN0001 — как STRING
        // Если ячейка содержит формулу — вычисляем её кэшированное значение
        CellType effectiveType = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();

        if (effectiveType == CellType.NUMERIC) {
            // Форматируем без дробной части если это целое
            double val = cell.getNumericCellValue();
            if (val == Math.floor(val)) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        }

        if (effectiveType == CellType.STRING) {
            return cell.getStringCellValue();
        }

        return "";
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
