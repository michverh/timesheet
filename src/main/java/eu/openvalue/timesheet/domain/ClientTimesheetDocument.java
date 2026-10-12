package eu.openvalue.timesheet.domain;

import java.util.Arrays;
import java.util.Objects;

/**
 * The timesheet a client keeps of the professional's hours, as uploaded to import the hours worked into the company's
 * own timesheet.
 */
public final class ClientTimesheetDocument {

  private final String fileName;
  private final DocumentFormat format;
  private final byte[] content;

  /**
   * @throws IllegalArgumentException when the file name is blank or the document is empty
   */
  public ClientTimesheetDocument(String fileName, DocumentFormat format, byte[] content) {
    Objects.requireNonNull(fileName, "fileName");
    Objects.requireNonNull(format, "format");
    Objects.requireNonNull(content, "content");
    if (fileName.isBlank()) {
      throw new IllegalArgumentException("A client timesheet needs a file name");
    }
    if (content.length == 0) {
      throw new IllegalArgumentException("A client timesheet cannot be empty");
    }
    this.fileName = fileName.strip();
    this.format = format;
    this.content = content.clone();
  }

  public String fileName() {
    return fileName;
  }

  public DocumentFormat format() {
    return format;
  }

  public byte[] content() {
    return content.clone();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ClientTimesheetDocument document
        && fileName.equals(document.fileName)
        && format == document.format
        && Arrays.equals(content, document.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, format, Arrays.hashCode(content));
  }

  @Override
  public String toString() {
    return "ClientTimesheetDocument[%s, %s, %d bytes]".formatted(fileName, format, content.length);
  }
}
