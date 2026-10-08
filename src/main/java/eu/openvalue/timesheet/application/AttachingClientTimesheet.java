package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.AttachClientTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import java.util.Objects;

public final class AttachingClientTimesheet implements AttachClientTimesheetUseCase {

  private final TimesheetDrafts drafts;

  public AttachingClientTimesheet(TimesheetDrafts drafts) {
    this.drafts = Objects.requireNonNull(drafts, "drafts");
  }

  @Override
  public void attach(AttachClientTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var draft = drafts.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
    draft.attachClientTimesheet(command.document());
    drafts.save(draft);
  }
}
