package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.Hours;

/**
 * How hours are shown on the page.
 */
final class HoursFormat {

  private HoursFormat() {
  }

  /**
   * @return whole hours as a number ({@code 8}), anything else as hours and minutes ({@code 7:05})
   */
  static String format(Hours hours) {
    return hours.remainingMinutes() == 0 ? Integer.toString(hours.wholeHours()) : hours.toString();
  }
}
