package eu.openvalue.timesheet.adapter.in.web;

import eu.openvalue.timesheet.domain.HolidayLeave;

/**
 * The professional's holiday leave in the year of the timesheet, as shown on the page.
 *
 * @param overdrawn how much more leave was taken and reserved than the entitlement; empty when within it
 */
public record HolidayLeaveSummary(int year, String entitlement, String taken, String reserved, String available,
    String overdrawn) {

  static HolidayLeaveSummary of(HolidayLeave leave) {
    return new HolidayLeaveSummary(leave.year().getValue(), HoursFormat.format(leave.entitlement()),
        HoursFormat.format(leave.taken()), HoursFormat.format(leave.reserved()), HoursFormat.format(leave.available()),
        leave.overdrawn().isZero() ? "" : HoursFormat.format(leave.overdrawn()));
  }

  public boolean isOverdrawn() {
    return !overdrawn.isEmpty();
  }
}
