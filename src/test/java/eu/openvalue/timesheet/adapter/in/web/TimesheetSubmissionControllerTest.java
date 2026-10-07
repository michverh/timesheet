package eu.openvalue.timesheet.adapter.in.web;

import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.JAN;
import static eu.openvalue.timesheet.adapter.in.web.WebTestConfiguration.MAY_2026;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import eu.openvalue.timesheet.application.port.in.CorrectTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.CorrectTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.SubmitTimesheetCommand;
import eu.openvalue.timesheet.application.port.in.SubmitTimesheetUseCase;
import eu.openvalue.timesheet.application.port.in.TimesheetSubmissionFailedException;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(TimesheetSubmissionController.class)
@Import(WebTestConfiguration.class)
class TimesheetSubmissionControllerTest {

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private SubmitTimesheetUseCase submit;

  @MockitoBean
  private CorrectTimesheetUseCase correct;

  @Test
  void submitsTheTimesheetOfTheCurrentProfessionalAndLetsThePageReload() {
    // When
    var response = mvc.post().uri("/timesheets/2026-05/submission");

    // Then
    assertThat(response).hasStatus(HttpStatus.NO_CONTENT).hasHeader("HX-Refresh", "true");
    verify(submit).submit(new SubmitTimesheetCommand(JAN, MAY_2026));
  }

  @Test
  void reopensTheTimesheetForCorrectionAndLetsThePageReload() {
    // When
    var response = mvc.post().uri("/timesheets/2026-05/correction");

    // Then
    assertThat(response).hasStatus(HttpStatus.NO_CONTENT).hasHeader("HX-Refresh", "true");
    verify(correct).reopenForCorrection(new CorrectTimesheetCommand(JAN, MAY_2026));
  }

  @Test
  void explainsThatAFailedSubmissionCanBeTriedAgain() {
    // Given
    doThrow(new TimesheetSubmissionFailedException(MAY_2026, null)).when(submit).submit(any());

    // When
    var response = mvc.post().uri("/timesheets/2026-05/submission");

    // Then
    assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE).bodyText()
        .contains("could not be sent to HR", "try again later");
  }

  @Test
  void explainsWhyATimesheetCannotBeSubmitted() {
    // Given
    doThrow(futureMonthNotSubmittable()).when(submit).submit(any());

    // When
    var response = mvc.post().uri("/timesheets/2026-07/submission");

    // Then
    assertThat(response).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT).bodyText()
        .contains("cannot be submitted before that month has started");
  }

  private static RuntimeException futureMonthNotSubmittable() {
    try {
      Timesheet.start(YearMonth.of(2026, 7), JAN, Optional.empty()).submit(LocalDate.of(2026, 5, 15));
      throw new AssertionError("Expected the submission to be refused");
    } catch (RuntimeException e) {
      return e;
    }
  }
}
