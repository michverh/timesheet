package eu.openvalue.timesheet.adapter.in.web;

import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.JAN;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.MAY_2026;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.draft;
import static eu.openvalue.timesheet.domain.HourCategory.LEAVE;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayCommand;
import eu.openvalue.timesheet.application.port.in.AdjustTimesheetDayUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetDraftNotFoundException;
import eu.openvalue.timesheet.domain.Explanation;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.Kilometres;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(TimesheetDayController.class)
@Import(WebTestConfiguration.class)
class TimesheetDayControllerTest {

  private static final LocalDate MONDAY = LocalDate.of(2026, 5, 4);

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private AdjustTimesheetDayUseCase adjustDay;

  @Test
  void adjustsTheDayOfTheCurrentProfessionalWithTheEnteredValues() {
    // Given
    when(adjustDay.adjustDay(any())).thenReturn(draft());

    // When
    mvc.post().uri("/timesheets/2026-05/days/2026-05-04")
        .param("hours-WORKED", "6").param("hours-LEAVE", "2")
        .param("kilometres", "42").param("explanation", "Dentist").exchange();

    // Then
    var command = ArgumentCaptor.forClass(AdjustTimesheetDayCommand.class);
    verify(adjustDay).adjustDay(command.capture());
    assertThat(command.getValue()).isEqualTo(new AdjustTimesheetDayCommand(JAN, MONDAY,
        Map.of(WORKED, Hours.of(6), LEAVE, Hours.of(2)), Kilometres.of(42), Optional.of(new Explanation("Dentist"))));
  }

  @Test
  void respondsWithTheAdjustedRowAndUpdatedTotals() {
    // Given
    var adjusted = draft();
    adjusted.rebookDay(MONDAY, Map.of(WORKED, Hours.of(5, 30), LEAVE, Hours.of(2, 30)));
    when(adjustDay.adjustDay(any())).thenReturn(adjusted);

    // When
    var response = mvc.post().uri("/timesheets/2026-05/days/2026-05-04").param("hours-WORKED", "5:30")
        .param("hours-LEAVE", "2:30");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("id=\"day-2026-05-04\"")
        .doesNotContain("id=\"day-2026-05-05\"")
        .contains("<template>", "id=\"totals\" hx-swap-oob=\"true\"", "id=\"warnings\"")
        .contains("value=\"5:30\"", "<td>149:30</td>", "<td>2:30</td>");
  }

  @Test
  void showsInvalidInputAgainWithTheReasonInsteadOfAdjustingTheDay() {
    // When
    var response = mvc.post().uri("/timesheets/2026-05/days/2026-05-04").param("hours-WORKED", "7:75");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("value=\"7:75\"", "Enter worked hours as hours and minutes, like 7:30")
        .doesNotContain("hx-swap-oob");
    verify(adjustDay, never()).adjustDay(any());
  }

  @Test
  void showsABrokenRuleInTheRow() {
    // Given
    when(adjustDay.adjustDay(any())).thenAnswer(invocation -> {
      draft().bookHours(MONDAY, LEAVE, Hours.of(20));
      return draft();
    });

    // When
    var response = mvc.post().uri("/timesheets/2026-05/days/2026-05-04").param("hours-LEAVE", "20");

    // Then
    assertThat(response).hasStatusOk().bodyText().contains("No more than 24 hours can be booked on 2026-05-04");
  }

  @Test
  void reportsALostDraft() {
    // Given
    when(adjustDay.adjustDay(any())).thenThrow(new TimesheetDraftNotFoundException(JAN, MAY_2026));

    // When
    var response = mvc.post().uri("/timesheets/2026-05/days/2026-05-04").param("hours-WORKED", "8");

    // Then
    assertThat(response).hasStatus(HttpStatus.NOT_FOUND).bodyText().contains("Reload the page");
  }

  @Test
  void rejectsADayOutsideTheMonth() {
    assertThat(mvc.post().uri("/timesheets/2026-05/days/2026-06-01").param("hours-WORKED", "8"))
        .hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void updatesTheHolidayLeaveAlongsideTheRow() {
    // Given
    when(adjustDay.adjustDay(any())).thenReturn(draft());

    // When
    var response = mvc.post().uri("/timesheets/2026-05/days/2026-05-04").param("hours-LEAVE", "8").exchange();

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .containsPattern("id=\"holiday-leave\"[^>]*hx-swap-oob=\"true\"")
        .contains("<dt>Available</dt><dd>152</dd>");
  }
}
