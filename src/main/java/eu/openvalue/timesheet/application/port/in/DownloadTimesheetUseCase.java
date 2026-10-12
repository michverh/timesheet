package eu.openvalue.timesheet.application.port.in;

public interface DownloadTimesheetUseCase {

  /**
   * Gives the draft timesheet of a professional for a month as the spreadsheet handed in to HR.
   */
  TimesheetDownload download(DownloadTimesheetCommand command);
}
