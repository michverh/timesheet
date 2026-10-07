package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Knows which client, if any, a professional works for.
 */
@FunctionalInterface
public interface Assignments {

  /**
   * @return the assignment, or empty when the professional is on the bench during the month
   */
  Optional<Assignment> assignmentOf(Professional professional, YearMonth month);
}
