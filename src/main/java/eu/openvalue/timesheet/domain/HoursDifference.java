package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A day on which the hours worked differ from what the client registered.
 *
 * @param worked the hours booked as worked on the timesheet
 * @param reportedByClient the hours worked according to the client's timesheet
 */
public record HoursDifference(LocalDate date, Hours worked, Hours reportedByClient) {

  public HoursDifference {
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(worked, "worked");
    Objects.requireNonNull(reportedByClient, "reportedByClient");
  }
}
