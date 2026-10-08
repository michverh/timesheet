package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.Professional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

/**
 * A timesheet as handed in to HR.
 *
 * @param replacesSubmissionOf when this is a correction: the date of the submission it replaces
 * @param fileName the name of the spreadsheet file
 * @param clientTimesheet the client's own timesheet, when handed in
 */
public record TimesheetSubmission(Professional professional, YearMonth month, LocalDate submittedOn,
    Optional<LocalDate> replacesSubmissionOf, String fileName, SpreadsheetFile spreadsheet,
    Optional<ClientTimesheetDocument> clientTimesheet) {

  public TimesheetSubmission {
    Objects.requireNonNull(professional, "professional");
    Objects.requireNonNull(month, "month");
    Objects.requireNonNull(submittedOn, "submittedOn");
    Objects.requireNonNull(replacesSubmissionOf, "replacesSubmissionOf");
    Objects.requireNonNull(fileName, "fileName");
    Objects.requireNonNull(spreadsheet, "spreadsheet");
    Objects.requireNonNull(clientTimesheet, "clientTimesheet");
  }

  public boolean isCorrection() {
    return replacesSubmissionOf.isPresent();
  }
}
