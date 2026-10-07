package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.CorrectTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.CorrectTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import java.util.Objects;

public final class CorrectingTimesheet implements CorrectTimesheetUseCase {

  private final TimesheetDrafts drafts;

  public CorrectingTimesheet(TimesheetDrafts drafts) {
    this.drafts = Objects.requireNonNull(drafts, "drafts");
  }

  @Override
  public void reopenForCorrection(CorrectTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var timesheet = drafts.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
    timesheet.reopenForCorrection();
    drafts.save(timesheet);
  }
}
