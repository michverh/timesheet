package eu.openvalue.timesheet.application.port.in;

public interface TakeOverClientHoursUseCase {

  /**
   * Books the hours worked according to the client's timesheet on the days where they differ from the professional's
   * and nothing else fills the workday.
   *
   * @throws TimesheetDraftNotFoundException when there is no timesheet for the month
   */
  void takeOverClientHours(TakeOverClientHoursCommand command);
}
