package eu.openvalue.timesheet.adapter.out.reading;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Writes out an Excel workbook as text a language model can read: every sheet as a Markdown table of the cells in
 * use, headed by the column letters and with the row numbers in front, showing the values as Excel displays them,
 * including the results of formulas.
 */
final class SpreadsheetTranscript {

  /** Keeps unexpectedly large workbooks from flooding the model. */
  private static final int MAXIMUM_ROWS = 200;
  private static final int MAXIMUM_COLUMNS = 40;

  private SpreadsheetTranscript() {
  }

  /**
   * @throws IOException when the content is not an Excel workbook
   */
  static String of(byte[] workbookContent) throws IOException {
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookContent))) {
      var formatter = new DataFormatter();
      var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
      var tables = new ArrayList<String>();
      for (var sheet : workbook) {
        var rows = filledRowsOf(sheet, formatter, evaluator);
        if (!rows.isEmpty()) {
          tables.add("Sheet " + sheet.getSheetName() + ":\n" + tableOf(rows));
        }
      }
      return String.join("\n\n", tables);
    }
  }

  private static List<FilledRow> filledRowsOf(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
    var rows = new ArrayList<FilledRow>();
    for (var row : sheet) {
      if (row.getRowNum() >= MAXIMUM_ROWS) {
        break;
      }
      var values = valuesOf(row, formatter, evaluator);
      if (values.stream().anyMatch(value -> !value.isEmpty())) {
        rows.add(new FilledRow(row.getRowNum() + 1, values));
      }
    }
    return rows;
  }

  private static List<String> valuesOf(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
    var values = new ArrayList<String>();
    for (int column = 0; column < Math.min(Math.max(row.getLastCellNum(), 0), MAXIMUM_COLUMNS); column++) {
      var cell = row.getCell(column);
      values.add(cell == null ? "" : formatter.formatCellValue(cell, evaluator).strip().replace('|', '/'));
    }
    return values;
  }

  /**
   * A Markdown table of the columns in use, from the first to the last column that holds a value.
   */
  private static String tableOf(List<FilledRow> rows) {
    var firstColumn = rows.stream().mapToInt(FilledRow::firstFilledColumn).min().orElse(0);
    var lastColumn = rows.stream().mapToInt(FilledRow::lastFilledColumn).max().orElse(0);
    var table = new StringBuilder("| Row |");
    for (int column = firstColumn; column <= lastColumn; column++) {
      table.append(' ').append(CellReference.convertNumToColString(column)).append(" |");
    }
    table.append("\n|---|").append("---|".repeat(lastColumn - firstColumn + 1));
    for (var row : rows) {
      table.append("\n| ").append(row.number()).append(" |");
      for (int column = firstColumn; column <= lastColumn; column++) {
        table.append(' ').append(row.value(column)).append(" |");
      }
    }
    return table.toString();
  }

  private record FilledRow(int number, List<String> values) {

    String value(int column) {
      return column < values.size() ? values.get(column) : "";
    }

    int firstFilledColumn() {
      for (int column = 0; column < values.size(); column++) {
        if (!values.get(column).isEmpty()) {
          return column;
        }
      }
      return Integer.MAX_VALUE;
    }

    int lastFilledColumn() {
      for (int column = values.size() - 1; column >= 0; column--) {
        if (!values.get(column).isEmpty()) {
          return column;
        }
      }
      return -1;
    }
  }
}
