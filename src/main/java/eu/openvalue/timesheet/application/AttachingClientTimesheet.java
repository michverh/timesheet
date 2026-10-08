package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetReader;
import eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException;
import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import eu.openvalue.timesheet.domain.Timesheet;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Hands in the client's timesheet and reads the hours worked from it, so they can be compared. A timesheet that cannot
 * be read is still handed in, to be compared by hand.
 */
public final class AttachingClientTimesheet implements AttachClientTimesheetUseCase {

  private static final Logger LOGGER = System.getLogger(AttachingClientTimesheet.class.getName());

  private final TimesheetDrafts drafts;
  private final ClientTimesheetReader reader;

  public AttachingClientTimesheet(TimesheetDrafts drafts, ClientTimesheetReader reader) {
    this.drafts = Objects.requireNonNull(drafts, "drafts");
    this.reader = Objects.requireNonNull(reader, "reader");
  }

  @Override
  public void attach(AttachClientTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var draft = drafts.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
    draft.attachClientTimesheet(command.document());
    readHoursReportedByClient(draft);
    drafts.save(draft);
  }

  private void readHoursReportedByClient(Timesheet draft) {
    var document = draft.clientTimesheet().orElseThrow();
    try {
      draft.recordHoursReportedByClient(reader.read(document));
    } catch (ClientTimesheetUnreadableException e) {
      LOGGER.log(Level.WARNING, "Could not read the hours from client timesheet %s".formatted(document), e);
      draft.markClientTimesheetUnreadable();
    }
  }
}
