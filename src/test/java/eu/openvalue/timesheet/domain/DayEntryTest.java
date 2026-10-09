package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.OVERTIME;
import static eu.openvalue.timesheet.domain.HourCategory.STAND_BY;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DayEntryTest {

  private static final LocalDate A_DAY = LocalDate.of(2028, 2, 10);

  @Test
  void isBookedAsGiven() {
    // When
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(8)), Kilometres.of(42),
        Optional.of(new Explanation("Visited the client")));

    // Then
    assertThat(day.bookedHours()).containsExactly(Map.entry(WORKED, Hours.of(8)));
    assertThat(day.kilometres()).isEqualTo(Kilometres.of(42));
    assertThat(day.explanation()).contains(new Explanation("Visited the client"));
  }

  @Test
  void leavesCategoriesWithoutHoursOut() {
    // When
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(8), OVERTIME, Hours.ZERO), Kilometres.ZERO,
        Optional.empty());

    // Then
    assertThat(day.bookedHours()).containsOnlyKeys(WORKED);
  }

  @Test
  void cannotHoldMoreThanADayOfHours() {
    assertThatThrownBy(() -> DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(20), OVERTIME, Hours.of(5)), Kilometres.ZERO,
        Optional.empty()))
        .isInstanceOf(DailyHoursLimitExceededException.class);
  }

  @Test
  void fillsTheWorkdayWithAllButOvertimeAndStandBy() {
    // Given
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(6), LEAVE, Hours.of(2), OVERTIME, Hours.of(1),
        STAND_BY, Hours.of(3)), Kilometres.ZERO, Optional.empty());

    // When / Then
    assertThat(day.workdayHours()).isEqualTo(Hours.of(8));
  }

  @Test
  void followsTheStandardWorkdayWhenItsHoursAddUpToEight() {
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(6), LEAVE, Hours.of(2), OVERTIME, Hours.of(2)),
        Kilometres.ZERO, Optional.empty());

    assertThat(day.deviatesFromStandardWorkday()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "7:30", "8:15"})
  void deviatesFromTheStandardWorkdayWhenAWeekdayAddsUpToMoreOrLess(String worked) {
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.parse(worked)), Kilometres.ZERO, Optional.empty());

    assertThat(day.deviatesFromStandardWorkday()).isTrue();
  }

  @Test
  void neverDeviatesFromTheStandardWorkdayInTheWeekend() {
    var saturday = DayEntry.of(LocalDate.of(2028, 2, 12), Map.of(STAND_BY, Hours.of(4)), Kilometres.ZERO,
        Optional.empty());

    assertThat(saturday.deviatesFromStandardWorkday()).isFalse();
  }

  @Test
  void canTakeOverHoursWorkedWhenNothingElseFillsTheWorkday() {
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(8), OVERTIME, Hours.of(1)), Kilometres.ZERO,
        Optional.empty());

    assertThat(day.canTakeOverHoursWorked(Hours.of(7, 30))).isTrue();
  }

  @Test
  void cannotTakeOverHoursWorkedWhenSomethingElseFillsTheWorkday() {
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(4), LEAVE, Hours.of(4)), Kilometres.ZERO, Optional.empty());

    assertThat(day.canTakeOverHoursWorked(Hours.of(8))).isFalse();
  }

  @Test
  void cannotTakeOverHoursWorkedThatWouldExceedADay() {
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(8), STAND_BY, Hours.of(10)), Kilometres.ZERO,
        Optional.empty());

    assertThat(day.canTakeOverHoursWorked(Hours.of(15))).isFalse();
  }
}
