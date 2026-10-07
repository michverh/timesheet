package eu.openvalue.timesheet.application.port.out;

/**
 * Hands submitted timesheets in to HR.
 */
@FunctionalInterface
public interface TimesheetDelivery {

  /**
   * @throws TimesheetDeliveryFailedException when the timesheet could not be handed in
   */
  void deliver(TimesheetSubmission submission);
}
