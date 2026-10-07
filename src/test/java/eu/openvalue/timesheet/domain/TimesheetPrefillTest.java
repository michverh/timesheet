package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.AVAILABLE;
import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.OTHER;
import static eu.openvalue.timesheet.domain.HourCategory.PUBLIC_HOLIDAY;
import static eu.openvalue.timesheet.domain.HourCategory.SICK;
import static eu.openvalue.timesheet.domain.HourCategory.TRAINING;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TimesheetPrefillTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);
  private static final Professional PROFESSIONAL = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final Optional<Assignment> ASSIGNED = Optional.of(Assignment.at(new Client("ACME")));
  private static final Optional<Assignment> ON_BENCH = Optional.empty();

  private final TimesheetPrefill prefill = new TimesheetPrefill(new DutchPublicHolidays());

  @Test
  void booksEightWorkedHoursOnRegularWeekdaysWhenAssignedToAClient() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED, List.of());

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(8)));
  }

  @Test
  void booksEightAvailableHoursOnRegularWeekdaysWhenOnTheBench() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ON_BENCH, List.of());

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(AVAILABLE, Hours.of(8)));
  }

  @Test
  void booksAStandardWorkWeekOverTheWholeMonth() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED, List.of());

    // Then: 21 weekdays, of which Ascension Day and Whit Monday are public holidays
    assertThat(timesheet.days())
        .filteredOn(day -> day.hours(WORKED).equals(Hours.of(8)))
        .hasSize(19);
  }

  @Test
  void leavesWeekendsEmptyEvenWhenAbsent() {
    // Given
    var saturday = LocalDate.of(2026, 5, 2);

    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED,
        List.of(Absence.of(saturday, AbsenceType.LEAVE, Hours.of(8))));

    // Then
    assertThat(timesheet.days()).filteredOn(DayEntry::isWeekend)
        .hasSize(10)
        .allSatisfy(day -> assertThat(day.bookedHours()).isEmpty());
  }

  @Test
  void booksPublicHolidaysEvenWhenAbsent() {
    // Given
    var ascensionDay = LocalDate.of(2026, 5, 14);

    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED,
        List.of(Absence.of(ascensionDay, AbsenceType.SICK, Hours.of(8))));

    // Then
    assertThat(timesheet.day(ascensionDay).bookedHours()).isEqualTo(Map.of(PUBLIC_HOLIDAY, Hours.of(8)));
  }

  @Test
  void booksAFullDayAbsenceInsteadOfWorkedHours() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED,
        List.of(Absence.of(MONDAY, AbsenceType.LEAVE, Hours.of(8))));

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(LEAVE, Hours.of(8)));
  }

  @Test
  void booksTheRestOfAPartiallyAbsentDayAsWorked() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED,
        List.of(Absence.of(MONDAY, AbsenceType.OTHER, Hours.of(new BigDecimal("2.5")))));

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours())
        .isEqualTo(Map.of(OTHER, Hours.of(new BigDecimal("2.5")), WORKED, Hours.of(new BigDecimal("5.5"))));
  }

  @Test
  void booksTheRestOfAPartiallyAbsentDayAsAvailableWhenOnTheBench() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ON_BENCH,
        List.of(Absence.of(MONDAY, AbsenceType.SICK, Hours.of(3))));

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(SICK, Hours.of(3), AVAILABLE, Hours.of(5)));
  }

  @Test
  void combinesSeveralAbsencesOnOneDay() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED, List.of(
        Absence.of(MONDAY, AbsenceType.LEAVE, Hours.of(4)),
        Absence.of(MONDAY, AbsenceType.SICK, Hours.of(4))));

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(LEAVE, Hours.of(4), SICK, Hours.of(4)));
  }

  @Test
  void booksNoWorkedHoursWhenAbsencesExceedTheStandardWorkday() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED,
        List.of(Absence.of(MONDAY, AbsenceType.TRAINING, Hours.of(10), new Explanation("Conference"))));

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(TRAINING, Hours.of(10)));
  }

  @Test
  void explainsTheDayWithTheDescriptionOfTheAbsence() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED,
        List.of(Absence.of(MONDAY, AbsenceType.OTHER, Hours.of(2), new Explanation("Doctor appointment"))));

    // Then
    assertThat(timesheet.day(MONDAY).explanation()).contains(new Explanation("Doctor appointment"));
  }

  @Test
  void explainsSpecialLeaveWithoutDescriptionAsSuch() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED,
        List.of(Absence.of(MONDAY, AbsenceType.SPECIAL_LEAVE, Hours.of(8))));

    // Then
    assertThat(timesheet.day(MONDAY).explanation()).contains(new Explanation("Bijzonder verlof"));
  }

  @Test
  void joinsTheExplanationsOfSeveralAbsencesOnOneDay() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED, List.of(
        Absence.of(MONDAY, AbsenceType.OTHER, Hours.of(2), new Explanation("Doctor appointment")),
        Absence.of(MONDAY, AbsenceType.TRAINING, Hours.of(4), new Explanation("Java course"))));

    // Then
    assertThat(timesheet.day(MONDAY).explanation())
        .contains(new Explanation("Doctor appointment; Java course"));
  }

  @Test
  void leavesTheDayUnexplainedWhenNoAbsenceIsDescribed() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED,
        List.of(Absence.of(MONDAY, AbsenceType.LEAVE, Hours.of(8))));

    // Then
    assertThat(timesheet.day(MONDAY).explanation()).isEmpty();
  }
}
