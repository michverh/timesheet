package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayCommand;
import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.HourCategory;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Kilometres;
import eu.openvalue.timesheet.domain.Professional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.MultiValueMap;

/**
 * The input of one day as submitted from the timesheet page, exactly as entered.
 */
record DayForm(Map<HourCategory, String> hours, String kilometres, String explanation) {

  static String hoursParameter(HourCategory category) {
    return "hours-" + category.name();
  }

  static DayForm from(MultiValueMap<String, String> parameters) {
    var hours = new EnumMap<HourCategory, String>(HourCategory.class);
    for (var category : HourCategory.values()) {
      hours.put(category, text(parameters.getFirst(hoursParameter(category))));
    }
    return new DayForm(hours, text(parameters.getFirst("kilometres")), text(parameters.getFirst("explanation")));
  }

  /**
   * @throws InvalidDayInputException when the input cannot be understood as hours, kilometres and an explanation
   */
  AdjustTimesheetDayCommand toCommand(Professional professional, LocalDate date) {
    var bookedHours = new EnumMap<HourCategory, Hours>(HourCategory.class);
    hours.forEach((category, input) -> {
      var parsed = parseHours(category, input);
      if (!parsed.isZero()) {
        bookedHours.put(category, parsed);
      }
    });
    return new AdjustTimesheetDayCommand(professional, date, bookedHours, parseKilometres(kilometres),
        explanation.isBlank() ? Optional.empty() : Optional.of(new Explanation(explanation)));
  }

  private static Hours parseHours(HourCategory category, String input) {
    if (input.isBlank()) {
      return Hours.ZERO;
    }
    try {
      return HoursFormat.parse(input);
    } catch (IllegalArgumentException e) {
      throw new InvalidDayInputException(
          "Enter %s hours as hours and minutes, like 7:30".formatted(label(category)));
    }
  }

  /**
   * Accepts decimal kilometres, rounded to the nearest whole kilometre with halves rounded up (12.5 becomes 13).
   */
  private static Kilometres parseKilometres(String input) {
    if (input.isBlank()) {
      return Kilometres.ZERO;
    }
    try {
      var kilometres = new BigDecimal(input.replace(',', '.'));
      if (kilometres.signum() < 0) {
        throw new IllegalArgumentException("Kilometres cannot be negative: " + input);
      }
      return Kilometres.of(kilometres.setScale(0, RoundingMode.HALF_UP).intValueExact());
    } catch (IllegalArgumentException | ArithmeticException e) {
      throw new InvalidDayInputException("Enter kilometres as a positive number");
    }
  }

  private static String label(HourCategory category) {
    return category.name().toLowerCase().replace('_', ' ');
  }

  private static String text(String value) {
    return value == null ? "" : value.strip();
  }
}
