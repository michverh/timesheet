package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  @ParameterizedTest
  @CsvSource({
      "8,      480",
      "7.5,    450",
      "0.25,   15",
      "7.3333, 440",
      "0.0083, 0",
      "0.0084, 1"
  })
  void readsDecimalHoursRoundedToTheNearestMinute(BigDecimal hours, int minutes) {
    assertThat(Hours.ofDecimalHours(hours)).isEqualTo(Hours.ofMinutes(minutes));
  }

  @Test
  void cannotBeReadFromNegativeDecimalHours() {
    assertThatThrownBy(() -> Hours.ofDecimalHours(new BigDecimal("-0.5")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotBeReadFromMoreDecimalHoursThanFitInMinutes() {
    assertThatThrownBy(() -> Hours.ofDecimalHours(new BigDecimal("1E12"))).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({
      "480, 8",
      "450, 7.5",
      "440, 7.3333333333",
      "0,   0"
  })
  void convertsToDecimalHours(int minutes, BigDecimal hours) {
    assertThat(Hours.ofMinutes(minutes).toDecimalHours()).isEqualByComparingTo(hours);
  }

  @ParameterizedTest
  @CsvSource({
      "7:30,  450",
      "7:05,  425",
      "7:5,   425",
      "0:45,  45",
      "8,     480",
      "' 8 ', 480",
      "7.5,   450",
      "'7,5', 450",
      "1.25,  75",
      "0.1,   6",
      "7.33,  440"
  })
  void parsesHoursAndMinutesWholeHoursOrDecimalHoursRoundedToTheMinute(String text, int minutes) {
    assertThat(Hours.parse(text)).isEqualTo(Hours.ofMinutes(minutes));
  }

  @ParameterizedTest
  @ValueSource(strings = {"7:60", "7:", ":30", "-1", "-0.5", "abc", "7h30", "1:2:3"})
  void cannotBeParsedFromAnythingElse(String text) {
    assertThatThrownBy(() -> Hours.parse(text)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void printsAsHoursAndMinutes() {
    assertThat(Hours.of(7, 5)).hasToString("7:05");
  }
}
