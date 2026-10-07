package eu.openvalue.timesheet.adapter.out.draft;

import static org.assertj.core.api.Assertions.assertThat;

import eu.openvalue.timesheet.domain.Professional;
import eu.openvalue.timesheet.domain.Timesheet;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryTimesheetDraftsTest {

  private static final Professional JAN = Professional.withEmail("jan.jansen@openvalue.nl");
  private static final Professional PIET = Professional.withEmail("piet.pietersen@openvalue.nl");
  private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);

  private final InMemoryTimesheetDrafts drafts = new InMemoryTimesheetDrafts();

  @Test
  void findsASavedDraftByProfessionalAndMonth() {
    // Given
    var draft = Timesheet.start(MAY_2026, JAN, Optional.empty());

    // When
    drafts.save(draft);

    // Then
    assertThat(drafts.find(Professional.withEmail("Jan.Jansen@openvalue.nl"), MAY_2026)).containsSame(draft);
  }

  @Test
  void keepsDraftsOfOtherProfessionalsAndMonthsApart() {
    // Given
    drafts.save(Timesheet.start(MAY_2026, JAN, Optional.empty()));

    // When / Then
    assertThat(drafts.find(PIET, MAY_2026)).isEmpty();
    assertThat(drafts.find(JAN, MAY_2026.plusMonths(1))).isEmpty();
  }

  @Test
  void replacesTheDraftOfTheSameProfessionalAndMonth() {
    // Given
    drafts.save(Timesheet.start(MAY_2026, JAN, Optional.empty()));
    var replacement = Timesheet.start(MAY_2026, JAN, Optional.empty());

    // When
    drafts.save(replacement);

    // Then
    assertThat(drafts.find(JAN, MAY_2026)).containsSame(replacement);
  }
}
