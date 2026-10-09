package eu.openvalue.timesheet.configuration;

import eu.openvalue.timesheet.adapter.in.web.CurrentProfessional;
import eu.openvalue.timesheet.adapter.in.web.CurrentProfessionalProperties;
import eu.openvalue.timesheet.adapter.out.absence.AbsenceRegistryProperties;
import eu.openvalue.timesheet.adapter.out.absence.HttpAbsenceRegistry;
import eu.openvalue.timesheet.adapter.out.assignment.AssignmentProperties;
import eu.openvalue.timesheet.adapter.out.assignment.ConfiguredAssignments;
import eu.openvalue.timesheet.adapter.out.mail.MailTimesheetDelivery;
import eu.openvalue.timesheet.adapter.out.mail.TimesheetMailProperties;
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
import eu.openvalue.timesheet.application.AttachingClientTimesheet;
import eu.openvalue.timesheet.application.CorrectingTimesheet;
import eu.openvalue.timesheet.application.DownloadingTimesheet;
import eu.openvalue.timesheet.application.PreparingTimesheetDraft;
import eu.openvalue.timesheet.application.SubmittingTimesheet;
import eu.openvalue.timesheet.application.TakingOverClientHours;
import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayUseCase;
import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.CorrectTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.DownloadTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.SubmitTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TakeOverClientHoursUseCase;
import eu.openvalue.timesheet.application.port.out.AbsenceRegistry;
import eu.openvalue.timesheet.application.port.out.Assignments;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetReader;
import eu.openvalue.timesheet.application.port.out.TimesheetDelivery;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
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
    AssignmentProperties.class, CurrentProfessionalProperties.class, SpreadsheetProperties.class,
    TimesheetMailProperties.class, AbsenceRegistryProperties.class, OllamaProperties.class,
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
  AbsenceRegistry absenceRegistry(AbsenceRegistryProperties properties) {
    return HttpAbsenceRegistry.connectingTo(properties.baseUrl(), properties.timeout());
  }

  @Bean
  Timesheets timesheets(TimesheetEntityRepository repository) {
    return new JpaTimesheets(repository);
  }

  /**
   * Serves both as {@link PrepareTimesheetDraftUseCase} and as {@link ResetTimesheetDraftUseCase}.
   */
  @Bean
  PreparingTimesheetDraft prepareTimesheetDraft(TimesheetPrefill prefill,
      AbsenceRegistry absenceRegistry, Assignments assignments, Timesheets timesheets) {
    return new PreparingTimesheetDraft(prefill, absenceRegistry, assignments, timesheets);
  }

  @Bean
  AdjustTimesheetDayUseCase adjustTimesheetDayUseCase(Timesheets timesheets) {
    return new AdjustingTimesheetDay(timesheets);
  }

  @Bean
  TimesheetSpreadsheet timesheetSpreadsheet(SpreadsheetProperties properties) {
    return new ExcelTimesheetSpreadsheet(properties.template());
  }

  @Bean
  DownloadTimesheetUseCase downloadTimesheetUseCase(Timesheets timesheets, TimesheetSpreadsheet spreadsheet) {
    return new DownloadingTimesheet(timesheets, spreadsheet);
  }

  @Bean
  TimesheetDelivery timesheetDelivery(JavaMailSender mailSender, TimesheetMailProperties properties) {
    return new MailTimesheetDelivery(mailSender, properties);
  }

  @Bean
  SubmitTimesheetUseCase submitTimesheetUseCase(Timesheets timesheets, TimesheetSpreadsheet spreadsheet,
      TimesheetDelivery delivery, Clock clock) {
    return new SubmittingTimesheet(timesheets, spreadsheet, delivery, clock);
  }

  @Bean
  CorrectTimesheetUseCase correctTimesheetUseCase(Timesheets timesheets) {
    return new CorrectingTimesheet(timesheets);
  }

  @Bean
  TakeOverClientHoursUseCase takeOverClientHoursUseCase(Timesheets timesheets) {
    return new TakingOverClientHours(timesheets);
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
  AttachClientTimesheetUseCase attachClientTimesheetUseCase(Timesheets timesheets, ClientTimesheetReader reader) {
    return new AttachingClientTimesheet(timesheets, reader);
  }
}
