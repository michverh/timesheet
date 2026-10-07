package eu.openvalue.timesheet.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.domain.Hours;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class HoursFormatTest {

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
      "0.1,   6"
  })
  void readsHoursAndMinutesWholeHoursOrDecimalHours(String text, int minutes) {
    assertThat(HoursFormat.parse(text)).isEqualTo(Hours.ofMinutes(minutes));
  }

  @ParameterizedTest
  @ValueSource(strings = {"7:60", "7:", ":30", "-1", "7.33", "abc", "7h30", "1:2:3"})
  void rejectsAnythingElse(String text) {
    assertThatThrownBy(() -> HoursFormat.parse(text)).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({
      "480, 8",
      "450, 7:30",
      "425, 7:05",
      "45,  0:45",
      "10470, 174:30"
  })
  void showsWholeHoursAsANumberAndOtherwiseHoursAndMinutes(int minutes, String text) {
    assertThat(HoursFormat.format(Hours.ofMinutes(minutes))).isEqualTo(text);
  }
}
