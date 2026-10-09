package eu.openvalue.timesheet.application;

import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.in.TakeOverClientHoursCommand;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TakingOverClientHoursTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");

  private final FakeTimesheets timesheets = new FakeTimesheets();
  private final TakingOverClientHours takeOver = new TakingOverClientHours(timesheets);

  @Test
  void keepsTheHoursWorkedAccordingToTheClient() {
    // Given
    var draft = Timesheet.start(MAY_2026, JAN, Optional.of(Assignment.at(new Client("ACME"))));
    draft.bookHours(MONDAY, WORKED, Hours.of(8));
    draft.attachClientTimesheet(new ClientTimesheetDocument("acme-may.pdf", DocumentFormat.PDF, new byte[] {1}));
    draft.recordHoursReportedByClient(new ClientReportedHours(Map.of(MONDAY, Hours.of(7, 30))));
    timesheets.save(draft);

    // When
    takeOver.takeOverClientHours(new TakeOverClientHoursCommand(JAN, MAY_2026));

    // Then
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().day(MONDAY).hours(WORKED)).isEqualTo(Hours.of(7, 30));
  }

  @Test
  void requiresATimesheetForTheMonth() {
    assertThatThrownBy(() -> takeOver.takeOverClientHours(new TakeOverClientHoursCommand(JAN, MAY_2026)))
        .isInstanceOf(TimesheetDraftNotFoundException.class);
  }
}
