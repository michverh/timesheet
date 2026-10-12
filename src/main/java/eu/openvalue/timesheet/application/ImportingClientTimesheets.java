package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.ImportClientTimesheetsCommand;
import eu.openvalue.timesheet.application.port.in.ImportClientTimesheetsUseCase;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetReader;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.domain.ClientTimesheet;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.ClientTimesheets;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Reads the hours worked from the client's timesheets and hands them in, so the hours worked they agree on are booked.
 * A client's timesheet that cannot be read is still imported, so its hours can be filled in by hand.
 */
public final class ImportingClientTimesheets implements ImportClientTimesheetsUseCase {

  private static final Logger LOGGER = System.getLogger(ImportingClientTimesheets.class.getName());

  private final CurrentTimesheets currentTimesheets;
  private final Timesheets timesheets;
  private final ClientTimesheetReader reader;

  public ImportingClientTimesheets(CurrentTimesheets currentTimesheets, Timesheets timesheets,
      ClientTimesheetReader reader) {
    this.currentTimesheets = Objects.requireNonNull(currentTimesheets, "currentTimesheets");
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
    this.reader = Objects.requireNonNull(reader, "reader");
  }

  /**
   * Reads each client's timesheet only once, also when another change to the timesheet gets in between, since reading
   * can be slow and costly.
   */
  @Override
  public void importClientTimesheets(ImportClientTimesheetsCommand command) {
    var professional = command.professional();
    var month = command.month();
    // Refuses before reading when the client's timesheets cannot be imported, such as when it was submitted.
    currentTimesheets.of(professional, month).ensureChangeable();
    var imported = ClientTimesheets.of(command.documents().stream().map(this::read).toList());
    ConcurrentChanges.retried(() -> {
      var draft = currentTimesheets.of(professional, month);
      draft.importClientTimesheets(imported);
      timesheets.save(draft);
    });
  }

  private ClientTimesheet read(ClientTimesheetDocument document) {
    var uploaded = ClientTimesheet.of(document);
    try {
      return uploaded.readAs(reader.read(document));
    } catch (ClientTimesheetUnreadableException e) {
      LOGGER.log(Level.WARNING, "Could not read the hours from client timesheet %s".formatted(document), e);
      return uploaded.unreadable();
    }
  }
}
