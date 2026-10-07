package eu.openvalue.timesheet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.in.SubmitTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.in.TimesheetSubmissionFailedException;
import eu.openvalue.timesheet.application.port.out.SpreadsheetFile;
import eu.openvalue.timesheet.application.port.out.TimesheetDelivery;
import eu.openvalue.timesheet.application.port.out.TimesheetDeliveryFailedException;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import eu.openvalue.timesheet.application.port.out.TimesheetSubmission;
import eu.openvalue.timesheet.domain.FutureMonthNotSubmittableException;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubmittingTimesheetTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate TODAY = LocalDate.of(2026, 6, 2);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");

  private final FakeTimesheetDrafts drafts = new FakeTimesheetDrafts();
  private final SpreadsheetFile file = new SpreadsheetFile("ods", "application/vnd.oasis.opendocument.spreadsheet",
      new byte[] {1, 2, 3});
  private final TimesheetSpreadsheet spreadsheet = timesheet -> file;
  private final List<TimesheetSubmission> delivered = new ArrayList<>();
  private final Clock clock = Clock.fixed(Instant.parse("2026-06-02T08:00:00Z"), ZoneId.of("Europe/Amsterdam"));

  private SubmittingTimesheet submitTimesheet(TimesheetDelivery delivery) {
    return new SubmittingTimesheet(drafts, spreadsheet, delivery, clock);
  }

  private final SubmittingTimesheet submit = submitTimesheet(delivered::add);

  @Test
  void handsTheSpreadsheetInWithTheCompanysFileName() {
    // Given
    drafts.save(Timesheet.start(MAY_2026, JAN, Optional.empty()));

    // When
    submit.submit(new SubmitTimesheetCommand(JAN, MAY_2026));

    // Then
    assertThat(delivered).singleElement().satisfies(submission -> {
      assertThat(submission.professional()).isEqualTo(JAN);
      assertThat(submission.month()).isEqualTo(MAY_2026);
      assertThat(submission.submittedOn()).isEqualTo(TODAY);
      assertThat(submission.isCorrection()).isFalse();
      assertThat(submission.fileName()).isEqualTo("Urenverantwoording_2026-05_Jan_Jansen.ods");
      assertThat(submission.spreadsheet()).isSameAs(file);
    });
  }

  @Test
  void marksTheTimesheetSubmittedToday() {
    // Given
    drafts.save(Timesheet.start(MAY_2026, JAN, Optional.empty()));

    // When
    submit.submit(new SubmitTimesheetCommand(JAN, MAY_2026));

    // Then
    assertThat(drafts.find(JAN, MAY_2026).orElseThrow().submittedOn()).contains(TODAY);
  }

  @Test
  void handsACorrectionInAsReplacingTheEarlierSubmission() {
    // Given
    var timesheet = Timesheet.start(MAY_2026, JAN, Optional.empty());
    timesheet.submit(LocalDate.of(2026, 5, 29));
    timesheet.reopenForCorrection();
    drafts.save(timesheet);

    // When
    submit.submit(new SubmitTimesheetCommand(JAN, MAY_2026));

    // Then
    assertThat(delivered).singleElement()
        .extracting(TimesheetSubmission::replacesSubmissionOf)
        .isEqualTo(Optional.of(LocalDate.of(2026, 5, 29)));
  }

  @Test
  void leavesTheTimesheetADraftWhenItCannotBeHandedIn() {
    // Given
    drafts.save(Timesheet.start(MAY_2026, JAN, Optional.empty()));
    var failingSubmit = submitTimesheet(submission -> {
      throw new TimesheetDeliveryFailedException("Mail server unavailable", null);
    });

    // When / Then
    assertThatThrownBy(() -> failingSubmit.submit(new SubmitTimesheetCommand(JAN, MAY_2026)))
        .isInstanceOf(TimesheetSubmissionFailedException.class);
    assertThat(drafts.find(JAN, MAY_2026).orElseThrow().isSubmitted()).isFalse();
  }

  @Test
  void handsNothingInWhenTheTimesheetCannotBeSubmitted() {
    // Given
    var nextMonth = YearMonth.of(2026, 7);
    drafts.save(Timesheet.start(nextMonth, JAN, Optional.empty()));

    // When / Then
    assertThatThrownBy(() -> submit.submit(new SubmitTimesheetCommand(JAN, nextMonth)))
        .isInstanceOf(FutureMonthNotSubmittableException.class);
    assertThat(delivered).isEmpty();
  }

  @Test
  void requiresADraftForTheMonth() {
    assertThatThrownBy(() -> submit.submit(new SubmitTimesheetCommand(JAN, MAY_2026)))
        .isInstanceOf(TimesheetDraftNotFoundException.class);
  }
}
