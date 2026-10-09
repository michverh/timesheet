package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.OVERTIME;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
}
