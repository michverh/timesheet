package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class HoursTest {

  @Test
  void acceptsZero() {
    assertThat(Hours.of(0).isZero()).isTrue();
  }

  @Test
  void addsUp() {
    assertThat(Hours.of(6).plus(Hours.of(2))).isEqualTo(Hours.of(8));
  }

  @Test
  void cannotBeNegative() {
    assertThatThrownBy(() -> Hours.of(-1)).isInstanceOf(IllegalArgumentException.class);
  }
}
