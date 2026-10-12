package eu.openvalue.timesheet.application;

import static org.assertj.core.api.Assertions.assertThat;

import eu.openvalue.timesheet.application.port.in.DownloadTimesheetCommand;
import eu.openvalue.timesheet.application.port.out.SpreadsheetFile;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DownloadingTimesheetTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final Professional JAN = Professional.withEmail("jan.van.der.berg@openvalue.nl");

  private final FakeTimesheets timesheets = new FakeTimesheets();
  private final TimesheetSpreadsheet spreadsheet = timesheet -> new SpreadsheetFile("ods",
      "application/vnd.oasis.opendocument.spreadsheet",
      ("spreadsheet of " + timesheet.professional().name() + " for " + timesheet.month())
          .getBytes(StandardCharsets.UTF_8));
  private final DownloadingTimesheet download = new DownloadingTimesheet(timesheets.current(), spreadsheet);

  @Test
  void givesTheDraftAsSpreadsheet() {
    // Given
    timesheets.save(Timesheet.start(MAY_2026, JAN, Optional.empty()));

    // When
    var downloaded = download.download(new DownloadTimesheetCommand(JAN, MAY_2026));

    // Then
    assertThat(new String(downloaded.content(), StandardCharsets.UTF_8))
        .isEqualTo("spreadsheet of Jan van der Berg for 2026-05");
    assertThat(downloaded.mediaType()).isEqualTo("application/vnd.oasis.opendocument.spreadsheet");
  }

  @Test
  void namesTheFileAfterTheMonthAndProfessionalInTheFormatOfTheSpreadsheet() {
    // Given
    timesheets.save(Timesheet.start(MAY_2026, JAN, Optional.empty()));

    // When
    var downloaded = download.download(new DownloadTimesheetCommand(JAN, MAY_2026));

    // Then
    assertThat(downloaded.fileName()).isEqualTo("Urenverantwoording_2026-05_Jan_van_der_Berg.ods");
  }

  @Test
  void downloadsTheMonthAsPrefilledWhenNotKeptYetWithoutKeepingIt() {
    // When
    var downloaded = download.download(new DownloadTimesheetCommand(JAN, MAY_2026));

    // Then
    assertThat(downloaded.fileName()).startsWith("Urenverantwoording_2026-05_");
    assertThat(timesheets.find(JAN, MAY_2026)).isEmpty();
  }
}
