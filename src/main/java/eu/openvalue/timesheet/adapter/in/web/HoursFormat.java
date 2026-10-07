package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.Hours;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * How hours are entered and shown on the page: as hours and minutes ({@code 7:30}), whole hours ({@code 8}) or
 * decimal hours that amount to whole minutes ({@code 7.5} or {@code 7,5}).
 */
final class HoursFormat {

  private static final Pattern HOURS_AND_MINUTES = Pattern.compile("(\\d{1,3}):(\\d{1,2})");
  private static final Pattern DECIMAL_HOURS = Pattern.compile("\\d{1,3}([.,]\\d+)?");
  private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

  private HoursFormat() {
  }

  /**
   * @throws IllegalArgumentException when the text is not a non-negative amount of whole minutes
   */
  static Hours parse(String text) {
    var input = text.strip();
    var hoursAndMinutes = HOURS_AND_MINUTES.matcher(input);
    if (hoursAndMinutes.matches()) {
      return Hours.of(Integer.parseInt(hoursAndMinutes.group(1)), Integer.parseInt(hoursAndMinutes.group(2)));
    }
    if (DECIMAL_HOURS.matcher(input).matches()) {
      var minutes = new BigDecimal(input.replace(',', '.')).multiply(MINUTES_PER_HOUR);
      try {
        return Hours.ofMinutes(minutes.intValueExact());
      } catch (ArithmeticException e) {
        throw new IllegalArgumentException("Not a whole number of minutes: " + text, e);
      }
    }
    throw new IllegalArgumentException("Not an amount of hours: " + text);
  }

  /**
   * @return whole hours as a number ({@code 8}), anything else as hours and minutes ({@code 7:05})
   */
  static String format(Hours hours) {
    return hours.remainingMinutes() == 0 ? Integer.toString(hours.wholeHours()) : hours.toString();
  }
}
