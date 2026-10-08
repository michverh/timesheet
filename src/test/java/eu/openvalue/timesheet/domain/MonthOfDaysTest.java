package eu.openvalue.timesheet.domain;

import static eu.openvalue.timesheet.domain.HourCategory.TRAINING;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MonthOfDaysTest {

  private static final YearMonth FEBRUARY_2028 = YearMonth.of(2028, 2);
  private static final LocalDate A_DAY = LocalDate.of(2028, 2, 10);

  private final MonthOfDays days = MonthOfDays.emptyFor(FEBRUARY_2028);

  @ParameterizedTest
  @CsvSource({
      "2028-02, 29",
      "2027-02, 28",
      "2026-04, 30",
      "2026-05, 31"
  })
  void hasAnEmptyEntryForEveryDayOfTheMonth(YearMonth month, int numberOfDays) {
    assertThat(MonthOfDays.emptyFor(month).all())
        .hasSize(numberOfDays)
        .allSatisfy(day -> assertThat(day.bookedHours()).isEmpty())
        .extracting(DayEntry::date)
        .isSorted()
        .allSatisfy(date -> assertThat(YearMonth.from(date)).isEqualTo(month));
  }

  @Test
  void replacesTheEntryOfADay() {
    // When
    var replaced = days.with(DayEntry.empty(A_DAY).withHours(WORKED, Hours.of(8)));

    // Then
    assertThat(replaced.day(A_DAY).hours(WORKED)).isEqualTo(Hours.of(8));
  }

  @Test
  void staysUnchangedWhenAnEntryIsReplaced() {
    // When
    days.with(DayEntry.empty(A_DAY).withHours(WORKED, Hours.of(8)));

    // Then
    assertThat(days.day(A_DAY).bookedHours()).isEmpty();
  }

  @Test
  void rejectsDaysOfAnotherMonth() {
    assertThatThrownBy(() -> days.day(LocalDate.of(2028, 3, 1))).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsEntriesOfAnotherMonth() {
    assertThatThrownBy(() -> days.with(DayEntry.empty(LocalDate.of(2028, 3, 1))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void totalsTheHoursOfACategory() {
    // Given
    var booked = days
        .with(DayEntry.empty(A_DAY).withHours(WORKED, Hours.of(7, 40)))
        .with(DayEntry.empty(A_DAY.plusDays(1)).withHours(WORKED, Hours.of(7, 35)));

    // When / Then
    assertThat(booked.totalHours(WORKED)).isEqualTo(Hours.of(15, 15));
  }

  @Test
  void totalsTheKilometres() {
    // Given
    var driven = days
        .with(DayEntry.empty(A_DAY).withKilometres(Kilometres.of(42)))
        .with(DayEntry.empty(A_DAY.plusDays(2)).withKilometres(Kilometres.of(58)));

    // When / Then
    assertThat(driven.totalKilometres()).isEqualTo(Kilometres.of(100));
  }

  @Test
  void findsTheDaysLackingARequiredExplanation() {
    // Given
    var withTraining = days.with(DayEntry.empty(A_DAY).withHours(TRAINING, Hours.of(8)));

    // When / Then
    assertThat(withTraining.daysLackingRequiredExplanation()).containsExactly(A_DAY);
  }
}
