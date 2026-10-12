package eu.openvalue.timesheet.adapter.out.persistence;

import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.SICK;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;

import eu.openvalue.timesheet.PostgresTestConfiguration;
import eu.openvalue.timesheet.domain.BookedLeave;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against PostgreSQL with the schema from the migrations, with timesheets kept as the application keeps them.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({PostgresTestConfiguration.class, JpaLeaveBookingsTest.Adapters.class})
class JpaLeaveBookingsTest {

  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final Professional PIET = Professional.withEmail("piet.de.vries@openvalue.nl");
  private static final Year YEAR_2026 = Year.of(2026);

  @Autowired
  private JpaTimesheets timesheets;

  @Autowired
  private JpaLeaveBookings leaveBookings;

  @Autowired
  private TimesheetEntityRepository repository;

  @BeforeEach
  void forgetAllTimesheets() {
    repository.deleteAll();
  }

  @Test
  void addsUpTheLeaveBookedInEachTimesheetOfTheYear() {
    // Given
    var march = Timesheet.start(YearMonth.of(2026, 3), JAN, Optional.empty());
    march.bookHours(LocalDate.of(2026, 3, 2), LEAVE, Hours.of(8));
    march.rebookDay(LocalDate.of(2026, 3, 3), Map.of(LEAVE, Hours.of(3, 30), WORKED, Hours.of(4, 30)));
    march.bookHours(LocalDate.of(2026, 3, 4), SICK, Hours.of(8));
    march.submit(LocalDate.of(2026, 4, 1));
    timesheets.save(march);
    var april = Timesheet.start(YearMonth.of(2026, 4), JAN, Optional.empty());
    april.bookHours(LocalDate.of(2026, 4, 1), WORKED, Hours.of(8));
    timesheets.save(april);

    // When
    var booked = leaveBookings.bookedIn(JAN, YEAR_2026);

    // Then
    assertThat(booked).containsExactly(
        new BookedLeave(YearMonth.of(2026, 3), Hours.of(11, 30), true),
        new BookedLeave(YearMonth.of(2026, 4), Hours.ZERO, false));
  }

  @Test
  void leavesOutOtherYearsAndOtherProfessionals() {
    // Given
    var lastYear = Timesheet.start(YearMonth.of(2025, 12), JAN, Optional.empty());
    lastYear.bookHours(LocalDate.of(2025, 12, 1), LEAVE, Hours.of(8));
    timesheets.save(lastYear);
    var piet = Timesheet.start(YearMonth.of(2026, 3), PIET, Optional.empty());
    piet.bookHours(LocalDate.of(2026, 3, 2), LEAVE, Hours.of(8));
    timesheets.save(piet);

    // When / Then
    assertThat(leaveBookings.bookedIn(JAN, YEAR_2026)).isEmpty();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Adapters {

    @Bean
    JpaTimesheets timesheets(TimesheetEntityRepository repository) {
      return new JpaTimesheets(repository);
    }

    @Bean
    JpaLeaveBookings leaveBookings(TimesheetEntityRepository repository) {
      return new JpaLeaveBookings(repository);
    }
  }
}
