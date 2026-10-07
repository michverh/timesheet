package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HoursTest {

  @Test
  void consistsOfWholeHoursAndRemainingMinutes() {
    // Given
    var hours = Hours.of(7, 20);

    // When / Then
    assertThat(hours.wholeHours()).isEqualTo(7);
    assertThat(hours.remainingMinutes()).isEqualTo(20);
    assertThat(hours.inMinutes()).isEqualTo(440);
  }

  @Test
  void equalsTheSameAmountOfTime() {
    assertThat(Hours.of(8, 0)).isEqualTo(Hours.of(8)).isEqualTo(Hours.ofMinutes(480));
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 60, 75})
  void rejectsMinutesOutsideAnHour(int minutes) {
    assertThatThrownBy(() -> Hours.of(7, minutes)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotBeNegative() {
    assertThatThrownBy(() -> Hours.of(-1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void carriesMinutesOverIntoHoursWhenAddingUp() {
    assertThat(Hours.of(7, 20).plus(Hours.of(7, 50))).isEqualTo(Hours.of(15, 10));
  }

  @Test
  void borrowsAnHourWhenSubtractingMoreMinutes() {
    assertThat(Hours.of(8).minus(Hours.of(2, 45))).isEqualTo(Hours.of(5, 15));
  }

  @Test
  void cannotSubtractMoreThanItHas() {
    assertThatThrownBy(() -> Hours.of(2).minus(Hours.of(2, 1))).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void printsAsHoursAndMinutes() {
    assertThat(Hours.of(7, 5)).hasToString("7:05");
  }
}
