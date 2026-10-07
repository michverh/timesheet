package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HoursTest {

  @ParameterizedTest
  @ValueSource(strings = {"0", "0.25", "0.5", "0.75", "7.5", "8.00", "24"})
  void acceptsQuartersOfAnHour(String hours) {
    assertThat(Hours.of(new BigDecimal(hours)).toDecimal()).isEqualByComparingTo(hours);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.1", "7.33", "0.125"})
  void rejectsFractionsThatAreNotQuarters(String hours) {
    assertThatThrownBy(() -> Hours.of(new BigDecimal(hours))).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void equalsTheSameAmountOfWholeHours() {
    assertThat(Hours.of(new BigDecimal("8.00"))).isEqualTo(Hours.of(8));
  }

  @Test
  void cannotBeNegative() {
    assertThatThrownBy(() -> Hours.of(-1)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void addsUp() {
    assertThat(Hours.of(6).plus(Hours.of(new BigDecimal("1.75")))).isEqualTo(Hours.of(new BigDecimal("7.75")));
  }

  @Test
  void subtracts() {
    assertThat(Hours.of(8).minus(Hours.of(new BigDecimal("2.5")))).isEqualTo(Hours.of(new BigDecimal("5.5")));
  }

  @Test
  void cannotSubtractMoreThanItHas() {
    assertThatThrownBy(() -> Hours.of(2).minus(Hours.of(3))).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void printsAsDecimalHours() {
    assertThat(Hours.of(new BigDecimal("7.50"))).hasToString("7.5h");
  }
}
