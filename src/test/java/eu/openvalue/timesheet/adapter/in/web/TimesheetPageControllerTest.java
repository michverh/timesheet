package eu.openvalue.timesheet.adapter.in.web;

import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.JAN;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.MAY_2026;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.draft;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(TimesheetPageController.class)
@Import(WebTestConfiguration.class)
class TimesheetPageControllerTest {

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private PrepareTimesheetDraftUseCase prepareDraft;

  @MockitoBean
  private ResetTimesheetDraftUseCase resetDraft;

  @Test
  void opensTheTimesheetOfTheCurrentMonth() {
    assertThat(mvc.get().uri("/")).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/timesheets/2026-05");
  }

  @Test
  void showsTheDraftOfTheCurrentProfessional() {
    // Given
    when(prepareDraft.prepareDraft(new PrepareTimesheetDraftCommand(JAN, MAY_2026))).thenReturn(draft());

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("Jan Jansen", "May 2026", "ACME", "Platform")
        .contains("id=\"day-2026-05-31\"")
        .containsPattern("id=\"2026-05-04-WORKED\"\\s+name=\"hours-WORKED\"\\s+value=\"8\"");
  }

  @Test
  void offersToResetAndSubmitADraft() {
    // Given
    when(prepareDraft.prepareDraft(any())).thenReturn(draft());

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("Reset to prefilled hours", "Submit to HR")
        .doesNotContain("Make a correction", "fieldset class=\"days\" disabled");
  }

  @Test
  void showsASubmittedTimesheetReadOnlyWithTheOptionToCorrectIt() {
    // Given
    var submitted = draft();
    submitted.submit(LocalDate.of(2026, 6, 1));
    when(prepareDraft.prepareDraft(any())).thenReturn(submitted);

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("Submitted to HR on <strong>1 June 2026</strong>")
        .contains("fieldset class=\"days\" disabled=\"disabled\"", "Make a correction")
        .doesNotContain("Reset to prefilled hours", "Submit to HR");
  }

  @Test
  void offersToSubmitTheCorrectionOfAReopenedTimesheet() {
    // Given
    var reopened = draft();
    reopened.submit(LocalDate.of(2026, 6, 1));
    reopened.reopenForCorrection();
    when(prepareDraft.prepareDraft(any())).thenReturn(reopened);

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("You are correcting the timesheet submitted on", "1 June 2026", "Submit correction")
        .doesNotContain("Make a correction");
  }

  @Test
  void warnsThatLeaveAndSicknessMayBeMissingWhenTheAbsencesWereUnknown() {
    // Given
    when(prepareDraft.prepareDraft(any())).thenReturn(new TimesheetPrefill(new DutchPublicHolidays())
        .prefillWithoutRegisteredAbsences(MAY_2026, JAN, Optional.empty()));

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("absence registry could not be reached", "no leave or sickness has been filled");
  }

  @Test
  void doesNotWarnAboutAbsencesWhenTheyWereKnown() {
    // Given
    when(prepareDraft.prepareDraft(any())).thenReturn(draft());

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText().doesNotContain("absence registry could not be reached");
  }

  @Test
  void prefillsTheMonthAgainAndLetsThePageReload() {
    // When
    var response = mvc.post().uri("/timesheets/2026-05/reset");

    // Then
    assertThat(response).hasStatus(HttpStatus.NO_CONTENT).hasHeader("HX-Refresh", "true");
    verify(resetDraft).resetDraft(new ResetTimesheetDraftCommand(JAN, MAY_2026));
  }
}
