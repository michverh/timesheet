package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.BookedLeave;
import eu.openvalue.timesheet.domain.Professional;
import java.time.Year;
import java.util.List;

/**
 * The holiday leave booked in a professional's timesheets, without the rest of the timesheets.
 */
public interface LeaveBookings {

  /**
   * @return the holiday leave booked per month of the year, for the months with a timesheet
   */
  List<BookedLeave> bookedIn(Professional professional, Year year);
}
