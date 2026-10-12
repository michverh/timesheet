package eu.openvalue.timesheet.adapter.out.employment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.domain.Professional;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ConfiguredEmploymentsTest {

  @Test
  void givesTheConfiguredStartDate() {
    // Given
    var employments = new ConfiguredEmployments(new EmploymentProperties(LocalDate.of(2024, 3, 1)));

    // When / Then
    assertThat(employments.employedSince(Professional.withEmail("jan.jansen@openvalue.nl")))
        .isEqualTo(LocalDate.of(2024, 3, 1));
  }

  @Test
  void needsAStartDate() {
    assertThatThrownBy(() -> new ConfiguredEmployments(new EmploymentProperties(null)))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("timesheet.employment.start-date");
  }
}
