package eu.openvalue.timesheet.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The file formats a client's own timesheet can be uploaded as: a screenshot, a PDF or an Excel workbook.
 */
public enum DocumentFormat {
  PNG("image/png", "png"),
  JPEG("image/jpeg", "jpg", "jpeg"),
  PDF("application/pdf", "pdf"),
  XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

  private final String mediaType;
  private final List<String> fileExtensions;

  DocumentFormat(String mediaType, String... fileExtensions) {
    this.mediaType = mediaType;
    this.fileExtensions = List.of(fileExtensions);
  }

  /**
   * @return the format a file name says it is in, or empty when it is not one of the accepted formats
   */
  public static Optional<DocumentFormat> ofFileName(String fileName) {
    var extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).filter(format -> format.fileExtensions.contains(extension)).findFirst();
  }

  public String mediaType() {
    return mediaType;
  }
}
