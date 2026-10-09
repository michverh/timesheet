package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.DownloadTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.DownloadTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDownload;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.Timesheets;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import java.util.Objects;

public final class DownloadingTimesheet implements DownloadTimesheetUseCase {

  private final Timesheets timesheets;
  private final TimesheetSpreadsheet spreadsheet;

  public DownloadingTimesheet(Timesheets timesheets, TimesheetSpreadsheet spreadsheet) {
    this.timesheets = Objects.requireNonNull(timesheets, "timesheets");
    this.spreadsheet = Objects.requireNonNull(spreadsheet, "spreadsheet");
  }

  @Override
  public TimesheetDownload download(DownloadTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var draft = timesheets.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
    var file = spreadsheet.create(draft);
    return new TimesheetDownload(SpreadsheetFileName.of(draft, file.fileExtension()), file.mediaType(),
        file.content());
  }
}
