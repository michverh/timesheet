package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.openvalue.timesheet.domain.SubmissionState.Draft;
import eu.openvalue.timesheet.domain.SubmissionState.Submitted;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SubmissionStateTest {

  private static final LocalDate FIRST_SUBMISSION = LocalDate.of(2026, 6, 1);
  private static final LocalDate CORRECTION = LocalDate.of(2026, 6, 5);

  @Test
  void startsAsANewDraftThatWasNeverSubmitted() {
    assertThat(Draft.NEW.previouslySubmittedOn()).isEmpty();
  }

  @Test
  void isSubmittedOnTheDayOfSubmission() {
    // When
    var submitted = Draft.NEW.submit(FIRST_SUBMISSION);

    // Then
    assertThat(submitted).isEqualTo(new Submitted(FIRST_SUBMISSION, Optional.empty()));
  }

  @Test
  void becomesADraftCorrectingTheSubmissionWhenReopened() {
    // When
    var reopened = Draft.NEW.submit(FIRST_SUBMISSION).reopen();

    // Then
    assertThat(reopened.previouslySubmittedOn()).contains(FIRST_SUBMISSION);
  }

  @Test
  void remembersWhichSubmissionACorrectionReplaces() {
    // When
    var corrected = Draft.NEW.submit(FIRST_SUBMISSION).reopen().submit(CORRECTION);

    // Then
    assertThat(corrected).isEqualTo(new Submitted(CORRECTION, Optional.of(FIRST_SUBMISSION)));
  }

  @Test
  void correctsTheLatestSubmissionWhenReopenedAgain() {
    // When
    var reopenedAgain = Draft.NEW.submit(FIRST_SUBMISSION).reopen().submit(CORRECTION).reopen();

    // Then
    assertThat(reopenedAgain.previouslySubmittedOn()).contains(CORRECTION);
  }

  @Test
  void isADraftWhenNotSubmitted() {
    assertThat(SubmissionState.of(Optional.empty(), Optional.of(FIRST_SUBMISSION)))
        .isEqualTo(new Draft(Optional.of(FIRST_SUBMISSION)));
  }

  @Test
  void isSubmittedWhenSubmitted() {
    assertThat(SubmissionState.of(Optional.of(CORRECTION), Optional.of(FIRST_SUBMISSION)))
        .isEqualTo(new Submitted(CORRECTION, Optional.of(FIRST_SUBMISSION)));
  }

  @Test
  void cannotBeSubmittedBeforeTheSubmissionItCorrects() {
    assertThatThrownBy(() -> new Submitted(FIRST_SUBMISSION, Optional.of(CORRECTION)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
