package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClientTest {

  @ParameterizedTest
  @ValueSource(strings = {"", "  "})
  void cannotBeBlank(String blank) {
    assertThatThrownBy(() -> new Client(blank)).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ACME", "  ACME ", "\tACME\n"})
  void ignoresSurroundingWhitespace(String text) {
    assertThat(new Client(text)).isEqualTo(new Client("ACME"));
  }
}
