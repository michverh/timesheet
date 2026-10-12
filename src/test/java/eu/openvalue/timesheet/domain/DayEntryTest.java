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
  void hasNoSuggestedLeaveUnlessRestoredSo() {
    var day = DayEntry.of(A_DAY, Map.of(LEAVE, Hours.of(8)), Kilometres.ZERO, Optional.empty());

    assertThat(day.isLeaveSuggested()).isFalse();
  }

  @Test
  void canBeRestoredWithSuggestedLeave() {
    var day = DayEntry.of(A_DAY, Map.of(LEAVE, Hours.of(8)), Kilometres.ZERO, Optional.empty(), true);

    assertThat(day.isLeaveSuggested()).isTrue();
  }

  @Test
  void cannotHaveSuggestedLeaveWithoutLeave() {
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(8)), Kilometres.ZERO, Optional.empty(), true);

    assertThat(day.isLeaveSuggested()).isFalse();
  }

  @Test
  void bookingSuggestedLeaveMarksItAsSuch() {
    // When
    var day = DayEntry.empty(A_DAY).withHours(WORKED, Hours.of(6)).withSuggestedLeave(Hours.of(2));

    // Then
    assertThat(day.bookedHours()).isEqualTo(Map.of(WORKED, Hours.of(6), LEAVE, Hours.of(2)));
    assertThat(day.isLeaveSuggested()).isTrue();
  }

  @Test
  void withdrawsSuggestedLeaveThatWasNotConfirmed() {
    // Given
    var day = DayEntry.empty(A_DAY).withSuggestedLeave(Hours.of(8));

    // When
    var withdrawn = day.withoutSuggestedLeave();

    // Then
    assertThat(withdrawn.bookedHours()).isEmpty();
    assertThat(withdrawn.isLeaveSuggested()).isFalse();
  }

  @Test
  void keepsConfirmedLeave() {
    // Given
    var day = DayEntry.empty(A_DAY).withSuggestedLeave(Hours.of(8)).confirmed();

    // When / Then
    assertThat(day.withoutSuggestedLeave().hours(LEAVE)).isEqualTo(Hours.of(8));
  }

  @Test
  void isShortOfTheHoursMissingFromAStandardWorkday() {
    var day = DayEntry.of(A_DAY, Map.of(WORKED, Hours.of(5), OVERTIME, Hours.of(3)), Kilometres.ZERO,
        Optional.empty());

    assertThat(day.shortOfStandardWorkday()).isEqualTo(Hours.of(3));
  }

  @Test
  void isShortOfNothingInTheWeekend() {
    assertThat(DayEntry.empty(LocalDate.of(2028, 2, 12)).shortOfStandardWorkday()).isEqualTo(Hours.ZERO);
  }
}
