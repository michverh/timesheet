package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Keeps professionals' timesheets, drafts and submitted ones alike, one per professional and month.
 */
public interface Timesheets {

  Optional<Timesheet> find(Professional professional, YearMonth month);

  /**
   * Keeps the timesheet, replacing what was kept for its professional and month before.
   */
  void save(Timesheet timesheet);
}
