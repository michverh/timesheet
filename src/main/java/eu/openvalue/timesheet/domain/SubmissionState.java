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
    }

    Draft reopen() {
      return new Draft(Optional.of(on));
    }
  }
}
