package eu.openvalue.timesheet.adapter.out.persistence;

import eu.openvalue.timesheet.application.port.out.LeaveBookings;
import eu.openvalue.timesheet.domain.BookedLeave;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * Adds up the holiday leave in the timesheets kept in the database, without loading the timesheets themselves.
 */
public class JpaLeaveBookings implements LeaveBookings {

  private final TimesheetEntityRepository repository;

  public JpaLeaveBookings(TimesheetEntityRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  @Override
  public List<BookedLeave> bookedIn(Professional professional, Year year) {
    return repository.leaveBookedBetween(professional.email().value(), year.atDay(1), year.plusYears(1).atDay(1))
        .stream()
        .map(month -> new BookedLeave(YearMonth.from(month.getMonth()), Hours.ofMinutes((int) month.getMinutes()),
            month.getSubmitted()))
        .toList();
  }
}
