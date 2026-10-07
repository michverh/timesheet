package eu.openvalue.timesheet.application.port.out;

import eu.openvalue.timesheet.domain.Timesheet;

/**
 * Produces the spreadsheet of a timesheet, as handed in to HR.
 */
@FunctionalInterface
public interface TimesheetSpreadsheet {

  SpreadsheetFile create(Timesheet timesheet);
}
