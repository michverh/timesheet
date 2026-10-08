package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.Absence;
import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;
import java.util.List;

/**
 * The source of truth for the absences HR has registered.
 */
@FunctionalInterface
public interface AbsenceRegistry {

  /**
   * @return the absences registered for the professional during the month; none when HR does not know the
   *     professional
   * @throws AbsenceRegistryUnavailableException when the registered absences cannot be retrieved
   */
  List<Absence> absencesOf(Professional professional, YearMonth month);
}
