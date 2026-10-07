package eu.openvalue.timesheet.application;

import eu.openvalue.timesheet.domain.Timesheet;

/**
 * The company's naming of timesheet files, like {@code Urenverantwoording_2026-05_Jan_van_der_Berg.xlsx}.
 */
final class SpreadsheetFileName {

  private SpreadsheetFileName() {
  }

  static String of(Timesheet timesheet, String fileExtension) {
    var name = timesheet.professional().name().replace(' ', '_');
    return "Urenverantwoording_%s_%s.%s".formatted(timesheet.month(), name, fileExtension);
  }
}
