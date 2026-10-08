package eu.openvalue.timesheet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetReader;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttachingClientTimesheetTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final ClientTimesheetDocument DOCUMENT =
      new ClientTimesheetDocument("acme-may.png", DocumentFormat.PNG, new byte[] {1, 2, 3});
  private static final ClientReportedHours REPORTED =
      new ClientReportedHours(Map.of(LocalDate.of(2026, 5, 4), Hours.of(8)));

  private final FakeTimesheetDrafts drafts = new FakeTimesheetDrafts();

  @BeforeEach
  void startDraft() {
    drafts.save(Timesheet.start(MAY_2026, JAN, Optional.of(Assignment.at(new Client("ACME")))));
  }

  @Test
  void handsTheClientsTimesheetInWithTheDraft() {
    // When
    attaching(document -> REPORTED).attach(new AttachClientTimesheetCommand(JAN, MAY_2026, DOCUMENT));

    // Then
    assertThat(draft().clientTimesheet()).contains(DOCUMENT);
  }

  @Test
  void readsTheHoursTheClientReported() {
    // When
    attaching(document -> REPORTED).attach(new AttachClientTimesheetCommand(JAN, MAY_2026, DOCUMENT));

    // Then
    assertThat(draft().hoursReportedByClient()).contains(REPORTED);
  }

  @Test
  void handsInAClientsTimesheetThatCannotBeRead() {
    // Given
    ClientTimesheetReader unreadable = document -> {
      throw new ClientTimesheetUnreadableException("Too blurry");
    };

    // When
    attaching(unreadable).attach(new AttachClientTimesheetCommand(JAN, MAY_2026, DOCUMENT));

    // Then
    assertThat(draft().clientTimesheet()).contains(DOCUMENT);
    assertThat(draft().isClientTimesheetUnreadable()).isTrue();
  }

  @Test
  void requiresADraftForTheMonth() {
    // Given
    var nextMonth = new AttachClientTimesheetCommand(JAN, MAY_2026.plusMonths(1), DOCUMENT);

    // When / Then
    assertThatThrownBy(() -> attaching(document -> REPORTED).attach(nextMonth))
        .isInstanceOf(TimesheetDraftNotFoundException.class);
  }

  private AttachingClientTimesheet attaching(ClientTimesheetReader reader) {
    return new AttachingClientTimesheet(drafts, reader);
  }

  private Timesheet draft() {
    return drafts.find(JAN, MAY_2026).orElseThrow();
  }
}
