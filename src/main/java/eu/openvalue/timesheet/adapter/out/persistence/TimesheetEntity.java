package eu.openvalue.timesheet.adapter.out.persistence;

import eu.openvalue.timesheet.domain.Assignment;
import eu.openvalue.timesheet.domain.Client;
import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Project;
import eu.openvalue.timesheet.domain.Timesheet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * How a timesheet is kept: one per professional and month.
 */
@Entity
@Table(name = "timesheet")
class TimesheetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private String professionalEmail;

  /**
   * The first day of the month.
   */
  @Column(nullable = false, updatable = false)
  private LocalDate month;

  private String clientName;

  private String projectName;

  @Column(nullable = false)
  private boolean registeredAbsencesUnknown;

  private LocalDate submittedOn;

  private LocalDate previouslySubmittedOn;

  @Column(nullable = false)
  private long version;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "client_timesheet_id")
  private ClientTimesheetEntity clientTimesheet;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "timesheet_id", nullable = false, updatable = false)
  @OrderBy("date")
  private List<TimesheetDayEntity> days = new ArrayList<>();

  protected TimesheetEntity() {
  }

  TimesheetEntity(Professional professional, YearMonth month) {
    this.professionalEmail = professional.email().value();
    this.month = month.atDay(1);
  }

  long version() {
    return version;
  }

  /**
   * Takes over the state of the timesheet, changing only what changed, as the next version.
   */
  void copy(Timesheet timesheet) {
    version++;
    clientName = timesheet.assignment().map(Assignment::client).map(Client::name).orElse(null);
    projectName = timesheet.assignment().flatMap(Assignment::project).map(Project::name).orElse(null);
    registeredAbsencesUnknown = timesheet.registeredAbsencesUnknown();
    submittedOn = timesheet.submittedOn().orElse(null);
    previouslySubmittedOn = timesheet.previouslySubmittedOn().orElse(null);
    copyDays(timesheet);
    copyClientTimesheet(timesheet);
  }

  Timesheet restore() {
    return Timesheet.restore(YearMonth.from(month), Professional.withEmail(professionalEmail), assignment(),
        days.stream().map(TimesheetDayEntity::restore).toList(), registeredAbsencesUnknown,
        Optional.ofNullable(submittedOn), Optional.ofNullable(previouslySubmittedOn),
        Optional.ofNullable(clientTimesheet).map(ClientTimesheetEntity::document),
        Optional.ofNullable(clientTimesheet).flatMap(ClientTimesheetEntity::hoursReportedByClient),
        clientTimesheet != null && clientTimesheet.isUnreadable());
  }

  private Optional<Assignment> assignment() {
    return Optional.ofNullable(clientName).map(client -> new Assignment(new Client(client),
        Optional.ofNullable(projectName).map(Project::new)));
  }

  /**
   * A timesheet always has the same days, so the kept days are updated in place.
   */
  private void copyDays(Timesheet timesheet) {
    var kept = days.stream().collect(Collectors.toMap(TimesheetDayEntity::date, Function.identity()));
    for (var day : timesheet.days()) {
      var entity = kept.get(day.date());
      if (entity == null) {
        entity = new TimesheetDayEntity(day.date());
        days.add(entity);
      }
      entity.copy(day);
    }
  }

  private void copyClientTimesheet(Timesheet timesheet) {
    var document = timesheet.clientTimesheet();
    if (document.isEmpty()) {
      clientTimesheet = null;
      return;
    }
    if (clientTimesheet == null || !clientTimesheet.holds(document.get())) {
      clientTimesheet = new ClientTimesheetEntity(document.get());
    }
    clientTimesheet.copyReading(timesheet);
  }
}
