package eu.openvalue.timesheet.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Where a timesheet is in its submission to HR: still a draft, possibly correcting an earlier submission, or
 * submitted.
 */
sealed interface SubmissionState {

  /**
   * When the timesheet was last submitted before it was reopened for correction, if it ever was.
   */
  Optional<LocalDate> previouslySubmittedOn();

  /**
   * @param submittedOn when the timesheet was submitted, or empty while it is a draft
   */
  static SubmissionState of(Optional<LocalDate> submittedOn, Optional<LocalDate> previouslySubmittedOn) {
    return submittedOn.<SubmissionState>map(on -> new Submitted(on, previouslySubmittedOn))
        .orElseGet(() -> new Draft(previouslySubmittedOn));
  }

  record Draft(Optional<LocalDate> previouslySubmittedOn) implements SubmissionState {

    static final Draft NEW = new Draft(Optional.empty());

    public Draft {
      Objects.requireNonNull(previouslySubmittedOn, "previouslySubmittedOn");
    }

    Submitted submit(LocalDate today) {
      return new Submitted(today, previouslySubmittedOn);
    }
  }

  record Submitted(LocalDate on, Optional<LocalDate> previouslySubmittedOn) implements SubmissionState {

    public Submitted {
      Objects.requireNonNull(on, "on");
      Objects.requireNonNull(previouslySubmittedOn, "previouslySubmittedOn");
      if (previouslySubmittedOn.filter(on::isBefore).isPresent()) {
        throw new IllegalArgumentException("A correction cannot be submitted before the submission it corrects");
      }
    }

    Draft reopen() {
      return new Draft(Optional.of(on));
    }
  }
}
