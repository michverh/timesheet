package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Timesheet;

public interface AdjustTimesheetDayUseCase {

  /**
   * Replaces what is booked on a day of a draft timesheet.
   *
   * @return the adjusted draft
   * @throws TimesheetDraftNotFoundException when there is no draft for the month of the day
   */
  Timesheet adjustDay(AdjustTimesheetDayCommand command);
}
