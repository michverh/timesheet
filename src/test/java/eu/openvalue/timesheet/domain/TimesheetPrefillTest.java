package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.AVAILABLE;
import static eu.openvalue.timesheet.domain.HourCategory.PUBLIC_HOLIDAY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
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
  void leavesRegularWeekdaysForTheClientsTimesheetsWhenAssignedToAClient() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED);

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEmpty();
  }

  @Test
  void booksEightAvailableHoursOnRegularWeekdaysWhenOnTheBench() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ON_BENCH);

    // Then
    assertThat(timesheet.day(MONDAY).bookedHours()).isEqualTo(Map.of(AVAILABLE, Hours.of(8)));
  }

  @Test
  void booksAStandardWorkWeekOverTheWholeMonthOnTheBench() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ON_BENCH);

    // Then: 21 weekdays, of which Ascension Day and Whit Monday are public holidays
    assertThat(timesheet.days())
        .filteredOn(day -> day.hours(AVAILABLE).equals(Hours.of(8)))
        .hasSize(19);
  }

  @Test
  void leavesWeekendsEmpty() {
    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED);

    // Then
    assertThat(timesheet.days()).filteredOn(DayEntry::isWeekend)
        .hasSize(10)
        .allSatisfy(day -> assertThat(day.bookedHours()).isEmpty());
  }

  @Test
  void booksPublicHolidaysAsSuch() {
    // Given
    var ascensionDay = LocalDate.of(2026, 5, 14);

    // When
    var timesheet = prefill.prefill(MAY_2026, PROFESSIONAL, ASSIGNED);

    // Then
    assertThat(timesheet.day(ascensionDay).bookedHours()).isEqualTo(Map.of(PUBLIC_HOLIDAY, Hours.of(8)));
  }
}
