package eu.openvalue.timesheet.application;

import static eu.openvalue.timesheet.domain.HourCategory.AVAILABLE;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.TimesheetAlreadySubmittedException;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PreparingTimesheetDraftTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final Professional PIET = Professional.withEmail("piet.pietersen@openvalue.nl");
  private static final Assignment AT_ACME = Assignment.at(new Client("ACME"));

  private final Assignments assignments = (professional, month) ->
      professional.equals(JAN) ? Optional.of(AT_ACME) : Optional.empty();

  private final FakeTimesheets timesheets = new FakeTimesheets();

  private final PreparingTimesheetDraft prepareDraft = new PreparingTimesheetDraft(
      new CurrentTimesheets(new TimesheetPrefill(new DutchPublicHolidays()), assignments, timesheets), timesheets);

  @Test
  void keepsNothingForAMonthThatIsOnlyLookedAt() {
    // When
    prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(timesheets.find(JAN, MAY_2026)).isEmpty();
  }

  @Test
  void keepsNothingWhenResettingAMonthNotKeptYet() {
    // When
    var reset = prepareDraft.resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(reset.month()).isEqualTo(MAY_2026);
    assertThat(timesheets.find(JAN, MAY_2026)).isEmpty();
  }

  @Test
  void givesTheExistingDraftInsteadOfPreparingANewOne() {
    // Given
    var existing = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));
    existing.bookHours(MONDAY, WORKED, Hours.of(2));
    timesheets.save(existing);

    // When
    var timesheet = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(timesheet.day(MONDAY).hours(WORKED)).isEqualTo(Hours.of(2));
  }

  @Test
  void prefillsTheDraftAgainWhenReset() {
    // Given
    var draft = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));
    draft.bookHours(MONDAY.plusDays(1), WORKED, Hours.of(2));
    timesheets.save(draft);

    // When
    prepareDraft.resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().day(MONDAY.plusDays(1)).bookedHours()).isEmpty();
  }

  @Test
  void refusesToResetASubmittedTimesheet() {
    // Given
    var draft = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));
    draft.submit(LocalDate.of(2026, 6, 1));
    timesheets.save(draft);

    // When / Then
    assertThatThrownBy(() -> prepareDraft.resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026)))
        .isInstanceOf(TimesheetAlreadySubmittedException.class);
  }

  @Test
  void keepsACorrectionACorrectionWhenReset() {
    // Given
    var draft = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));
    draft.submit(LocalDate.of(2026, 6, 1));
    draft.reopenForCorrection();
    timesheets.save(draft);

    // When
    var reset = prepareDraft.resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(reset.previouslySubmittedOn()).contains(LocalDate.of(2026, 6, 1));
  }

  @Test
  void draftsTheTimesheetOfTheProfessionalForTheMonth() {
    // When
    var timesheet = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(timesheet.professional()).isEqualTo(JAN);
    assertThat(timesheet.month()).isEqualTo(MAY_2026);
  }

  @Test
  void prefillsTheAssignmentOfTheProfessional() {
    // When
    var timesheet = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(timesheet.assignment()).contains(AT_ACME);
    assertThat(timesheet.isOnBench()).isFalse();
  }

  @Test
  void draftsABenchTimesheetForAProfessionalWithoutAssignment() {
    // When
    var timesheet = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(PIET, MAY_2026));

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(AVAILABLE, Hours.of(8)));
  }

  @Test
  void resetsTheTimesheetAsChangedConcurrently() {
    // Given
    timesheets.save(prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026)));
    timesheets.changeConcurrently(changed -> changed.bookHours(MONDAY.plusDays(1), WORKED, Hours.of(3)));

    // When
    prepareDraft.resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(timesheets.find(JAN, MAY_2026).orElseThrow().day(MONDAY.plusDays(1)).bookedHours()).isEmpty();
  }
}
