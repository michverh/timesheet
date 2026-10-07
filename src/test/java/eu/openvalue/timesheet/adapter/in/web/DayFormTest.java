package eu.openvalue.timesheet.adapter.in.web;

import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Kilometres;
import eu.openvalue.timesheet.domain.Professional;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.LinkedMultiValueMap;

class DayFormTest {

  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);

  @Test
  void turnsTheInputIntoAnAdjustmentOfTheDay() {
    // Given
    var form = form("hours-WORKED", "6:30", "hours-LEAVE", "1,5",
        "kilometres", "42", "explanation", " Client visit ");

    // When
    var command = form.toCommand(JAN, MONDAY);

    // Then
    assertThat(command.hours()).containsEntry(WORKED, Hours.of(6, 30))
        .containsEntry(LEAVE, Hours.of(1, 30));
    assertThat(command.kilometres()).isEqualTo(Kilometres.of(42));
    assertThat(command.explanation()).contains(new Explanation("Client visit"));
  }

  @Test
  void treatsBlankFieldsAsNothingBooked() {
    // When
    var command = form("hours-WORKED", "", "kilometres", " ", "explanation", "").toCommand(JAN, MONDAY);

    // Then
    assertThat(command.hours()).isEmpty();
    assertThat(command.kilometres()).isEqualTo(Kilometres.ZERO);
    assertThat(command.explanation()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "-1", "7:75", "7.33"})
  void rejectsHoursThatAreNotPositiveHoursAndMinutes(String hours) {
    assertThatThrownBy(() -> form("hours-WORKED", hours).toCommand(JAN, MONDAY))
        .isInstanceOf(InvalidDayInputException.class)
        .hasMessageContaining("worked hours");
  }

  @ParameterizedTest
  @CsvSource({
      "12,     12",
      "12.49,  12",
      "12.5,   13",
      "12.51,  13",
      "'12,5', 13",
      "0.4,    0"
  })
  void roundsKilometresToTheNearestWholeKilometreWithHalvesUp(String input, int kilometres) {
    // When
    var command = form("kilometres", input).toCommand(JAN, MONDAY);

    // Then
    assertThat(command.kilometres()).isEqualTo(Kilometres.of(kilometres));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "-1", "-0.4", "12.5.1"})
  void rejectsKilometresThatAreNotPositiveNumbers(String kilometres) {
    assertThatThrownBy(() -> form("kilometres", kilometres).toCommand(JAN, MONDAY))
        .isInstanceOf(InvalidDayInputException.class)
        .hasMessageContaining("kilometres");
  }

  private static DayForm form(String... namesAndValues) {
    var parameters = new LinkedMultiValueMap<String, String>();
    for (int i = 0; i < namesAndValues.length; i += 2) {
      parameters.add(namesAndValues[i], namesAndValues[i + 1]);
    }
    return DayForm.from(parameters);
  }
}
