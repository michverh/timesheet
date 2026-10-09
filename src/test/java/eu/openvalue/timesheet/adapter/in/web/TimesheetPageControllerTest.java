package eu.openvalue.timesheet.adapter.in.web;

import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.JAN;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.MAY_2026;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.draft;
import static eu.openvalue.timesheet.domain.HourCategory.OVERTIME;
import static eu.openvalue.timesheet.domain.HourCategory.WORKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.PrepareTimesheetDraftUseCase;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftCommand;
import eu.openvalue.timesheet.application.port.in.ResetTimesheetDraftUseCase;
import eu.openvalue.timesheet.domain.ClientReportedHours;
import eu.openvalue.timesheet.domain.ClientTimesheetDocument;
import eu.openvalue.timesheet.domain.DocumentFormat;
import eu.openvalue.timesheet.domain.DutchPublicHolidays;
import eu.openvalue.timesheet.domain.Hours;
import eu.openvalue.timesheet.domain.TimesheetPrefill;
import java.time.LocalDate;
import java.util.Map;
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
  void warnsWhenTheClientsTimesheetHasNotBeenUploaded() {
    // Given
    when(prepareDraft.prepareDraft(any())).thenReturn(draft());

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("The timesheet <span>ACME</span> keeps of your hours has not been uploaded yet")
        .contains("not uploaded", ">Upload</button>");
  }

  @Test
  void showsTheUploadedClientsTimesheetAndOffersToReplaceIt() {
    // Given
    var withClientTimesheet = draft();
    withClientTimesheet.attachClientTimesheet(
        new ClientTimesheetDocument("acme-may.pdf", DocumentFormat.PDF, new byte[] {1}));
    when(prepareDraft.prepareDraft(any())).thenReturn(withClientTimesheet);

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("<strong>acme-may.pdf</strong>", ">Replace</button>")
        .doesNotContain("has not been uploaded yet");
  }

  @Test
  void showsThePeriodTheClientsTimesheetCovers() {
    // Given
    var draft = draft();
    draft.attachClientTimesheet(new ClientTimesheetDocument("week-19.png", DocumentFormat.PNG, new byte[] {1}));
    draft.recordHoursReportedByClient(new ClientReportedHours(Map.of(
        LocalDate.of(2026, 5, 4), Hours.of(8),
        LocalDate.of(2026, 5, 10), Hours.ZERO)));
    when(prepareDraft.prepareDraft(any())).thenReturn(draft);

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("(4 – 10 May 2026)")
        .doesNotContain("Did you upload the client's timesheet for the right month?");
  }

  @Test
  void warnsWhenTheClientsTimesheetCoversAnotherPeriod() {
    // Given
    var draft = draft();
    draft.attachClientTimesheet(new ClientTimesheetDocument("week-27.png", DocumentFormat.PNG, new byte[] {1}));
    draft.recordHoursReportedByClient(new ClientReportedHours(Map.of(
        LocalDate.of(2026, 6, 29), Hours.of(8),
        LocalDate.of(2026, 7, 5), Hours.ZERO)));
    when(prepareDraft.prepareDraft(any())).thenReturn(draft);

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("<span>week-27.png</span> covers", "<span>29 June – 5 July 2026</span>, which is not part of",
            "Did you upload the client's timesheet for the right month?");
  }

  @Test
  void namesBothYearsOfAPeriodAcrossTheTurnOfTheYear() {
    // Given
    var draft = draft();
    draft.attachClientTimesheet(new ClientTimesheetDocument("week-1.png", DocumentFormat.PNG, new byte[] {1}));
    draft.recordHoursReportedByClient(new ClientReportedHours(Map.of(
        LocalDate.of(2025, 12, 29), Hours.of(8),
        LocalDate.of(2026, 1, 4), Hours.ZERO)));
    when(prepareDraft.prepareDraft(any())).thenReturn(draft);

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText().contains("(29 December 2025 – 4 January 2026)");
  }

  @Test
  void pointsOutWeekdaysThatDoNotAddUpToAStandardWorkday() {
    // Given
    var draft = draft();
    draft.rebookDay(LocalDate.of(2026, 5, 4), Map.of(WORKED, Hours.of(7, 30), OVERTIME, Hours.of(1)));
    when(prepareDraft.prepareDraft(any())).thenReturn(draft);

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("These weekdays do not add up to 8 hours", "Monday 4 May: 7:30 instead of 8")
        .containsPattern("id=\"day-2026-05-04\"[^>]*class=\"off-workday \"")
        .containsPattern("title=\"Adds up to 7:30 instead of 8 hours\">7:30</span>")
        .doesNotContainPattern("id=\"day-2026-05-05\"[^>]*class=\"off-workday");
  }

  @Test
  void asksToCompareByHandWhenTheClientsTimesheetCouldNotBeRead() {
    // Given
    var draft = draft();
    draft.attachClientTimesheet(new ClientTimesheetDocument("acme.pdf", DocumentFormat.PDF, new byte[] {1}));
    draft.markClientTimesheetUnreadable();
    when(prepareDraft.prepareDraft(any())).thenReturn(draft);

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText()
        .contains("The hours could not be read reliably from <span>acme.pdf</span>");
  }

  @Test
  void offersNoClientsTimesheetOnTheBench() {
    // Given
    when(prepareDraft.prepareDraft(any())).thenReturn(new TimesheetPrefill(new DutchPublicHolidays())
        .prefill(MAY_2026, JAN, Optional.empty()));

    // When
    var response = mvc.get().uri("/timesheets/2026-05");

    // Then
    assertThat(response).hasStatusOk().bodyText().doesNotContain("Client timesheet", "has not been uploaded yet");
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
