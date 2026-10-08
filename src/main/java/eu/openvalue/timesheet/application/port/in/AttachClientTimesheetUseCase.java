package eu.openvalue.timesheet.application.port.in;

public interface AttachClientTimesheetUseCase {

  /**
   * Hands in the client's own timesheet alongside the draft timesheet of a professional for a month, replacing any
   * handed in before, and reads the hours worked from it to compare them. When they cannot be read, the client's
   * timesheet is handed in all the same, marked as unreadable.
   *
   * @throws TimesheetDraftNotFoundException when there is no draft for the month
   * @throws eu.openvalue.timesheet.domain.TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  void attach(AttachClientTimesheetCommand command);
}
