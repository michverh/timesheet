package eu.openvalue.timesheet.application.port.in;

public interface AttachClientTimesheetUseCase {

  /**
   * Hands in the client's own timesheet alongside the draft timesheet of a professional for a month, replacing any
   * handed in before.
   *
   * @throws TimesheetDraftNotFoundException when there is no draft for the month
   * @throws eu.openvalue.timesheet.domain.TimesheetAlreadySubmittedException when the timesheet has been submitted
   */
  void attach(AttachClientTimesheetCommand command);
}
