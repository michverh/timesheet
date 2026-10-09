package eu.openvalue.timesheet.adapter.out.persistence;

import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Timesheet;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * How the client's own timesheet handed in with a timesheet is kept, together with what was read from it.
 */
@Entity
@Table(name = "client_timesheet")
class ClientTimesheetEntity {

  /**
   * Whether the hours worked were read from the client's timesheet.
   */
  enum Reading {
    UNREAD, READ, UNREADABLE
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private String fileName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private DocumentFormat format;

  @Column(nullable = false, updatable = false)
  private byte[] content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Reading reading = Reading.UNREAD;

  @ElementCollection
  @CollectionTable(name = "client_reported_hours", joinColumns = @JoinColumn(name = "client_timesheet_id"))
  @MapKeyColumn(name = "day")
  @Column(name = "minutes_worked", nullable = false)
  private Map<LocalDate, Integer> reportedMinutesPerDay = new HashMap<>();

  protected ClientTimesheetEntity() {
  }

  ClientTimesheetEntity(ClientTimesheetDocument document) {
    this.fileName = document.fileName();
    this.format = document.format();
    this.content = document.content();
  }

  boolean holds(ClientTimesheetDocument document) {
    return document().equals(document);
  }

  /**
   * Keeps what was read from the client's timesheet.
   */
  void copyReading(Timesheet timesheet) {
    reading = timesheet.hoursReportedByClient().isPresent() ? Reading.READ
        : timesheet.isClientTimesheetUnreadable() ? Reading.UNREADABLE
        : Reading.UNREAD;
    var minutes = new HashMap<LocalDate, Integer>();
    timesheet.hoursReportedByClient().ifPresent(reported -> reported.hoursWorkedPerDay()
        .forEach((date, hours) -> minutes.put(date, hours.inMinutes())));
    if (!reportedMinutesPerDay.equals(minutes)) {
      reportedMinutesPerDay.clear();
      reportedMinutesPerDay.putAll(minutes);
    }
  }

  ClientTimesheetDocument document() {
    return new ClientTimesheetDocument(fileName, format, content);
  }

  Optional<ClientReportedHours> hoursReportedByClient() {
    if (reading != Reading.READ) {
      return Optional.empty();
    }
    var hours = new TreeMap<LocalDate, Hours>();
    reportedMinutesPerDay.forEach((date, minutes) -> hours.put(date, Hours.ofMinutes(minutes)));
    return Optional.of(new ClientReportedHours(hours));
  }

  boolean isUnreadable() {
    return reading == Reading.UNREADABLE;
  }
}
