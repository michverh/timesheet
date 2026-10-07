package eu.openvalue.timesheet.application.port.out;

import java.util.Objects;

/**
 * The content of a spreadsheet, in a file format chosen by whoever created it.
 *
 * @param fileExtension the extension that identifies the file format, without leading dot
 * @param mediaType the media type of the file format
 */
public record SpreadsheetFile(String fileExtension, String mediaType, byte[] content) {

  public SpreadsheetFile {
    Objects.requireNonNull(fileExtension, "fileExtension");
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(content, "content");
  }
}
