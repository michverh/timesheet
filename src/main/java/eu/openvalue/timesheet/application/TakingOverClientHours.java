package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.TakeOverClientHoursCommand;
import eu.openvalue.timesheet.application.port.in.TakeOverClientHoursUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import java.util.Objects;

public final class TakingOverClientHours implements TakeOverClientHoursUseCase {

  private final Timesheets timesheets;

  public TakingOverClientHours(Timesheets timesheets) {
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
  }

  @Override
  public void takeOverClientHours(TakeOverClientHoursCommand command) {
    ConcurrentChanges.retried(() -> takeOver(command));
  }

  private void takeOver(TakeOverClientHoursCommand command) {
    var professional = command.professional();
    var month = command.month();
    var draft = timesheets.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
    draft.takeOverHoursReportedByClient();
    timesheets.save(draft);
  }
}
