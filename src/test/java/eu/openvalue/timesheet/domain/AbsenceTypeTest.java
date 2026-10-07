package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AbsenceTypeTest {

  @ParameterizedTest
  @CsvSource({
      "LEAVE,         LEAVE",
      "SICK,          SICK",
      "TRAINING,      TRAINING",
      "SPECIAL_LEAVE, OTHER",
      "OTHER,         OTHER"
  })
  void isBookedInTheMatchingTimesheetCategory(AbsenceType type, HourCategory category) {
    assertThat(type.category()).isEqualTo(category);
  }
}
