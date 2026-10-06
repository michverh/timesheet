package eu.openvalue.timesheet.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Dutch public holidays observed by the company. Good Friday is never a day off; Liberation Day only in
 * lustrum years (every five years).
 */
public final class DutchPublicHolidays {

  public boolean isPublicHoliday(LocalDate date) {
    return observedIn(date.getYear()).contains(date);
  }

  public Set<LocalDate> observedIn(int year) {
    var easterSunday = easterSunday(year);
    var holidays = Stream.<LocalDate>builder()
        .add(LocalDate.of(year, Month.JANUARY, 1))
        .add(easterSunday.plusDays(1))
        .add(kingsDay(year))
        .add(easterSunday.plusDays(39))
        .add(easterSunday.plusDays(50))
        .add(LocalDate.of(year, Month.DECEMBER, 25))
        .add(LocalDate.of(year, Month.DECEMBER, 26));
    if (year % 5 == 0) {
      holidays.add(LocalDate.of(year, Month.MAY, 5));
    }
    return holidays.build().collect(Collectors.toUnmodifiableSet());
  }

  /**
   * King's Day is celebrated on 27 April, or on the 26th when the 27th falls on a Sunday.
   */
  private static LocalDate kingsDay(int year) {
    var kingsDay = LocalDate.of(year, Month.APRIL, 27);
    return kingsDay.getDayOfWeek() == DayOfWeek.SUNDAY ? kingsDay.minusDays(1) : kingsDay;
  }

  /**
   * Gregorian Easter Sunday, using the anonymous (Meeus/Jones/Butcher) algorithm.
   */
  private static LocalDate easterSunday(int year) {
    int a = year % 19;
    int b = year / 100;
    int c = year % 100;
    int d = b / 4;
    int e = b % 4;
    int f = (b + 8) / 25;
    int g = (b - f + 1) / 3;
    int h = (19 * a + b - d - g + 15) % 30;
    int i = c / 4;
    int k = c % 4;
    int l = (32 + 2 * e + 2 * i - h - k) % 7;
    int m = (a + 11 * h + 22 * l) / 451;
    int month = (h + l - 7 * m + 114) / 31;
    int day = (h + l - 7 * m + 114) % 31 + 1;
    return LocalDate.of(year, month, day);
  }
}
