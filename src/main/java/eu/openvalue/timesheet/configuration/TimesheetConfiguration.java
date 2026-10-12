package eu.openvalue.timesheet.configuration;

import eu.openvalue.timesheet.adapter.in.web.CurrentProfessional;
import eu.openvalue.timesheet.adapter.in.web.CurrentProfessionalProperties;
import eu.openvalue.timesheet.adapter.out.assignment.AssignmentProperties;
import eu.openvalue.timesheet.adapter.out.assignment.ConfiguredAssignments;
import eu.openvalue.timesheet.adapter.out.employment.ConfiguredEmployments;
import eu.openvalue.timesheet.adapter.out.employment.EmploymentProperties;
import eu.openvalue.timesheet.adapter.out.mail.MailTimesheetDelivery;
import eu.openvalue.timesheet.adapter.out.mail.TimesheetMailProperties;
import eu.openvalue.timesheet.adapter.out.persistence.JpaLeaveBookings;
import eu.openvalue.timesheet.adapter.out.persistence.JpaTimesheets;
import eu.openvalue.timesheet.adapter.out.persistence.TimesheetEntityRepository;
import eu.openvalue.timesheet.adapter.out.reading.ClaudeProperties;
import eu.openvalue.timesheet.adapter.out.reading.ClaudeTranscriber;
import eu.openvalue.timesheet.adapter.out.reading.OllamaProperties;
import eu.openvalue.timesheet.adapter.out.reading.OllamaTranscriber;
import eu.openvalue.timesheet.adapter.out.reading.Transcriber;
import eu.openvalue.timesheet.adapter.out.reading.TranscribingClientTimesheetReader;
import eu.openvalue.timesheet.adapter.out.spreadsheet.ExcelTimesheetSpreadsheet;
import eu.openvalue.timesheet.adapter.out.spreadsheet.SpreadsheetProperties;
import eu.openvalue.timesheet.application.AdjustingTimesheetDay;
import eu.openvalue.timesheet.application.CorrectingTimesheet;
import eu.openvalue.timesheet.application.CurrentTimesheets;
import eu.openvalue.timesheet.application.DownloadingTimesheet;
import eu.openvalue.timesheet.application.ImportingClientTimesheets;
import eu.openvalue.timesheet.application.PreparingTimesheetDraft;
import eu.openvalue.timesheet.application.SubmittingTimesheet;
import eu.openvalue.timesheet.application.ViewingHolidayLeave;
import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayUseCase;
import eu.openvalue.timesheet.application.port.in.CorrectTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.DownloadTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.ImportClientTimesheetsUseCase;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.SubmitTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.ViewHolidayLeaveUseCase;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetReader;
import eu.openvalue.timesheet.application.port.out.Employments;
import eu.openvalue.timesheet.application.port.out.LeaveBookings;
import eu.openvalue.timesheet.application.port.out.TimesheetDelivery;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
    AssignmentProperties.class, EmploymentProperties.class, CurrentProfessionalProperties.class, SpreadsheetProperties.class,
    TimesheetMailProperties.class, OllamaProperties.class,
    ClaudeProperties.class})
class TimesheetConfiguration {

  private static final String TRANSCRIBER = "timesheet.client-timesheet-reader.transcriber";

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
  Employments employments(EmploymentProperties properties) {
    return new ConfiguredEmployments(properties);
  }

  @Bean
  Timesheets timesheets(TimesheetEntityRepository repository) {
    return new JpaTimesheets(repository);
  }

  @Bean
  LeaveBookings leaveBookings(TimesheetEntityRepository repository) {
    return new JpaLeaveBookings(repository);
  }

  @Bean
  ViewHolidayLeaveUseCase viewHolidayLeaveUseCase(Employments employments, LeaveBookings leaveBookings) {
    return new ViewingHolidayLeave(employments, leaveBookings);
  }

  /**
   * Serves both as {@link PrepareTimesheetDraftUseCase} and as {@link ResetTimesheetDraftUseCase}.
   */
  @Bean
  CurrentTimesheets currentTimesheets(TimesheetPrefill prefill, Assignments assignments, Timesheets timesheets) {
    return new CurrentTimesheets(prefill, assignments, timesheets);
  }

  @Bean
  PreparingTimesheetDraft prepareTimesheetDraft(CurrentTimesheets currentTimesheets, Timesheets timesheets) {
    return new PreparingTimesheetDraft(currentTimesheets, timesheets);
  }

  @Bean
  AdjustTimesheetDayUseCase adjustTimesheetDayUseCase(CurrentTimesheets currentTimesheets,
      Timesheets timesheets) {
    return new AdjustingTimesheetDay(currentTimesheets, timesheets);
  }

  @Bean
  TimesheetSpreadsheet timesheetSpreadsheet(SpreadsheetProperties properties) {
    return new ExcelTimesheetSpreadsheet(properties.template());
  }

  @Bean
  DownloadTimesheetUseCase downloadTimesheetUseCase(CurrentTimesheets currentTimesheets,
      TimesheetSpreadsheet spreadsheet) {
    return new DownloadingTimesheet(currentTimesheets, spreadsheet);
  }

  @Bean
  TimesheetDelivery timesheetDelivery(JavaMailSender mailSender, TimesheetMailProperties properties) {
    return new MailTimesheetDelivery(mailSender, properties);
  }

  @Bean
  SubmitTimesheetUseCase submitTimesheetUseCase(CurrentTimesheets currentTimesheets, Timesheets timesheets,
      TimesheetSpreadsheet spreadsheet, TimesheetDelivery delivery, Clock clock) {
    return new SubmittingTimesheet(currentTimesheets, timesheets, spreadsheet, delivery, clock);
  }

  @Bean
  CorrectTimesheetUseCase correctTimesheetUseCase(Timesheets timesheets) {
    return new CorrectingTimesheet(timesheets);
  }

  /**
   * Reads client timesheets with a local model in Ollama, unless configured to use Claude.
   */
  @Bean
  @ConditionalOnProperty(name = TRANSCRIBER, havingValue = "ollama", matchIfMissing = true)
  Transcriber ollamaTranscriber(OllamaProperties properties) {
    return OllamaTranscriber.connectingTo(properties.baseUrl(), properties.model(), properties.timeout());
  }

  @Bean
  @ConditionalOnProperty(name = TRANSCRIBER, havingValue = "claude")
  Transcriber claudeTranscriber(ClaudeProperties properties) {
    return ClaudeTranscriber.connectingTo(properties.baseUrl(), Optional.ofNullable(properties.apiKey()),
        properties.model(), properties.effortToSet(), properties.timeout());
  }

  @Bean
  ClientTimesheetReader clientTimesheetReader(Transcriber transcriber) {
    return new TranscribingClientTimesheetReader(transcriber);
  }

  @Bean
  ImportClientTimesheetsUseCase importClientTimesheetsUseCase(CurrentTimesheets currentTimesheets,
      Timesheets timesheets, ClientTimesheetReader reader) {
    return new ImportingClientTimesheets(currentTimesheets, timesheets, reader);
  }
}
