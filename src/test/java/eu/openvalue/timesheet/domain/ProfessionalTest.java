package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProfessionalTest {

  @ParameterizedTest
  @ValueSource(strings = {"", "  "})
  void cannotBeBlank(String blank) {
    assertThatThrownBy(() -> new Professional(blank)).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ACME", "  ACME ", "\tACME\n"})
  void ignoresSurroundingWhitespace(String text) {
    assertThat(new Professional(text)).isEqualTo(new Professional("ACME"));
  }
}
