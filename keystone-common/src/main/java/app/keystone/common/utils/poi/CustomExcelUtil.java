package app.keystone.common.utils.poi;

import app.keystone.common.annotation.ExcelColumn;
import app.keystone.common.annotation.ExcelSheet;
import app.keystone.common.exception.ApiException;
import app.keystone.common.exception.error.ErrorCode.Internal;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 自定义Excel 导入导出工具
 *
 * @author likanug
 */
@Slf4j
public class CustomExcelUtil {

    private CustomExcelUtil() {
    }

    public static <T> void writeToResponse(List<T> list, Class<T> clazz, HttpServletResponse response) {
        try {
            writeToOutputStream(list, clazz, response.getOutputStream());
        } catch (IOException e) {
            throw new ApiException(e, Internal.EXCEL_PROCESS_ERROR, e.getMessage());
        }
    }

    public static <T> List<T> readFromRequest(Class<T> clazz, MultipartFile file) {
        try {
            return readFromInputStream(clazz, file.getInputStream());
        } catch (IOException e) {
            throw new ApiException(e, Internal.EXCEL_PROCESS_ERROR, e.getMessage());
        }
    }

    public static <T> void writeToOutputStream(List<T> list, Class<T> clazz, OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            ExcelSheet sheetAnno = clazz.getAnnotation(ExcelSheet.class);
            String sheetName = sheetAnno != null ? sheetAnno.name() : "Sheet1";
            if (sheetName == null || sheetName.isBlank()) {
                sheetName = "Sheet1";
            }
            Sheet sheet = workbook.createSheet(sheetName);

            List<Field> annotatedFields = getAnnotatedFields(clazz);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < annotatedFields.size(); i++) {
                Field field = annotatedFields.get(i);
                ExcelColumn column = field.getAnnotation(ExcelColumn.class);
                headerRow.createCell(i, CellType.STRING).setCellValue(column.name());
            }

            for (int rowIndex = 0; rowIndex < list.size(); rowIndex++) {
                T item = list.get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                for (int colIndex = 0; colIndex < annotatedFields.size(); colIndex++) {
                    Field field = annotatedFields.get(colIndex);
                    field.setAccessible(true);
                    Object value = field.get(item);
                    row.createCell(colIndex, CellType.STRING).setCellValue(value == null ? "" : String.valueOf(value));
                }
            }

            for (int i = 0; i < annotatedFields.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            outputStream.flush();
        } catch (IOException | IllegalAccessException e) {
            throw new ApiException(e, Internal.EXCEL_PROCESS_ERROR, e.getMessage());
        }
    }

    public static <T> List<T> readFromInputStream(Class<T> clazz, InputStream inputStream) {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return List.of();
            }

            Map<String, Integer> headerIndexMap = new HashMap<>();
            for (Cell headerCell : headerRow) {
                headerIndexMap.put(headerCell.getStringCellValue(), headerCell.getColumnIndex());
            }

            List<Field> annotatedFields = getAnnotatedFields(clazz);
            Map<Field, Integer> fieldColumnMap = new LinkedHashMap<>();
            for (Field field : annotatedFields) {
                ExcelColumn column = field.getAnnotation(ExcelColumn.class);
                Integer columnIndex = headerIndexMap.get(column.name());
                if (columnIndex != null) {
                    fieldColumnMap.put(field, columnIndex);
                }
            }

            List<T> result = new ArrayList<>();
            int lastRowNum = sheet.getLastRowNum();
            for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }
                T target = clazz.getDeclaredConstructor().newInstance();
                for (Map.Entry<Field, Integer> entry : fieldColumnMap.entrySet()) {
                    Field field = entry.getKey();
                    field.setAccessible(true);
                    Cell cell = row.getCell(entry.getValue());
                    String cellValue = getCellStringValue(cell);
                    if (cellValue != null) {
                        cellValue = TrimXssEditor.sanitize(cellValue);
                    }
                    Object converted = convertValue(field.getType(), cellValue);
                    field.set(target, converted);
                }
                result.add(target);
            }

            return result;
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new ApiException(e, Internal.EXCEL_PROCESS_ERROR, e.getMessage());
        }
    }

    private static List<Field> getAnnotatedFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<Field> result = new ArrayList<>();
        for (Field field : fields) {
            if (field.getAnnotation(ExcelColumn.class) != null) {
                result.add(field);
            }
        }
        return result;
    }

    private static boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !getCellStringValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double numericValue = cell.getNumericCellValue();
                long longValue = (long) numericValue;
                if (Double.compare(numericValue, longValue) == 0) {
                    yield String.valueOf(longValue);
                }
                yield String.valueOf(numericValue);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK, _NONE, ERROR -> "";
        };
    }

    private static Object convertValue(Class<?> targetType, String value) {
        if (value == null || value.isBlank()) {
            if (targetType.isPrimitive()) {
                if (targetType == boolean.class) {
                    return false;
                }
                if (targetType == char.class) {
                    return '\0';
                }
                return 0;
            }
            return null;
        }
        if (targetType == String.class) {
            return value;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value);
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(value);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.parseShort(value);
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.parseByte(value);
        }
        if (targetType == Character.class || targetType == char.class) {
            return value.charAt(0);
        }
        if (targetType == LocalDate.class) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return value;
    }
}
