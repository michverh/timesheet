package eu.openvalue.timesheet.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import eu.openvalue.timesheet.application.port.out.SpreadsheetFile;
import eu.openvalue.timesheet.application.port.out.TimesheetDeliveryFailedException;
import eu.openvalue.timesheet.application.port.out.TimesheetSubmission;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Professional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MailTimesheetDeliveryTest {

  private static final int SMTP_PORT = 1025;
  private static final int API_PORT = 8025;
  private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {
  };

  @Container
  private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:v1.31")
      .withExposedPorts(SMTP_PORT, API_PORT)
      .waitingFor(Wait.forHttp("/livez").forPort(API_PORT));

  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final byte[] CONTENT = {1, 2, 3, 4};

  private RestClient mailpit;
  private MailTimesheetDelivery delivery;

  @BeforeEach
  void connectToMailpit() {
    mailpit = RestClient.create("http://%s:%d/api/v1".formatted(MAILPIT.getHost(), MAILPIT.getMappedPort(API_PORT)));
    mailpit.delete().uri("/messages").retrieve().toBodilessEntity();

    var mailSender = new JavaMailSenderImpl();
    mailSender.setHost(MAILPIT.getHost());
    mailSender.setPort(MAILPIT.getMappedPort(SMTP_PORT));
    delivery = new MailTimesheetDelivery(mailSender, new TimesheetMailProperties("timesheets@openvalue.example",
        List.of("hr@openvalue.example", "finance@openvalue.example")));
  }

  @Test
  void emailsTheTimesheetToHrWithTheProfessionalInCopy() {
    // When
    delivery.deliver(submission(Optional.empty()));

    // Then
    var message = onlyMessage();
    assertThat(addresses(message.get("From"))).containsExactly("timesheets@openvalue.example");
    assertThat(addresses(message.get("To"))).containsExactly("hr@openvalue.example", "finance@openvalue.example");
    assertThat(addresses(message.get("Cc"))).containsExactly("jan.jansen@openvalue.nl");
    assertThat(addresses(message.get("ReplyTo"))).containsExactly("jan.jansen@openvalue.nl");
    assertThat(message.get("Subject")).isEqualTo("Timesheet");
  }

  @Test
  void explainsWhatIsAttached() {
    // When
    delivery.deliver(submission(Optional.empty()));

    // Then
    assertThat((String) onlyMessage().get("Text"))
        .startsWith("Dear HR,")
        .contains("Please find attached the timesheet of Jan Jansen for May 2026, submitted on 2 June 2026.")
        .contains("The attached file, Urenverantwoording_2026-05_Jan_Jansen.xlsx, lists the timesheet for May 2026.")
        .contains("Kind regards,")
        .doesNotContain("corrected", "client's own timesheet");
  }

  @Test
  void attachesTheClientsTimesheetWhenHandedIn() {
    // Given
    var clientTimesheet = new ClientTimesheetDocument("acme-may.pdf", DocumentFormat.PDF, new byte[] {9, 8, 7});

    // When
    delivery.deliver(submission(Optional.empty(), Optional.of(clientTimesheet)));

    // Then
    var message = onlyMessage();
    assertThat(attachments(message))
        .extracting(attachment -> attachment.get("FileName"), attachment -> attachment.get("ContentType"))
        .containsExactly(
            tuple("Urenverantwoording_2026-05_Jan_Jansen.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            tuple("acme-may.pdf", "application/pdf"));
    assertThat((String) message.get("Text")).contains("The client's own timesheet is attached as acme-may.pdf.");
  }

  @Test
  void attachesTheSpreadsheet() {
    // When
    delivery.deliver(submission(Optional.empty()));

    // Then
    var message = onlyMessage();
    var attachment = attachments(message).getFirst();
    assertThat(attachment.get("FileName")).isEqualTo("Urenverantwoording_2026-05_Jan_Jansen.xlsx");
    assertThat(attachment.get("ContentType"))
        .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    var content = mailpit.get().uri("/message/{id}/part/{part}", message.get("ID"), attachment.get("PartID"))
        .retrieve().body(byte[].class);
    assertThat(content).isEqualTo(CONTENT);
  }

  @Test
  void marksACorrectionAsReplacingTheEarlierSubmission() {
    // When
    delivery.deliver(submission(Optional.of(LocalDate.of(2026, 5, 29))));

    // Then
    var message = onlyMessage();
    assertThat(message.get("Subject")).isEqualTo("Timesheet (correction)");
    assertThat((String) message.get("Text")).contains("Please find attached the corrected timesheet of Jan Jansen "
        + "for May 2026, submitted on 2 June 2026. It replaces the timesheet submitted on 29 May 2026");
  }

  @Test
  void reportsAFailedDelivery() {
    // Given
    var unreachable = new JavaMailSenderImpl();
    unreachable.setHost("localhost");
    unreachable.setPort(1);
    var failingDelivery = new MailTimesheetDelivery(unreachable,
        new TimesheetMailProperties("timesheets@openvalue.example", List.of("hr@openvalue.example")));

    // When / Then
    assertThatThrownBy(() -> failingDelivery.deliver(submission(Optional.empty())))
        .isInstanceOf(TimesheetDeliveryFailedException.class);
  }

  private static TimesheetSubmission submission(Optional<LocalDate> replacesSubmissionOf) {
    return submission(replacesSubmissionOf, Optional.empty());
  }

  private static TimesheetSubmission submission(Optional<LocalDate> replacesSubmissionOf,
      Optional<ClientTimesheetDocument> clientTimesheet) {
    return new TimesheetSubmission(JAN, YearMonth.of(2026, 5), LocalDate.of(2026, 6, 2), replacesSubmissionOf,
        "Urenverantwoording_2026-05_Jan_Jansen.xlsx", new SpreadsheetFile("xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", CONTENT), clientTimesheet);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> onlyMessage() {
    var messages = (List<Map<String, Object>>) mailpit.get().uri("/messages").retrieve().body(JSON).get("messages");
    assertThat(messages).hasSize(1);
    return mailpit.get().uri("/message/{id}", messages.getFirst().get("ID")).retrieve().body(JSON);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> attachments(Map<String, Object> message) {
    return (List<Map<String, Object>>) message.get("Attachments");
  }

  @SuppressWarnings("unchecked")
  private static List<String> addresses(Object field) {
    var entries = field instanceof Map<?, ?> single ? List.of(single) : (List<Map<String, Object>>) field;
    return entries.stream().map(entry -> (String) entry.get("Address")).toList();
  }
}
