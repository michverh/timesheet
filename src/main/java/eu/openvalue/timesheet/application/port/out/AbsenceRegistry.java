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

  List<Absence> absencesOf(Professional professional, YearMonth month);
}
