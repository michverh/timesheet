package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AbsenceTest {

  @Test
  void cannotEndBeforeItStarts() {
    assertThatThrownBy(() -> new Absence(LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({
      "2026-04-30, false",
      "2026-05-01, true",
      "2026-05-03, true",
      "2026-05-04, false"
  })
  void coversItsFirstAndLastDayInclusive(LocalDate day, boolean covered) {
    // Given
    var absence = new Absence(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));

    // When / Then
    assertThat(absence.covers(day)).isEqualTo(covered);
  }

  @Test
  void coversASingleDay() {
    // Given
    var day = LocalDate.of(2026, 5, 1);

    // When / Then
    assertThat(Absence.on(day).covers(day)).isTrue();
  }
}
