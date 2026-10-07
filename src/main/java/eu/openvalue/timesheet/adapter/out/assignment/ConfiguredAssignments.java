package eu.openvalue.timesheet.adapter.out.assignment;

import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Project;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Gives every professional the assignment from the application configuration, regardless of the month.
 */
public final class ConfiguredAssignments implements Assignments {

  private final Optional<Assignment> assignment;

  public ConfiguredAssignments(AssignmentProperties properties) {
    this.assignment = nonBlank(properties.client())
        .map(client -> new Assignment(new Client(client), nonBlank(properties.project()).map(Project::new)));
  }

  @Override
  public Optional<Assignment> assignmentOf(Professional professional, YearMonth month) {
    return assignment;
  }

  private static Optional<String> nonBlank(String value) {
    return Optional.ofNullable(value).filter(text -> !text.isBlank());
  }
}
