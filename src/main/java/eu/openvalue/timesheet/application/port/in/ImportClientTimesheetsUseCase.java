package eu.openvalue.timesheet.application.port.in;

public interface ImportClientTimesheetsUseCase {

  /**
   * Reads the hours worked from the client's own timesheets and hands them in alongside the draft timesheet of a
   * professional for a month, replacing those imported before, so the hours worked they agree on are booked. A
   * client's timesheet whose hours cannot be read is imported all the same, marked as unreadable.
   *
   * @throws TimesheetDraftNotFoundException when there is no draft for the month
   * @throws eu.openvalue.timesheet.domain.TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  void importClientTimesheets(ImportClientTimesheetsCommand command);
}
