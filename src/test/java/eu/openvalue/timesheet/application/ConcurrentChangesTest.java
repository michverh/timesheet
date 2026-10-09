package eu.openvalue.timesheet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.application.port.out.TimesheetChangedConcurrentlyException;
import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConcurrentChangesTest {

  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");

  @Test
  void appliesTheChangeAgainUntilNoOtherChangeGetsInBetween() {
    // Given
    var attempts = new AtomicInteger();

    // When
    var result = ConcurrentChanges.retried(() -> {
      if (attempts.incrementAndGet() < ConcurrentChanges.ATTEMPTS) {
        throw conflict();
      }
      return "kept";
    });

    // Then
    assertThat(result).isEqualTo("kept");
  }

  @Test
  void givesUpWhenOtherChangesKeepGettingInBetween() {
    // Given
    var attempts = new AtomicInteger();

    // When / Then
    assertThatThrownBy(() -> ConcurrentChanges.retried(() -> {
      attempts.incrementAndGet();
      throw conflict();
    })).isInstanceOf(TimesheetChangedConcurrentlyException.class);
    assertThat(attempts).hasValue(ConcurrentChanges.ATTEMPTS);
  }

  private static TimesheetChangedConcurrentlyException conflict() {
    return new TimesheetChangedConcurrentlyException(JAN, YearMonth.of(2026, 5));
  }
}
