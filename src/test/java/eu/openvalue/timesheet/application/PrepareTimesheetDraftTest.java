package eu.openvalue.timesheet.application;

import static eu.openvalue.timesheet.domain.HourCategory.AVAILABLE;
import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistry;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.domain.Absence;
import eu.openvalue.timesheet.domain.AbsenceType;
import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PrepareTimesheetDraftTest {

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

  private final PrepareTimesheetDraft prepareDraft =
      new PrepareTimesheetDraft(new TimesheetPrefill(new DutchPublicHolidays()), absenceRegistry, assignments);

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
