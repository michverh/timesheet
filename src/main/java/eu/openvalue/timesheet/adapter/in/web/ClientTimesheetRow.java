package eu.openvalue.timesheet.adapter.in.web;

import java.util.Optional;

/**
 * A client's timesheet imported into the timesheet, as listed on the page.
 *
 * @param period the days it covers, when it was read
 * @param ofAnotherPeriod whether it was read, but covers none of the days of the month
 */
public record ClientTimesheetRow(String fileName, Optional<String> period, boolean unreadable,
    boolean ofAnotherPeriod) {
}
