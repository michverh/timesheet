package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.time.Year;
import java.util.Collection;
import java.util.Objects;

/**
 * A professional's holiday leave in a year: what they are entitled to, what they took in submitted timesheets, what
 * they reserved in timesheets not submitted yet, and what is left.
 */
public record HolidayLeave(Year year, Hours entitlement, Hours taken, Hours reserved) {

  /**
   * The holiday leave of a full year when working full time.
   */
  public static final Hours FULL_YEAR_ENTITLEMENT = Hours.of(208);

  public HolidayLeave {
    Objects.requireNonNull(year, "year");
    Objects.requireNonNull(entitlement, "entitlement");
    Objects.requireNonNull(taken, "taken");
    Objects.requireNonNull(reserved, "reserved");
  }

  /**
   * The holiday leave in a year of a professional employed since the given date, from the leave booked in their
   * timesheets of that year.
   */
  public static HolidayLeave of(Year year, LocalDate employedSince, Collection<BookedLeave> booked) {
    Objects.requireNonNull(booked, "booked");
    var inYear = booked.stream().filter(leave -> Year.from(leave.month()).equals(year)).toList();
    return new HolidayLeave(year, entitlementIn(year, employedSince),
        inYear.stream().filter(BookedLeave::submitted).map(BookedLeave::hours).reduce(Hours.ZERO, Hours::plus),
        inYear.stream().filter(leave -> !leave.submitted()).map(BookedLeave::hours).reduce(Hours.ZERO, Hours::plus));
  }

  /**
   * The full year's entitlement, in proportion to the days employed in the year, rounded to whole hours.
   */
  static Hours entitlementIn(Year year, LocalDate employedSince) {
    Objects.requireNonNull(employedSince, "employedSince");
    if (employedSince.getYear() > year.getValue()) {
      return Hours.ZERO;
    }
    if (employedSince.getYear() < year.getValue()) {
      return FULL_YEAR_ENTITLEMENT;
    }
    var daysEmployed = year.length() - employedSince.getDayOfYear() + 1;
    return Hours.of((int) Math.round((double) FULL_YEAR_ENTITLEMENT.wholeHours() * daysEmployed / year.length()));
  }

  /**
   * What is left of the entitlement once the leave taken and reserved is subtracted; none when overdrawn.
   */
  public Hours available() {
    var used = taken.plus(reserved);
    return entitlement.isGreaterThan(used) ? entitlement.minus(used) : Hours.ZERO;
  }

  /**
   * How much more leave was taken and reserved than the entitlement; none when within it.
   */
  public Hours overdrawn() {
    var used = taken.plus(reserved);
    return used.isGreaterThan(entitlement) ? used.minus(entitlement) : Hours.ZERO;
  }
}
