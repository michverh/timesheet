package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Project;
import eu.openvalue.timesheet.domain.Timesheet;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
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
   * Jan's prefilled draft for May 2026, assigned to ACME.
   */
  static Timesheet draft() {
    return new TimesheetPrefill(new DutchPublicHolidays())
        .prefill(MAY_2026, JAN, Optional.of(Assignment.at(new Client("ACME"), new Project("Platform"))), List.of());
  }

  @Bean
  Clock clock() {
    return Clock.fixed(Instant.parse("2026-05-15T10:00:00Z"), ZoneId.of("Europe/Amsterdam"));
  }

  @Bean
  CurrentProfessional currentProfessional() {
    return new CurrentProfessional(JAN);
  }
}
