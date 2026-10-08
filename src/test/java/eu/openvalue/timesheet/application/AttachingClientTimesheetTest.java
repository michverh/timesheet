package eu.openvalue.timesheet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AttachingClientTimesheetTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final ClientTimesheetDocument DOCUMENT =
      new ClientTimesheetDocument("acme-may.pdf", DocumentFormat.PDF, new byte[] {1, 2, 3});

  private final FakeTimesheetDrafts drafts = new FakeTimesheetDrafts();
  private final AttachingClientTimesheet attaching = new AttachingClientTimesheet(drafts);

  @Test
  void handsTheClientsTimesheetInWithTheDraft() {
    // Given
    drafts.save(Timesheet.start(MAY_2026, JAN, Optional.of(Assignment.at(new Client("ACME")))));

    // When
    attaching.attach(new AttachClientTimesheetCommand(JAN, MAY_2026, DOCUMENT));

    // Then
    assertThat(drafts.find(JAN, MAY_2026).orElseThrow().clientTimesheet()).contains(DOCUMENT);
  }

  @Test
  void requiresADraftForTheMonth() {
    assertThatThrownBy(() -> attaching.attach(new AttachClientTimesheetCommand(JAN, MAY_2026, DOCUMENT)))
        .isInstanceOf(TimesheetDraftNotFoundException.class);
  }
}
