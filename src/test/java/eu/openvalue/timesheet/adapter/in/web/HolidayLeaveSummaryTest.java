package eu.openvalue.timesheet.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.openvalue.timesheet.domain.HolidayLeave;
import eu.openvalue.timesheet.domain.Hours;
import java.time.Year;
import org.junit.jupiter.api.Test;

class HolidayLeaveSummaryTest {

  @Test
  void showsTheHoursOfTheYear() {
    // When
    var summary = HolidayLeaveSummary.of(new HolidayLeave(Year.of(2026), Hours.of(208), Hours.of(40),
        Hours.of(7, 30)));

    // Then
    assertThat(summary).isEqualTo(new HolidayLeaveSummary(2026, "208", "40", "7:30", "160:30", ""));
    assertThat(summary.isOverdrawn()).isFalse();
  }

  @Test
  void showsHowMuchIsOverdrawn() {
    // When
    var summary = HolidayLeaveSummary.of(new HolidayLeave(Year.of(2026), Hours.of(100), Hours.of(96), Hours.of(12)));

    // Then
    assertThat(summary.overdrawn()).isEqualTo("8");
    assertThat(summary.isOverdrawn()).isTrue();
  }
}
