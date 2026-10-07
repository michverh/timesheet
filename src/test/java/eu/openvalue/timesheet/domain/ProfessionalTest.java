package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProfessionalTest {

  @ParameterizedTest
  @CsvSource({
      "jan.jansen@openvalue.nl,          Jan Jansen",
      "anne-marie.de-vries@openvalue.nl, Anne-Marie De-Vries",
      "jan.van.der.berg@openvalue.nl,    Jan van der Berg",
      "piet.de.jong@openvalue.nl,        Piet de Jong",
      "kees.van.den.ende@openvalue.nl,   Kees van den Ende",
      "Jan.Jansen@OpenValue.nl,          Jan Jansen"
  })
  void derivesTheNameFromTheEmailAddress(String email, String name) {
    assertThat(Professional.withEmail(email).name()).isEqualTo(name);
  }

  @ParameterizedTest
  @ValueSource(strings = {"jan@openvalue.nl", "jan.@openvalue.nl", ".jansen@openvalue.nl", "jan..jansen@openvalue.nl"})
  void requiresAFirstAndLastNameInTheEmailAddress(String email) {
    assertThatThrownBy(() -> Professional.withEmail(email)).isInstanceOf(IllegalArgumentException.class);
  }
}
