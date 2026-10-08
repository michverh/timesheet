package eu.openvalue.timesheet.configuration;

import eu.openvalue.timesheet.adapter.in.web.CurrentProfessional;
import eu.openvalue.timesheet.adapter.in.web.CurrentProfessionalProperties;
import eu.openvalue.timesheet.adapter.out.absence.AbsenceRegistryProperties;
import eu.openvalue.timesheet.adapter.out.absence.HttpAbsenceRegistry;
import eu.openvalue.timesheet.adapter.out.assignment.AssignmentProperties;
import eu.openvalue.timesheet.adapter.out.assignment.ConfiguredAssignments;
import eu.openvalue.timesheet.adapter.out.draft.InMemoryTimesheetDrafts;
import eu.openvalue.timesheet.adapter.out.mail.MailTimesheetDelivery;
import eu.openvalue.timesheet.adapter.out.mail.TimesheetMailProperties;
import eu.openvalue.timesheet.adapter.out.spreadsheet.ExcelTimesheetSpreadsheet;
import eu.openvalue.timesheet.adapter.out.spreadsheet.SpreadsheetProperties;
import eu.openvalue.timesheet.application.AdjustingTimesheetDay;
import eu.openvalue.timesheet.application.AttachingClientTimesheet;
import eu.openvalue.timesheet.application.CorrectingTimesheet;
import eu.openvalue.timesheet.application.DownloadingTimesheet;
import eu.openvalue.timesheet.application.PreparingTimesheetDraft;
import eu.openvalue.timesheet.application.SubmittingTimesheet;
import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayUseCase;
import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.CorrectTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.DownloadTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.SubmitTimesheetUseCase;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistry;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.application.port.out.TimesheetDelivery;
import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
    AssignmentProperties.class, CurrentProfessionalProperties.class, SpreadsheetProperties.class,
    TimesheetMailProperties.class, AbsenceRegistryProperties.class})
class TimesheetConfiguration {

  @Bean
  Clock clock() {
    return Clock.system(ZoneId.of("Europe/Amsterdam"));
  }

  @Bean
  CurrentProfessional currentProfessional(CurrentProfessionalProperties properties) {
    return CurrentProfessional.from(properties);
  }

  @Bean
  TimesheetPrefill timesheetPrefill() {
    return new TimesheetPrefill(new DutchPublicHolidays());
  }

  @Bean
  Assignments assignments(AssignmentProperties properties) {
    return new ConfiguredAssignments(properties);
  }

  @Bean
  AbsenceRegistry absenceRegistry(AbsenceRegistryProperties properties) {
    return HttpAbsenceRegistry.connectingTo(properties.baseUrl(), properties.timeout());
  }

  @Bean
  TimesheetDrafts timesheetDrafts() {
    return new InMemoryTimesheetDrafts();
  }

  /**
   * Serves both as {@link PrepareTimesheetDraftUseCase} and as {@link ResetTimesheetDraftUseCase}.
   */
  @Bean
  PreparingTimesheetDraft prepareTimesheetDraft(TimesheetPrefill prefill,
      AbsenceRegistry absenceRegistry, Assignments assignments, TimesheetDrafts drafts) {
    return new PreparingTimesheetDraft(prefill, absenceRegistry, assignments, drafts);
  }

  @Bean
  AdjustTimesheetDayUseCase adjustTimesheetDayUseCase(TimesheetDrafts drafts) {
    return new AdjustingTimesheetDay(drafts);
  }

  @Bean
  TimesheetSpreadsheet timesheetSpreadsheet(SpreadsheetProperties properties) {
    return new ExcelTimesheetSpreadsheet(properties.template());
  }

  @Bean
  DownloadTimesheetUseCase downloadTimesheetUseCase(TimesheetDrafts drafts, TimesheetSpreadsheet spreadsheet) {
    return new DownloadingTimesheet(drafts, spreadsheet);
  }

  @Bean
  TimesheetDelivery timesheetDelivery(JavaMailSender mailSender, TimesheetMailProperties properties) {
    return new MailTimesheetDelivery(mailSender, properties);
  }

  @Bean
  SubmitTimesheetUseCase submitTimesheetUseCase(TimesheetDrafts drafts, TimesheetSpreadsheet spreadsheet,
      TimesheetDelivery delivery, Clock clock) {
    return new SubmittingTimesheet(drafts, spreadsheet, delivery, clock);
  }

  @Bean
  CorrectTimesheetUseCase correctTimesheetUseCase(TimesheetDrafts drafts) {
    return new CorrectingTimesheet(drafts);
  }

  @Bean
  AttachClientTimesheetUseCase attachClientTimesheetUseCase(TimesheetDrafts drafts) {
    return new AttachingClientTimesheet(drafts);
  }
}
