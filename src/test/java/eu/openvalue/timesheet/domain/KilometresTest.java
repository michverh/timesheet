package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KilometresTest {

  @Test
  void acceptsZero() {
    assertThat(Kilometres.of(0).isZero()).isTrue();
  }

  @Test
  void cannotBeNegative() {
    assertThatThrownBy(() -> Kilometres.of(-1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addsUp() {
    assertThat(Kilometres.of(42).plus(Kilometres.of(58))).isEqualTo(Kilometres.of(100));
  }
}
