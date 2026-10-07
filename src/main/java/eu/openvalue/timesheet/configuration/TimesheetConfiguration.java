package eu.openvalue.timesheet.configuration;

import eu.openvalue.timesheet.adapter.out.assignment.AssignmentProperties;
import eu.openvalue.timesheet.adapter.out.assignment.ConfiguredAssignments;
import eu.openvalue.timesheet.application.PrepareTimesheetDraft;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistry;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AssignmentProperties.class)
class TimesheetConfiguration {

  @Bean
  TimesheetPrefill timesheetPrefill() {
    return new TimesheetPrefill(new DutchPublicHolidays());
  }

  @Bean
  Assignments assignments(AssignmentProperties properties) {
    return new ConfiguredAssignments(properties);
  }

  /**
   * Registers no absences at all, until the HTTP adapter for the absence registry is available.
   */
  @Bean
  AbsenceRegistry absenceRegistry() {
    return (professional, month) -> List.of();
  }

  @Bean
  PrepareTimesheetDraftUseCase prepareTimesheetDraftUseCase(TimesheetPrefill prefill,
      AbsenceRegistry absenceRegistry, Assignments assignments) {
    return new PrepareTimesheetDraft(prefill, absenceRegistry, assignments);
  }
}
