package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DutchPublicHolidaysTest {

  private final DutchPublicHolidays publicHolidays = new DutchPublicHolidays();

  @Test
  void observesTheDutchPublicHolidaysOf2026() {
    // When
    var holidays = publicHolidays.observedIn(2026);

    // Then
    assertThat(holidays).containsExactlyInAnyOrder(
        LocalDate.of(2026, 1, 1),   // New Year's Day
        LocalDate.of(2026, 4, 6),   // Easter Monday
        LocalDate.of(2026, 4, 27),  // King's Day
        LocalDate.of(2026, 5, 14),  // Ascension Day
        LocalDate.of(2026, 5, 25),  // Whit Monday
        LocalDate.of(2026, 12, 25), // Christmas Day
        LocalDate.of(2026, 12, 26)  // Boxing Day
    );
  }

  @Test
  void observesLiberationDayInLustrumYearsAndMovesKingsDayOffSunday() {
    // When
    var holidays = publicHolidays.observedIn(2025);

    // Then
    assertThat(holidays).containsExactlyInAnyOrder(
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 4, 21),
        LocalDate.of(2025, 4, 26), // King's Day, as 27 April 2025 is a Sunday
        LocalDate.of(2025, 5, 5),  // Liberation Day
        LocalDate.of(2025, 5, 29),
        LocalDate.of(2025, 6, 9),
        LocalDate.of(2025, 12, 25),
        LocalDate.of(2025, 12, 26)
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {"2024-04-01", "2019-04-22", "2038-04-26", "2030-05-05"})
  void recognisesPublicHolidays(LocalDate date) {
    assertThat(publicHolidays.isPublicHoliday(date)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "2026-04-03", // Good Friday
      "2026-05-05", // Liberation Day outside a lustrum year
      "2026-04-05", // Easter Sunday is not an extra day off
      "2026-10-06"
  })
  void doesNotTreatOtherDaysAsPublicHolidays(LocalDate date) {
    assertThat(publicHolidays.isPublicHoliday(date)).isFalse();
  }
}
