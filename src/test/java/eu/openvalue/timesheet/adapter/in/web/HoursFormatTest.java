package eu.openvalue.timesheet.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.openvalue.timesheet.domain.Hours;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HoursFormatTest {

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
