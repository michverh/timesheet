package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.Professional;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

/**
 * @param documents the client's timesheets for the month, typically one per week
 */
public record ImportClientTimesheetsCommand(Professional professional, YearMonth month,
    List<ClientTimesheetDocument> documents) {

  public ImportClientTimesheetsCommand {
    Objects.requireNonNull(professional, "professional");
    Objects.requireNonNull(month, "month");
    documents = List.copyOf(documents);
    if (documents.isEmpty()) {
      throw new IllegalArgumentException("Import at least one client's timesheet");
    }
  }
}
