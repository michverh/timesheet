package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.HolidayLeave;

public interface ViewHolidayLeaveUseCase {

  /**
   * Gives the holiday leave of a professional in a year: the entitlement, the leave taken in submitted timesheets, the
   * leave reserved in timesheets not submitted yet, and what is left.
   */
  HolidayLeave holidayLeave(ViewHolidayLeaveQuery query);
}
