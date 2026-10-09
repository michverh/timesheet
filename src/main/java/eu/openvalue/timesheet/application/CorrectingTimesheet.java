package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.CorrectTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.CorrectTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import java.util.Objects;

public final class CorrectingTimesheet implements CorrectTimesheetUseCase {

  private final Timesheets timesheets;

  public CorrectingTimesheet(Timesheets timesheets) {
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
  }

  @Override
  public void reopenForCorrection(CorrectTimesheetCommand command) {
    ConcurrentChanges.retried(() -> reopen(command));
  }

  private void reopen(CorrectTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var timesheet = timesheets.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
    timesheet.reopenForCorrection();
    timesheets.save(timesheet);
  }
}
