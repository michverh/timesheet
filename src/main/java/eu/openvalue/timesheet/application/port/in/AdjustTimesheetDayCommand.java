package eu.openvalue.timesheet.application.port.in;

import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Kilometres;
import eu.openvalue.timesheet.domain.Professional;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @param hours the hours per category; categories left out are cleared
 * @param explanation the explanation of the day, or empty to remove it
 */
public record AdjustTimesheetDayCommand(Professional professional, LocalDate date, Map<HourCategory, Hours> hours,
    Kilometres kilometres, Optional<Explanation> explanation) {

  public AdjustTimesheetDayCommand {
    Objects.requireNonNull(professional, "professional");
    Objects.requireNonNull(date, "date");
    hours = Map.copyOf(hours);
    Objects.requireNonNull(kilometres, "kilometres");
    Objects.requireNonNull(explanation, "explanation");
  }
}
