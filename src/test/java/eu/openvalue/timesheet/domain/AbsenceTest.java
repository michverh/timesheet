package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AbsenceTest {

  private static final LocalDate A_DAY = LocalDate.of(2026, 5, 4);

  @Test
  void mustLastLongerThanZeroHours() {
    assertThatThrownBy(() -> Absence.of(A_DAY, AbsenceType.LEAVE, Hours.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void isExplainedByItsDescription() {
    // Given
    var absence = Absence.of(A_DAY, AbsenceType.SPECIAL_LEAVE, Hours.of(8), new Explanation("Wedding"));

    // When / Then
    assertThat(absence.explanation()).contains(new Explanation("Wedding"));
  }

  @Test
  void fallsBackToTheDefaultExplanationOfItsType() {
    // Given
    var absence = Absence.of(A_DAY, AbsenceType.SPECIAL_LEAVE, Hours.of(8));

    // When / Then
    assertThat(absence.explanation()).contains(new Explanation("Bijzonder verlof"));
  }

  @Test
  void isUnexplainedWithoutDescriptionOrDefault() {
    assertThat(Absence.of(A_DAY, AbsenceType.LEAVE, Hours.of(8)).explanation()).isEmpty();
  }
}
