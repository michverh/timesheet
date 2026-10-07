package eu.openvalue.timesheet.adapter.out.mail;

import eu.openvalue.timesheet.application.port.out.TimesheetDelivery;
import eu.openvalue.timesheet.application.port.out.TimesheetDeliveryFailedException;
import eu.openvalue.timesheet.application.port.out.TimesheetSubmission;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Emails submitted timesheets to HR, with the spreadsheet attached and the professional in copy.
 */
public final class MailTimesheetDelivery implements TimesheetDelivery {

  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

  private final JavaMailSender mailSender;
  private final TimesheetMailProperties properties;

  public MailTimesheetDelivery(JavaMailSender mailSender, TimesheetMailProperties properties) {
    this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
    this.properties = Objects.requireNonNull(properties, "properties");
  }

  @Override
  public void deliver(TimesheetSubmission submission) {
    try {
      mailSender.send(messageFor(submission));
    } catch (MailException | MessagingException e) {
      throw new TimesheetDeliveryFailedException("Could not email the timesheet of %s for %s".formatted(
          submission.professional().email(), submission.month()), e);
    }
  }

  private MimeMessage messageFor(TimesheetSubmission submission) throws MessagingException {
    var message = mailSender.createMimeMessage();
    var helper = new MimeMessageHelper(message, true, "UTF-8");
    address(helper, submission);
    helper.setSubject(subjectOf(submission));
    helper.setText(bodyOf(submission));
    attachSpreadsheet(helper, submission);
    return message;
  }

  /**
   * Sends the mail to HR, with the professional in copy so replies reach them too.
   */
  private void address(MimeMessageHelper helper, TimesheetSubmission submission) throws MessagingException {
    var professional = submission.professional().email().value();
    helper.setFrom(properties.from());
    helper.setTo(properties.to().toArray(String[]::new));
    helper.setCc(professional);
    helper.setReplyTo(professional);
  }

  private static void attachSpreadsheet(MimeMessageHelper helper, TimesheetSubmission submission)
      throws MessagingException {
    var spreadsheet = submission.spreadsheet();
    helper.addAttachment(submission.fileName(), new ByteArrayResource(spreadsheet.content()),
        spreadsheet.mediaType());
  }

  private static String subjectOf(TimesheetSubmission submission) {
    return submission.isCorrection() ? "Timesheet (correction)" : "Timesheet";
  }

  private static String bodyOf(TimesheetSubmission submission) {
    return """
        Dear HR,

        %s The attached file, %s, lists the timesheet for %s.

        Kind regards,

        %s
        %s
        """.formatted(introductionOf(submission), submission.fileName(), month(submission.month()),
        submission.professional().name(), submission.professional().email());
  }

  private static String introductionOf(TimesheetSubmission submission) {
    var name = submission.professional().name();
    var month = month(submission.month());
    var submittedOn = date(submission.submittedOn());
    return submission.replacesSubmissionOf()
        .map(earlier -> """
            Please find attached the corrected timesheet of %s for %s, submitted on %s. It replaces the timesheet \
            submitted on %s, which can be discarded.""".formatted(name, month, submittedOn, date(earlier)))
        .orElseGet(() -> "Please find attached the timesheet of %s for %s, submitted on %s.".formatted(name, month,
            submittedOn));
  }

  private static String month(YearMonth month) {
    return MONTH.format(month);
  }

  private static String date(LocalDate date) {
    return DATE.format(date);
  }
}
