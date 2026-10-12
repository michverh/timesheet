package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.application.port.in.ViewHolidayLeaveUseCase;
import eu.openvalue.timesheet.domain.HolidayLeave;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Project;
import eu.openvalue.timesheet.domain.Timesheet;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * What the web slice tests share: Jan Jansen using the application in May 2026.
 */
@TestConfiguration(proxyBeanMethods = false)
class WebTestConfiguration {

  static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  static final YearMonth MAY_2026 = YearMonth.of(2026, 5);

  /**
   * Jan's draft for May 2026, assigned to ACME, prefilled and with 8 hours worked on every other weekday.
   */
  static Timesheet draft() {
    var draft = new TimesheetPrefill(new DutchPublicHolidays())
        .prefill(MAY_2026, JAN, Optional.of(Assignment.at(new Client("ACME"), new Project("Platform"))));
    draft.days().stream()
        .filter(day -> !day.isWeekend() && day.bookedHours().isEmpty())
        .forEach(day -> draft.bookHours(day.date(), HourCategory.WORKED, Hours.of(8)));
    return draft;
  }

  @Bean
  Clock clock() {
    return Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneId.of("Europe/Amsterdam"));
  }

  /**
   * Jan's holiday leave in any year: 208 hours, of which 40 taken and 16 reserved.
   */
  @Bean
  ViewHolidayLeaveUseCase viewHolidayLeave() {
    return query -> new HolidayLeave(query.year(), Hours.of(208), Hours.of(40), Hours.of(16));
  }

  @Bean
  CurrentProfessional currentProfessional() {
    return new CurrentProfessional(JAN);
  }
}
