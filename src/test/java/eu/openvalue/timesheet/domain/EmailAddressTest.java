package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTest {

  @Test
  void isCaseInsensitiveAndIgnoresSurroundingWhitespace() {
    assertThat(new EmailAddress(" Jan.Jansen@OpenValue.nl ")).isEqualTo(new EmailAddress("jan.jansen@openvalue.nl"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "jan.jansen", "jan.jansen@", "@openvalue.nl", "jan jansen@openvalue.nl", "jan@openvalue"})
  void rejectsMalformedAddresses(String address) {
    assertThatThrownBy(() -> new EmailAddress(address)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void exposesTheLocalPart() {
    assertThat(new EmailAddress("jan.jansen@openvalue.nl").localPart()).isEqualTo("jan.jansen");
  }
}
