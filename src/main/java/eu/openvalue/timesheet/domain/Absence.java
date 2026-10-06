package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A period, in whole days, during which the professional is absent on leave.
 */
public record Absence(LocalDate firstDay, LocalDate lastDay) {

  public Absence {
    Objects.requireNonNull(firstDay, "firstDay");
    Objects.requireNonNull(lastDay, "lastDay");
    if (lastDay.isBefore(firstDay)) {
      throw new IllegalArgumentException("Absence cannot end (%s) before it starts (%s)".formatted(lastDay, firstDay));
    }
  }

  public static Absence on(LocalDate day) {
    return new Absence(day, day);
  }

  public boolean covers(LocalDate day) {
    return !day.isBefore(firstDay) && !day.isAfter(lastDay);
  }
}
