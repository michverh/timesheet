package eu.openvalue.timesheet.adapter.out.reading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class SpreadsheetTranscriptTest {

  @Test
  void writesOutEverySheetAsAMarkdownTableOfTheColumnsInUse() throws IOException {
    // Given
    var workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet("Uren");
    var header = sheet.createRow(1);
    header.createCell(1).setCellValue("Dag");
    header.createCell(2).setCellValue("Gewerkt");
    var day = sheet.createRow(2);
    day.createCell(1).setCellValue(3);
    day.createCell(2).setCellValue(8);

    // When
    var transcript = SpreadsheetTranscript.of(contentOf(workbook));

    // Then
    assertThat(transcript).isEqualTo("""
        Sheet Uren:
        | Row | B | C |
        |---|---|---|
        | 2 | Dag | Gewerkt |
        | 3 | 3 | 8 |""");
  }

  @Test
  void showsTheResultsOfFormulas() throws IOException {
    // Given
    var workbook = new XSSFWorkbook();
    var row = workbook.createSheet("Uren").createRow(0);
    row.createCell(0).setCellValue(8);
    row.createCell(1).setCellValue(7.5);
    row.createCell(2).setCellFormula("SUM(A1:B1)");

    // When
    var transcript = SpreadsheetTranscript.of(contentOf(workbook));

    // Then
    assertThat(transcript).contains("| 1 | 8 | 7.5 | 15.5 |");
  }

  @Test
  void leavesOutEmptyRowsAndSheets() throws IOException {
    // Given
    var workbook = new XSSFWorkbook();
    var sheet = workbook.createSheet("Uren");
    sheet.createRow(0).createCell(0).setCellValue("Week 27");
    sheet.createRow(1).createCell(0).setCellValue(" ");
    sheet.createRow(4).createCell(0).setCellValue(8);
    workbook.createSheet("Leeg");

    // When
    var transcript = SpreadsheetTranscript.of(contentOf(workbook));

    // Then
    assertThat(transcript).isEqualTo("""
        Sheet Uren:
        | Row | A |
        |---|---|
        | 1 | Week 27 |
        | 5 | 8 |""");
  }

  @Test
  void refusesSomethingThatIsNotAnExcelWorkbook() {
    assertThatThrownBy(() -> SpreadsheetTranscript.of(new byte[] {1, 2, 3})).isInstanceOf(Exception.class);
  }

  static byte[] contentOf(XSSFWorkbook workbook) throws IOException {
    try (workbook; var output = new ByteArrayOutputStream()) {
      workbook.write(output);
      return output.toByteArray();
    }
  }
}
