package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.DownloadTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.DownloadTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDownload;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import java.util.Objects;

public final class DownloadingTimesheet implements DownloadTimesheetUseCase {

  private final CurrentTimesheets currentTimesheets;
  private final TimesheetSpreadsheet spreadsheet;

  public DownloadingTimesheet(CurrentTimesheets currentTimesheets, TimesheetSpreadsheet spreadsheet) {
    this.currentTimesheets = Objects.requireNonNull(currentTimesheets, "currentTimesheets");
    this.spreadsheet = Objects.requireNonNull(spreadsheet, "spreadsheet");
  }

  @Override
  public TimesheetDownload download(DownloadTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var draft = currentTimesheets.of(professional, month);
    var file = spreadsheet.create(draft);
    return new TimesheetDownload(SpreadsheetFileName.of(draft, file.fileExtension()), file.mediaType(),
        file.content());
  }
}
