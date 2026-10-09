package eu.openvalue.timesheet.adapter.out.reading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Hours;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class TranscribingClientTimesheetReaderTest {

  private static final byte[] IMAGE = {1, 2, 3, 4};
  private static final ClientTimesheetDocument SCREENSHOT =
      new ClientTimesheetDocument("week-27.png", DocumentFormat.PNG, IMAGE);
  private static final String NO_DAYS = """
      {"year": 2026, "days": []}""";

  private final List<TranscriptionRequest> requests = new ArrayList<>();

  @Test
  void asksToTranscribeAnImageInAFixedStructure() {
    // When
    answering(NO_DAYS).read(SCREENSHOT);

    // Then
    var request = requests.getFirst();
    assertThat(request.prompt()).startsWith("This image is a timesheet");
    assertThat(request.images()).singleElement().satisfies(image -> {
      assertThat(image.mediaType()).isEqualTo("image/png");
      assertThat(image.content()).isEqualTo(IMAGE);
    });
    assertThat(request.schema()).containsEntry("required", List.of("year", "days", "totals"));
  }

  @Test
  void asksToTranscribeAnExcelWorkbookWrittenOutAsText() throws IOException {
    // Given
    var workbook = new XSSFWorkbook();
    workbook.createSheet("Uren").createRow(0).createCell(0).setCellValue("Gewerkt");

    // When
    answering(NO_DAYS).read(new ClientTimesheetDocument("uren.xlsx", DocumentFormat.XLSX,
        SpreadsheetTranscriptTest.contentOf(workbook)));

    // Then
    var request = requests.getFirst();
    assertThat(request.prompt()).contains("a spreadsheet", "| 1 | Gewerkt |");
    assertThat(request.images()).isEmpty();
  }

  @Test
  void givesTheHoursWorkedPerDayAsTranscribed() {
    // Given: week 27 of 2026, as a model transcribed it
    var reader = answering("""
        {"year": 2026, "days": [
          {"d": 29, "m": 6, "h": "08:00"},
          {"d": 30, "m": 6, "h": "7:30"},
          {"d": 1, "m": 7, "h": "-"},
          {"d": 2, "m": 7, "h": "7,5"},
          {"d": 3, "m": 7, "h": ""}
        ]}""");

    // When
    var reported = reader.read(SCREENSHOT);

    // Then
    assertThat(reported).isEqualTo(new ClientReportedHours(Map.of(
        LocalDate.of(2026, 6, 29), Hours.of(8),
        LocalDate.of(2026, 6, 30), Hours.of(7, 30),
        LocalDate.of(2026, 7, 1), Hours.ZERO,
        LocalDate.of(2026, 7, 2), Hours.of(7, 30),
        LocalDate.of(2026, 7, 3), Hours.ZERO)));
  }

  @Test
  void movesOnToTheNextYearWhenAWeekPassesNewYear() {
    // Given
    var reader = answering("""
        {"year": 2026, "days": [{"d": 31, "m": 12, "h": "8"}, {"d": 1, "m": 1, "h": ""}]}""");

    // When
    var reported = reader.read(SCREENSHOT);

    // Then
    assertThat(reported.on(LocalDate.of(2027, 1, 1))).contains(Hours.ZERO);
  }

  @Test
  void acceptsHoursThatAddUpToOneOfTheTotalsTheTimesheetStates() {
    // Given: a grand total of all kinds of hours, the total worked and a total of kilometres
    var reader = answering("""
        {"year": 2026, "days": [{"d": 3, "m": 8, "h": "8"}, {"d": 4, "m": 8, "h": "7:30"}],
         "totals": ["23,5", "15:30", "399 km"]}""");

    // When
    var reported = reader.read(SCREENSHOT);

    // Then
    assertThat(reported.total()).isEqualTo(Hours.of(15, 30));
  }

  @Test
  void acceptsHoursOfATimesheetThatStatesNoTotals() {
    // Given
    var reader = answering("""
        {"year": 2026, "days": [{"d": 3, "m": 8, "h": "8"}], "totals": []}""");

    // When / Then
    assertThat(reader.read(SCREENSHOT).total()).isEqualTo(Hours.of(8));
  }

  @Test
  void reportsHoursThatAddUpToNoneOfTheStatedTotalsAsUnreliable() {
    // Given: one day misread, so the days no longer add up to the stated 16 hours
    var reader = answering("""
        {"year": 2026, "days": [{"d": 3, "m": 8, "h": "8"}, {"d": 4, "m": 8, "h": ""}], "totals": ["16"]}""");

    // When / Then
    assertThatThrownBy(() -> reader.read(SCREENSHOT))
        .isInstanceOf(ClientTimesheetUnreadableException.class)
        .hasMessageContaining("add up to 8:00, which matches none of the totals it states: [16]");
  }

  @Test
  void doesNotReadPdfDocumentsYet() {
    assertThatThrownBy(() -> answering(NO_DAYS).read(
        new ClientTimesheetDocument("acme.pdf", DocumentFormat.PDF, IMAGE)))
        .isInstanceOf(ClientTimesheetUnreadableException.class)
        .hasMessageContaining("not supported yet");
  }

  @Test
  void reportsAnExcelWorkbookThatCannotBeOpened() {
    assertThatThrownBy(() -> answering(NO_DAYS).read(
        new ClientTimesheetDocument("uren.xlsx", DocumentFormat.XLSX, IMAGE)))
        .isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void reportsATranscriptionThatIsNotInTheFixedStructure() {
    assertThatThrownBy(() -> answering("The timesheet shows 32 hours.").read(SCREENSHOT))
        .isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void reportsATranscriptionWithSomethingOtherThanHours() {
    // Given
    var reader = answering("""
        {"year": 2026, "days": [{"d": 1, "m": 7, "h": "sick"}]}""");

    // When / Then
    assertThatThrownBy(() -> reader.read(SCREENSHOT)).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void reportsATranscriptionWithADayThatDoesNotExist() {
    // Given
    var reader = answering("""
        {"year": 2026, "days": [{"d": 30, "m": 2, "h": "8"}]}""");

    // When / Then
    assertThatThrownBy(() -> reader.read(SCREENSHOT)).isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  @Test
  void reportsAnIncompleteTranscription() {
    assertThatThrownBy(() -> answering("{\"days\": []}").read(SCREENSHOT))
        .isInstanceOf(ClientTimesheetUnreadableException.class);
  }

  private TranscribingClientTimesheetReader answering(String transcription) {
    return new TranscribingClientTimesheetReader(request -> {
      requests.add(request);
      return transcription;
    });
  }
}
