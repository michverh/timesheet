package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClientTimesheetDocumentTest {

  @Test
  void cannotBeEmpty() {
    assertThatThrownBy(() -> new ClientTimesheetDocument("acme.pdf", DocumentFormat.PDF, new byte[0]))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void needsAFileName() {
    assertThatThrownBy(() -> new ClientTimesheetDocument(" ", DocumentFormat.PDF, new byte[] {1}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotBeChangedThroughTheContentItWasGivenOrGives() {
    // Given
    var content = new byte[] {1, 2, 3};
    var document = new ClientTimesheetDocument("acme.pdf", DocumentFormat.PDF, content);

    // When
    content[0] = 9;
    document.content()[1] = 9;

    // Then
    assertThat(document.content()).containsExactly(1, 2, 3);
  }

  @Test
  void equalsADocumentWithTheSameNameFormatAndContent() {
    assertThat(new ClientTimesheetDocument("acme.pdf", DocumentFormat.PDF, new byte[] {1, 2}))
        .isEqualTo(new ClientTimesheetDocument("acme.pdf", DocumentFormat.PDF, new byte[] {1, 2}));
  }
}
