package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.ViewHolidayLeaveQuery;
import eu.openvalue.timesheet.application.port.in.ViewHolidayLeaveUseCase;
import eu.openvalue.timesheet.application.port.out.Employments;
import eu.openvalue.timesheet.application.port.out.LeaveBookings;
import eu.openvalue.timesheet.domain.HolidayLeave;
import java.util.Objects;

public final class ViewingHolidayLeave implements ViewHolidayLeaveUseCase {

  private final Employments employments;
  private final LeaveBookings leaveBookings;

  public ViewingHolidayLeave(Employments employments, LeaveBookings leaveBookings) {
    this.employments = Objects.requireNonNull(employments, "employments");
    this.leaveBookings = Objects.requireNonNull(leaveBookings, "leaveBookings");
  }

  @Override
  public HolidayLeave holidayLeave(ViewHolidayLeaveQuery query) {
    var professional = query.professional();
    return HolidayLeave.of(query.year(), employments.employedSince(professional),
        leaveBookings.bookedIn(professional, query.year()));
  }
}
