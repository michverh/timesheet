package eu.openvalue.timesheet.application.port.out;

public class ClientTimesheetUnreadableException extends RuntimeException {

  public ClientTimesheetUnreadableException(String message) {
    super(message);
  }

  public ClientTimesheetUnreadableException(String message, Throwable cause) {
    super(message, cause);
  }
}
