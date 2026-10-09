package eu.openvalue.timesheet.adapter.out.persistence;

import eu.openvalue.timesheet.domain.DayEntry;
import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Kilometres;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * How a day of a timesheet is kept.
 */
@Entity
@Table(name = "timesheet_day")
class TimesheetDayEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "day", nullable = false, updatable = false)
  private LocalDate date;

  @Column(nullable = false)
  private int kilometres;

  private String explanation;

  @ElementCollection
  @CollectionTable(name = "timesheet_day_hours", joinColumns = @JoinColumn(name = "timesheet_day_id"))
  @MapKeyColumn(name = "category")
  @MapKeyEnumerated(EnumType.STRING)
  @Column(name = "minutes", nullable = false)
  private Map<HourCategory, Integer> minutesPerCategory = new HashMap<>();

  protected TimesheetDayEntity() {
  }

  TimesheetDayEntity(LocalDate date) {
    this.date = date;
  }

  LocalDate date() {
    return date;
  }

  void copy(DayEntry day) {
    kilometres = day.kilometres().value();
    explanation = day.explanation().map(Explanation::text).orElse(null);
    var minutes = new EnumMap<HourCategory, Integer>(HourCategory.class);
    day.bookedHours().forEach((category, hours) -> minutes.put(category, hours.inMinutes()));
    if (!minutesPerCategory.equals(minutes)) {
      minutesPerCategory.clear();
      minutesPerCategory.putAll(minutes);
    }
  }

  DayEntry restore() {
    var hours = new EnumMap<HourCategory, Hours>(HourCategory.class);
    minutesPerCategory.forEach((category, minutes) -> hours.put(category, Hours.ofMinutes(minutes)));
    return DayEntry.of(date, hours, Kilometres.of(kilometres), Optional.ofNullable(explanation).map(Explanation::new));
  }
}
