package eu.openvalue.timesheet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.in.CorrectTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CorrectingTimesheetTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");

  private final FakeTimesheetDrafts drafts = new FakeTimesheetDrafts();
  private final CorrectingTimesheet correct = new CorrectingTimesheet(drafts);

  @Test
  void reopensTheSubmittedTimesheet() {
    // Given
    var timesheet = Timesheet.start(MAY_2026, JAN, Optional.empty());
    timesheet.submit(LocalDate.of(2026, 6, 1));
    drafts.save(timesheet);

    // When
    correct.reopenForCorrection(new CorrectTimesheetCommand(JAN, MAY_2026));

    // Then
    var reopened = drafts.find(JAN, MAY_2026).orElseThrow();
    assertThat(reopened.isSubmitted()).isFalse();
    assertThat(reopened.previouslySubmittedOn()).contains(LocalDate.of(2026, 6, 1));
  }

  @Test
  void requiresATimesheetForTheMonth() {
    assertThatThrownBy(() -> correct.reopenForCorrection(new CorrectTimesheetCommand(JAN, MAY_2026)))
        .isInstanceOf(TimesheetDraftNotFoundException.class);
  }
}
