package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class HourCategoryTest {

  @ParameterizedTest
  @EnumSource(names = {"OVERTIME", "STAND_BY"}, mode = EnumSource.Mode.EXCLUDE)
  void fillsTheWorkday(HourCategory category) {
    assertThat(category.fillsWorkday()).isTrue();
  }

  @ParameterizedTest
  @EnumSource(names = {"OVERTIME", "STAND_BY"})
  void comesOnTopOfTheWorkday(HourCategory category) {
    assertThat(category.fillsWorkday()).isFalse();
  }

  @Test
  void asksForAnExplanationOfTrainingAndOtherHoursOnly() {
    assertThat(HourCategory.values()).filteredOn(HourCategory::requiresExplanation)
        .containsExactly(HourCategory.TRAINING, HourCategory.OTHER);
  }
}
