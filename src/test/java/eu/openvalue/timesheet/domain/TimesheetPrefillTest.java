package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.AVAILABLE;
import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.PUBLIC_HOLIDAY;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TimesheetPrefillTest {

  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);
  private static final Professional PROFESSIONAL = new Professional("Jan Jansen");
  private static final Optional<Assignment> ASSIGNED = Optional.of(Assignment.at(new Client("ACME")));

  private final TimesheetPrefill prefill = new TimesheetPrefill(new DutchPublicHolidays());

  @Test
  void booksEightWorkedHoursOnRegularWeekdaysWhenAssignedToAClient() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED, List.of());

    // Then
    assertThat(timesheet.day(LocalDate.of(2026, 5, 4)).bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(8)));
  }

  @Test
  void booksEightAvailableHoursOnRegularWeekdaysWhenOnTheBench() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, Optional.empty(), List.of());

    // Then
    assertThat(timesheet.day(LocalDate.of(2026, 5, 4)).bookedHours()).isEqualTo(Map.of(AVAILABLE, Hours.of(8)));
  }

  @Test
  void leavesWeekendsEmpty() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED, List.of(Absence.on(LocalDate.of(2026, 5, 2))));

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
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED, List.of(Absence.on(ascensionDay)));

    // Then
    assertThat(timesheet.day(ascensionDay).bookedHours()).isEqualTo(Map.of(PUBLIC_HOLIDAY, Hours.of(8)));
  }

  @Test
  void booksLeaveOnEveryWeekdayCoveredByAnAbsence() {
    // Given
    var absenceAcrossMonthStart = new Absence(LocalDate.of(2026, 4, 28), LocalDate.of(2026, 5, 4));

    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED, List.of(absenceAcrossMonthStart));

    // Then
    assertThat(timesheet.days())
        .filteredOn(day -> day.hours(LEAVE).equals(Hours.of(8)))
        .extracting(DayEntry::date)
        .containsExactly(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4));
  }

  @Test
  void booksAStandardWorkWeekOverTheWholeMonth() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED, List.of());

    // Then: 21 weekdays, of which Ascension Day and Whit Monday are public holidays
    assertThat(timesheet.days())
        .extracting(day -> day.hours(WORKED).value())
        .filteredOn(hours -> hours == 8)
        .hasSize(19);
  }
}
