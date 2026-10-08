package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;

/**
 * Reads the hours worked from the timesheet a client keeps, whatever its layout.
 */
@FunctionalInterface
public interface ClientTimesheetReader {

  /**
   * @throws ClientTimesheetUnreadableException when the hours worked cannot be read from the document
   */
  ClientReportedHours read(ClientTimesheetDocument document);
}
