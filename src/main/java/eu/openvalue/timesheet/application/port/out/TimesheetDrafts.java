package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Keeps the timesheets a professional is still working on.
 */
public interface TimesheetDrafts {

  Optional<Timesheet> find(Professional professional, YearMonth month);

  void save(Timesheet draft);
}
