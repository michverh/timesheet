package eu.openvalue.timesheet.adapter.out.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Kilometres;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Project;
import eu.openvalue.timesheet.domain.Timesheet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExcelTimesheetSpreadsheetTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);
  private static final int MONDAY_ROW = 23;
  private static final Professional JAN = Professional.withEmail("jan.van.der.berg@openvalue.nl");

  private final ExcelTimesheetSpreadsheet spreadsheet =
      new ExcelTimesheetSpreadsheet(new ClassPathResource("timesheet.xlsx"));

  private final Timesheet timesheet =
      Timesheet.start(MAY_2026, JAN, Optional.of(Assignment.at(new Client("ACME"), new Project("Platform"))));

  @Test
  void createsAnExcelWorkbook() {
    // When
    var file = spreadsheet.create(timesheet);

    // Then
    assertThat(file.fileExtension()).isEqualTo("xlsx");
    assertThat(file.mediaType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  }

  @Test
  void fillsInTheMonthAndWhoseTimesheetItIs() throws IOException {
    // When
    var sheet = create(timesheet);

    // Then
    assertThat(cell(sheet, "O7").getNumericCellValue()).isEqualTo(5);
    assertThat(cell(sheet, "O8").getNumericCellValue()).isEqualTo(2026);
    assertThat(cell(sheet, "E14").getStringCellValue()).isEqualTo("Jan van der Berg");
    assertThat(cell(sheet, "O10").getStringCellValue()).isEqualTo("ACME");
    assertThat(cell(sheet, "O11").getStringCellValue()).isEqualTo("Platform");
  }

  @Test
  void leavesTheClientEmptyWhenOnTheBench() throws IOException {
    // When
    var sheet = create(Timesheet.start(MAY_2026, JAN, Optional.empty()));

    // Then
    assertThat(cellOrNull(sheet, "O10")).satisfiesAnyOf(
        cell -> assertThat(cell).isNull(),
        cell -> assertThat(cell.getCellType()).isEqualTo(CellType.BLANK));
  }

  @Test
  void writesEachCategoryInItsOwnColumnOnTheRowOfTheDay() throws IOException {
    // Given
    var categories = HourCategory.values();
    for (int i = 0; i < categories.length; i++) {
      timesheet.bookHours(MONDAY, categories[i], Hours.ofMinutes(15 * (i + 1)));
    }

    // When
    var sheet = create(timesheet);

    // Then: Gewerkt, Overwerk, Ziekte, Verlof, Feestdag, Beschikbaar, Opleiding, Overig, Stand-By
    assertThat("CDEFGHIJK".chars().mapToObj(column -> cell(sheet, (char) column + "" + MONDAY_ROW)))
        .extracting(Cell::getNumericCellValue)
        .containsExactly(0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25);
  }

  @Test
  void writesHoursAndMinutesAsDecimalHours() throws IOException {
    // Given
    timesheet.bookHours(MONDAY, HourCategory.WORKED, Hours.of(7, 30));
    timesheet.bookHours(MONDAY.plusDays(1), HourCategory.WORKED, Hours.of(7, 20));

    // When
    var sheet = create(timesheet);

    // Then
    assertThat(cell(sheet, "C" + MONDAY_ROW).getNumericCellValue()).isEqualTo(7.5);
    assertThat(cell(sheet, "C" + (MONDAY_ROW + 1)).getNumericCellValue()).isCloseTo(7.3333, within(0.0001));
  }

  @Test
  void leavesCategoriesWithoutHoursEmpty() throws IOException {
    // Given
    timesheet.bookHours(MONDAY, HourCategory.WORKED, Hours.of(8));

    // When
    var sheet = create(timesheet);

    // Then
    assertThat(cellOrNull(sheet, "D" + MONDAY_ROW)).satisfiesAnyOf(
        cell -> assertThat(cell).isNull(),
        cell -> assertThat(cell.getCellType()).isEqualTo(CellType.BLANK));
  }

  @Test
  void writesKilometresAndExplanation() throws IOException {
    // Given
    timesheet.recordKilometres(MONDAY, Kilometres.of(42));
    timesheet.explain(MONDAY, new Explanation("Visit to client office"));

    // When
    var sheet = create(timesheet);

    // Then
    assertThat(cell(sheet, "L" + MONDAY_ROW).getNumericCellValue()).isEqualTo(42);
    assertThat(cell(sheet, "M" + MONDAY_ROW).getStringCellValue()).isEqualTo("Visit to client office");
  }

  @Test
  void keepsTheFormulasOfTheTemplateAndCalculatesThem() throws IOException {
    // Given
    timesheet.bookHours(MONDAY, HourCategory.WORKED, Hours.of(7, 30));
    timesheet.bookHours(MONDAY.plusDays(1), HourCategory.WORKED, Hours.of(8));
    timesheet.bookHours(MONDAY.plusDays(1), HourCategory.LEAVE, Hours.of(0, 30));

    // When
    var sheet = create(timesheet);

    // Then
    var workedTotal = cell(sheet, "C52");
    assertThat(workedTotal.getCellFormula()).isEqualTo("SUM(C20:C50)");
    assertThat(workedTotal.getNumericCellValue()).isEqualTo(15.5);
    assertThat(cell(sheet, "B52").getNumericCellValue()).isEqualTo(16.0);
    assertThat(cell(sheet, "B20").getLocalDateTimeCellValue().atZone(ZoneId.systemDefault()).toLocalDate())
        .isEqualTo(LocalDate.of(2026, 5, 1));
  }

  private XSSFSheet create(Timesheet timesheet) throws IOException {
    var workbook = new XSSFWorkbook(new ByteArrayInputStream(spreadsheet.create(timesheet).content()));
    return workbook.getSheet("Blad1");
  }

  private static Cell cell(XSSFSheet sheet, String reference) {
    var cell = cellOrNull(sheet, reference);
    assertThat(cell).as("cell %s", reference).isNotNull();
    return cell;
  }

  private static Cell cellOrNull(XSSFSheet sheet, String reference) {
    var cellReference = new CellReference(reference);
    var row = sheet.getRow(cellReference.getRow());
    return row == null ? null : row.getCell(cellReference.getCol());
  }
}
