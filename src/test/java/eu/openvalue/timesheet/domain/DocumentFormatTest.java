package eu.openvalue.timesheet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentFormatTest {

  @ParameterizedTest
  @CsvSource({
      "screenshot.png,     PNG",
      "photo.jpg,          JPEG",
      "photo.JPEG,         JPEG",
      "acme.may.2026.pdf,  PDF",
      "uren.xlsx,          XLSX"
  })
  void recognisesTheAcceptedFormatsByFileName(String fileName, DocumentFormat format) {
    assertThat(DocumentFormat.ofFileName(fileName)).contains(format);
  }

  @ParameterizedTest
  @ValueSource(strings = {"uren.xls", "uren.csv", "notes.txt", "no-extension", "archive.pdf.zip"})
  void doesNotAcceptOtherFormats(String fileName) {
    assertThat(DocumentFormat.ofFileName(fileName)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
      "PNG,  image/png",
      "JPEG, image/jpeg",
      "PDF,  application/pdf",
      "XLSX, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  })
  void knowsItsMediaType(DocumentFormat format, String mediaType) {
    assertThat(format.mediaType()).isEqualTo(mediaType);
  }
}
