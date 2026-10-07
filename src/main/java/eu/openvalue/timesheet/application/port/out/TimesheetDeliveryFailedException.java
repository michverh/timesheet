package eu.openvalue.timesheet.application.port.out;

public class TimesheetDeliveryFailedException extends RuntimeException {

  public TimesheetDeliveryFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
