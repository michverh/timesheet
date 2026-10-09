package eu.openvalue.timesheet.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import eu.openvalue.timesheet.adapter.out.persistence.TimesheetEntityRepository;
import eu.openvalue.timesheet.adapter.out.reading.ClaudeTranscriber;
import eu.openvalue.timesheet.adapter.out.reading.OllamaTranscriber;
import eu.openvalue.timesheet.adapter.out.reading.Transcriber;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class TimesheetConfigurationTest {

  private final ApplicationContextRunner context = new ApplicationContextRunner()
      .withUserConfiguration(TimesheetConfiguration.class)
      .withBean(JavaMailSender.class, JavaMailSenderImpl::new)
      .withBean(TimesheetEntityRepository.class, () -> mock(TimesheetEntityRepository.class))
      .withPropertyValues(
          "timesheet.professional.email=jan.jansen@openvalue.nl",
          "timesheet.absence-registry.base-url=http://localhost:8090",
          "timesheet.client-timesheet-reader.ollama.base-url=http://localhost:11434",
          "timesheet.client-timesheet-reader.ollama.model=qwen3-vl:8b-instruct",
          "timesheet.mail.from=timesheets@openvalue.example",
          "timesheet.mail.to=hr@openvalue.example");

  @Test
  void readsClientTimesheetsWithOllamaUnlessConfiguredOtherwise() {
    context.run(started -> assertThat(started.getBean(Transcriber.class)).isInstanceOf(OllamaTranscriber.class));
  }

  @Test
  void readsClientTimesheetsWithClaudeWhenConfigured() {
    context
        .withPropertyValues(
            "timesheet.client-timesheet-reader.transcriber=claude",
            "timesheet.client-timesheet-reader.claude.api-key=test-key")
        .run(started -> assertThat(started.getBean(Transcriber.class)).isInstanceOf(ClaudeTranscriber.class));
  }
}
