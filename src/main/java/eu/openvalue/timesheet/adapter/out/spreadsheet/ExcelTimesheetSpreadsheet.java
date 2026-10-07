package eu.openvalue.timesheet.adapter.out.spreadsheet;

import eu.openvalue.timesheet.application.port.out.SpreadsheetFile;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.DayEntry;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Timesheet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;

/**
 * Fills in a copy of the company's Excel template. Only input cells are written; the template's own formulas (the
 * dates of the month, the totals) are kept and recalculated. Hours are written as decimal hours, so 7:30 becomes 7.5.
 */
public final class ExcelTimesheetSpreadsheet implements TimesheetSpreadsheet {

  private static final String FILE_EXTENSION = "xlsx";
  private static final String MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final String SHEET = "Blad1";
  private static final String MONTH = "O7";
  private static final String YEAR = "O8";
  private static final String CLIENT = "O10";
  private static final String PROJECT = "O11";
  private static final String PROFESSIONAL_NAME = "E14";
  /** The row of the first day of the month; every next day is on the next row. */
  private static final int FIRST_DAY_ROW = 20;
  private static final Map<HourCategory, String> HOURS_COLUMNS = hoursColumns();
  private static final String KILOMETRES_COLUMN = "L";
  private static final String EXPLANATION_COLUMN = "M";
  private static final double MINUTES_PER_HOUR = 60.0;

  private final Resource template;

  public ExcelTimesheetSpreadsheet(Resource template) {
    this.template = Objects.requireNonNull(template, "template");
  }

  @Override
  public SpreadsheetFile create(Timesheet timesheet) {
    try (var input = template.getInputStream();
        var workbook = new XSSFWorkbook(input);
        var output = new ByteArrayOutputStream()) {
      var sheet = workbook.getSheet(SHEET);
      fillHeader(sheet, timesheet);
      timesheet.days().forEach(day -> fillDay(sheet, day));
      XSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
      workbook.setForceFormulaRecalculation(true);
      workbook.write(output);
      return new SpreadsheetFile(FILE_EXTENSION, MEDIA_TYPE, output.toByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException("Could not create the timesheet spreadsheet from " + template, e);
    }
  }

  private static void fillHeader(Sheet sheet, Timesheet timesheet) {
    cell(sheet, MONTH).setCellValue(timesheet.month().getMonthValue());
    cell(sheet, YEAR).setCellValue(timesheet.month().getYear());
    cell(sheet, PROFESSIONAL_NAME).setCellValue(timesheet.professional().name());
    timesheet.assignment().ifPresent(assignment -> fillAssignment(sheet, assignment));
  }

  private static void fillAssignment(Sheet sheet, Assignment assignment) {
    cell(sheet, CLIENT).setCellValue(assignment.client().name());
    assignment.project().ifPresent(project -> cell(sheet, PROJECT).setCellValue(project.name()));
  }

  private static void fillDay(Sheet sheet, DayEntry day) {
    var row = FIRST_DAY_ROW + day.date().getDayOfMonth() - 1;
    day.bookedHours().forEach((category, hours) ->
        cell(sheet, HOURS_COLUMNS.get(category) + row).setCellValue(decimalHours(hours)));
    if (!day.kilometres().isZero()) {
      cell(sheet, KILOMETRES_COLUMN + row).setCellValue(day.kilometres().value());
    }
    day.explanation().ifPresent(explanation ->
        cell(sheet, EXPLANATION_COLUMN + row).setCellValue(explanation.text()));
  }

  private static double decimalHours(Hours hours) {
    return hours.inMinutes() / MINUTES_PER_HOUR;
  }

  private static Cell cell(Sheet sheet, String reference) {
    var cellReference = new CellReference(reference);
    var row = sheet.getRow(cellReference.getRow());
    if (row == null) {
      row = sheet.createRow(cellReference.getRow());
    }
    return row.getCell(cellReference.getCol(), MissingCellPolicy.CREATE_NULL_AS_BLANK);
  }

  private static Map<HourCategory, String> hoursColumns() {
    var columns = new EnumMap<HourCategory, String>(HourCategory.class);
    columns.put(HourCategory.WORKED, "C");
    columns.put(HourCategory.OVERTIME, "D");
    columns.put(HourCategory.SICK, "E");
    columns.put(HourCategory.LEAVE, "F");
    columns.put(HourCategory.PUBLIC_HOLIDAY, "G");
    columns.put(HourCategory.AVAILABLE, "H");
    columns.put(HourCategory.TRAINING, "I");
    columns.put(HourCategory.OTHER, "J");
    columns.put(HourCategory.STAND_BY, "K");
    if (columns.size() != HourCategory.values().length) {
      throw new IllegalStateException("Every hour category needs a column in the spreadsheet");
    }
    return columns;
  }
}
