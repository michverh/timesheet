package eu.openvalue.timesheet.application;

import static org.assertj.core.api.Assertions.assertThat;

import eu.openvalue.timesheet.application.port.in.ViewHolidayLeaveQuery;
import eu.openvalue.timesheet.application.port.out.Employments;
import eu.openvalue.timesheet.application.port.out.LeaveBookings;
import eu.openvalue.timesheet.domain.BookedLeave;
import eu.openvalue.timesheet.domain.HolidayLeave;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViewingHolidayLeaveTest {

  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final Year YEAR_2026 = Year.of(2026);

  @Test
  void givesTheHolidayLeaveOfTheProfessionalFromTheirEmploymentAndBookedLeave() {
    // Given
    Employments employments = professional -> LocalDate.of(2026, 7, 1);
    LeaveBookings leaveBookings = (professional, year) -> List.of(
        new BookedLeave(YearMonth.of(2026, 8), Hours.of(16), true),
        new BookedLeave(YearMonth.of(2026, 9), Hours.of(8), false));

    // When
    var leave = new ViewingHolidayLeave(employments, leaveBookings)
        .holidayLeave(new ViewHolidayLeaveQuery(JAN, YEAR_2026));

    // Then
    assertThat(leave).isEqualTo(new HolidayLeave(YEAR_2026, Hours.of(105), Hours.of(16), Hours.of(8)));
  }
}
