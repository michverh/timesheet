package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.OVERTIME;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkWeekTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);

  @Test
  void isNumberedAsInIso8601() {
    assertThat(new WorkWeek(List.of(day(0, Map.of()))).number()).isEqualTo(19);
  }

  @Test
  void expectsAStandardWorkdayForEachOfItsWeekdays() {
    // Given: the week of Friday 1 May 2026 within May
    var week = new WorkWeek(List.of(
        DayEntry.empty(LocalDate.of(2026, 5, 1)), DayEntry.empty(LocalDate.of(2026, 5, 2)),
        DayEntry.empty(LocalDate.of(2026, 5, 3))));

    // When / Then
    assertThat(week.standardHours()).isEqualTo(Hours.of(8));
  }

  @Test
  void addsUpTheHoursFillingTheWorkday() {
    // Given
    var week = new WorkWeek(List.of(
        day(0, Map.of(WORKED, Hours.of(6), LEAVE, Hours.of(2), OVERTIME, Hours.of(3))),
        day(1, Map.of(WORKED, Hours.of(7, 30)))));

    // When / Then
    assertThat(week.bookedHours()).isEqualTo(Hours.of(15, 30));
  }

  @Test
  void followsTheStandardWorkWeekHoweverItsHoursAreSpread() {
    // Given
    var week = new WorkWeek(List.of(day(0, Map.of(WORKED, Hours.of(10))), day(1, Map.of(WORKED, Hours.of(6)))));

    // When / Then
    assertThat(week.deviatesFromStandardWorkWeek()).isFalse();
    assertThat(week.shortfall()).isEqualTo(Hours.ZERO);
  }

  @Test
  void isShortOfTheHoursMissingFromTheStandardWorkWeek() {
    // Given
    var week = new WorkWeek(List.of(day(0, Map.of(WORKED, Hours.of(8))), day(1, Map.of(WORKED, Hours.of(5)))));

    // When / Then
    assertThat(week.deviatesFromStandardWorkWeek()).isTrue();
    assertThat(week.shortfall()).isEqualTo(Hours.of(3));
  }

  @Test
  void isShortOfNothingWhenMoreWasBooked() {
    // Given
    var week = new WorkWeek(List.of(day(0, Map.of(WORKED, Hours.of(9)))));

    // When / Then
    assertThat(week.deviatesFromStandardWorkWeek()).isTrue();
    assertThat(week.shortfall()).isEqualTo(Hours.ZERO);
  }

  @Test
  void hasAtLeastOneDay() {
    assertThatThrownBy(() -> new WorkWeek(List.of())).isInstanceOf(IllegalArgumentException.class);
  }

  private static DayEntry day(int daysAfterMonday, Map<HourCategory, Hours> hours) {
    return DayEntry.of(MONDAY.plusDays(daysAfterMonday), hours, Kilometres.ZERO, Optional.empty());
  }
}
