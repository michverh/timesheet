package eu.openvalue.timesheet.adapter.out.reading;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ClaudePropertiesTest {

  @Test
  void setsTheConfiguredEffort() {
    assertThat(withEffort("low").effortToSet()).contains("low");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " ", "none", "NONE"})
  void setsNoEffortWhenConfiguredNotTo(String effort) {
    assertThat(withEffort(effort).effortToSet()).isEmpty();
  }

  private static ClaudeProperties withEffort(String effort) {
    return new ClaudeProperties(URI.create("https://api.anthropic.com"), "test-key", "claude-haiku-4-5", effort,
        Duration.ofMinutes(2));
  }
}
