package eu.openvalue.timesheet.domain;

import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

/**
 * The client's own timesheet, handed in alongside a timesheet, and what could be read from it: not read yet, read,
 * or unreadable, so it has to be compared by hand.
 */
public final class ClientTimesheet {

  private final ClientTimesheetDocument document;
  private final Reading reading;

  private ClientTimesheet(ClientTimesheetDocument document, Reading reading) {
    this.document = Objects.requireNonNull(document, "document");
    this.reading = reading;
  }

  /**
   * A client's timesheet that has been handed in, but not read yet.
   */
  public static ClientTimesheet handedIn(ClientTimesheetDocument document) {
    return new ClientTimesheet(document, Reading.Unread.INSTANCE);
  }

  /**
   * @return this client's timesheet, with the hours worked as read from it
   */
  public ClientTimesheet readAs(ClientReportedHours reported) {
    return new ClientTimesheet(document, new Reading.Read(Objects.requireNonNull(reported, "reported")));
  }

  /**
   * @return this client's timesheet, from which the hours worked could not be read
   */
  public ClientTimesheet unreadable() {
    return new ClientTimesheet(document, Reading.Unreadable.INSTANCE);
  }

  public ClientTimesheetDocument document() {
    return document;
  }

  /**
   * The hours worked as read from the client's timesheet; empty when it has not been read, or could not be.
   */
  public Optional<ClientReportedHours> hoursReportedByClient() {
    return reading instanceof Reading.Read read ? Optional.of(read.hours()) : Optional.empty();
  }

  public boolean isUnreadable() {
    return reading instanceof Reading.Unreadable;
  }

  /**
   * Whether the client's timesheet was read, but concerns another period: it covers none of the days of the month.
   */
  boolean isOfAnotherPeriodThan(YearMonth month) {
    return hoursReportedByClient()
        .filter(reported -> !reported.isEmpty() && !reported.coversAnyDayOf(month))
        .isPresent();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ClientTimesheet that && document.equals(that.document) && reading.equals(that.reading);
  }

  @Override
  public int hashCode() {
    return Objects.hash(document, reading);
  }

  @Override
  public String toString() {
    return "ClientTimesheet[" + document + ", " + reading + "]";
  }

  private sealed interface Reading {

    enum Unread implements Reading {
      INSTANCE
    }

    record Read(ClientReportedHours hours) implements Reading {
    }

    enum Unreadable implements Reading {
      INSTANCE
    }
  }
}
