package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.application.port.in.DownloadTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.DownloadTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDownload;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.application.port.out.TimesheetDrafts;
import eu.openvalue.timesheet.application.port.out.TimesheetSpreadsheet;
import eu.openvalue.timesheet.domain.Timesheet;
import java.util.Objects;

public final class DownloadTimesheet implements DownloadTimesheetUseCase {

  private final TimesheetDrafts drafts;
  private final TimesheetSpreadsheet spreadsheet;

  public DownloadTimesheet(TimesheetDrafts drafts, TimesheetSpreadsheet spreadsheet) {
    this.drafts = Objects.requireNonNull(drafts, "drafts");
    this.spreadsheet = Objects.requireNonNull(spreadsheet, "spreadsheet");
  }

  @Override
  public TimesheetDownload download(DownloadTimesheetCommand command) {
    var professional = command.professional();
    var month = command.month();
    var draft = drafts.find(professional, month)
        .orElseThrow(() -> new TimesheetDraftNotFoundException(professional, month));
    var file = spreadsheet.create(draft);
    return new TimesheetDownload(fileNameOf(draft, file.fileExtension()), file.mediaType(), file.content());
  }

  private static String fileNameOf(Timesheet timesheet, String fileExtension) {
    var name = timesheet.professional().name().replace(' ', '_');
    return "Urenverantwoording_%s_%s.%s".formatted(timesheet.month(), name, fileExtension);
  }
}
