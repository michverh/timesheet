package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetReader;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

/**
 * Hands in the client's timesheet and reads the hours worked from it, so they can be compared. A timesheet that cannot
 * be read is still handed in, to be compared by hand.
 */
public final class AttachingClientTimesheet implements AttachClientTimesheetUseCase {

  private static final Logger LOGGER = System.getLogger(AttachingClientTimesheet.class.getName());

  private final Timesheets timesheets;
  private final ClientTimesheetReader reader;

  public AttachingClientTimesheet(Timesheets timesheets, ClientTimesheetReader reader) {
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
    this.reader = Objects.requireNonNull(reader, "reader");
  }

  /**
   * Reads the client's timesheet only once, also when another change to the timesheet gets in between, since reading
   * can be slow and costly.
   */
  @Override
  public void attach(AttachClientTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var document = command.document();
    // Refuses before reading when the timesheet cannot take the client's timesheet, such as when it was submitted.
    draftOf(professional, month).attachClientTimesheet(document);
    var reading = read(document);
    ConcurrentChanges.retried(() -> {
      var draft = draftOf(professional, month);
      draft.attachClientTimesheet(document);
      reading.ifPresentOrElse(draft::recordHoursReportedByClient, draft::markClientTimesheetUnreadable);
      timesheets.save(draft);
    });
  }

  private Timesheet draftOf(Professional professional, YearMonth month) {
    return timesheets.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
  }

  /**
   * @return the hours worked according to the client's timesheet, or empty when they could not be read
   */
  private Optional<ClientReportedHours> read(ClientTimesheetDocument document) {
    try {
      return Optional.of(reader.read(document));
    } catch (ClientTimesheetUnreadableException e) {
      LOGGER.log(Level.WARNING, "Could not read the hours from client timesheet %s".formatted(document), e);
      return Optional.empty();
    }
  }
}
