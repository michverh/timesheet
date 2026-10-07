package eu.openvalue.timesheet.adapter.out.assignment;

import static org.assertj.core.api.Assertions.assertThat;

import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Project;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConfiguredAssignmentsTest {

  private static final Professional PROFESSIONAL = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final YearMonth MONTH = YearMonth.of(2026, 5);

  @Test
  void assignsTheConfiguredClientAndProject() {
    // Given
    var assignments = new ConfiguredAssignments(new AssignmentProperties("ACME", "Platform"));

    // When / Then
    assertThat(assignments.assignmentOf(PROFESSIONAL, MONTH))
        .contains(Assignment.at(new Client("ACME"), new Project("Platform")));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "  "})
  void assignsTheConfiguredClientWithoutProject(String project) {
    // Given
    var assignments = new ConfiguredAssignments(new AssignmentProperties("ACME", project));

    // When / Then
    assertThat(assignments.assignmentOf(PROFESSIONAL, MONTH)).contains(Assignment.at(new Client("ACME")));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "  "})
  void putsTheProfessionalOnTheBenchWithoutConfiguredClient(String client) {
    // Given
    var assignments = new ConfiguredAssignments(new AssignmentProperties(client, "Platform"));

    // When / Then
    assertThat(assignments.assignmentOf(PROFESSIONAL, MONTH)).isEmpty();
  }
}
