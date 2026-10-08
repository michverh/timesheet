package eu.openvalue.timesheet.application;

import static eu.openvalue.timesheet.domain.HourCategory.AVAILABLE;
import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistry;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistryUnavailableException;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.domain.Absence;
import eu.openvalue.timesheet.domain.AbsenceType;
import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.TimesheetAlreadySubmittedException;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PreparingTimesheetDraftTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);
  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final Professional PIET = Professional.withEmail("piet.pietersen@openvalue.nl");
  private static final Assignment AT_ACME = Assignment.at(new Client("ACME"));

  private final AbsenceRegistry absenceRegistry = (professional, month) ->
      professional.equals(JAN) && month.equals(MAY_2026)
          ? List.of(Absence.of(MONDAY, AbsenceType.LEAVE, Hours.of(8)))
          : List.of();
  private final Assignments assignments = (professional, month) ->
      professional.equals(JAN) ? Optional.of(AT_ACME) : Optional.empty();

  private final FakeTimesheetDrafts drafts = new FakeTimesheetDrafts();

  private final PreparingTimesheetDraft prepareDraft = new PreparingTimesheetDraft(
      new TimesheetPrefill(new DutchPublicHolidays()), absenceRegistry, assignments, drafts);

  @Test
  void keepsTheNewDraft() {
    // When
    var timesheet = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(drafts.find(JAN, MAY_2026)).containsSame(timesheet);
  }

  @Test
  void givesTheExistingDraftInsteadOfPreparingANewOne() {
    // Given
    var existing = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));
    existing.bookHours(MONDAY, WORKED, Hours.of(2));

    // When
    var timesheet = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(timesheet.day(MONDAY).hours(WORKED)).isEqualTo(Hours.of(2));
  }

  @Test
  void prefillsTheDraftAgainWhenReset() {
    // Given
    prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026))
        .bookHours(MONDAY.plusDays(1), WORKED, Hours.of(2));

    // When
    var reset = prepareDraft.resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(reset.day(MONDAY.plusDays(1)).hours(WORKED)).isEqualTo(Hours.of(8));
    assertThat(drafts.find(JAN, MAY_2026)).containsSame(reset);
  }

  @Test
  void refusesToResetASubmittedTimesheet() {
    // Given
    prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026)).submit(LocalDate.of(2026, 6, 1));

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

    // When
    var reset = prepareDraft.resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(reset.previouslySubmittedOn()).contains(LocalDate.of(2026, 6, 1));
  }

  @Test
  void prefillsAsIfThereWereNoAbsencesWhenTheRegistryCannotBeReached() {
    // Given
    var preparing = new PreparingTimesheetDraft(
        new TimesheetPrefill(new DutchPublicHolidays()), unreachableRegistry(), assignments, drafts);

    // When
    var draft = preparing.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(draft.day(MONDAY).bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(8)));
    assertThat(draft.registeredAbsencesUnknown()).isTrue();
    assertThat(drafts.find(JAN, MAY_2026)).containsSame(draft);
  }

  @Test
  void prefillsTheRegisteredAbsencesWhenResetOnceTheRegistryIsReachableAgain() {
    // Given
    new PreparingTimesheetDraft(new TimesheetPrefill(new DutchPublicHolidays()), unreachableRegistry(), assignments,
        drafts).prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));

    // When
    var reset = prepareDraft.resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(reset.day(MONDAY).bookedHours()).isEqualTo(Map.of(LEAVE, Hours.of(8)));
    assertThat(reset.registeredAbsencesUnknown()).isFalse();
  }

  private static AbsenceRegistry unreachableRegistry() {
    return (professional, month) -> {
      throw new AbsenceRegistryUnavailableException("Registry down", null);
    };
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
    assertThat(timesheet.day(MONDAY.plusDays(1)).bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(8)));
  }

  @Test
  void prefillsTheRegisteredAbsencesOfTheProfessional() {
    // When
    var timesheet = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026));

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(LEAVE, Hours.of(8)));
  }

  @Test
  void draftsABenchTimesheetWithoutAbsencesForAnotherProfessional() {
    // When
    var timesheet = prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(PIET, MAY_2026));

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(AVAILABLE, Hours.of(8)));
  }
}
