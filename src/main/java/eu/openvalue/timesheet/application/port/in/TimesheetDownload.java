package eu.openvalue.timesheet.application.port.in;

import java.util.Objects;

/**
 * A timesheet as a file to save.
 *
 * @param fileName the name to save the file under, like {@code Urenverantwoording_2026-05_Jan_Jansen} followed by
 *     the extension of its file format
 * @param mediaType the media type of the file format
 */
public record TimesheetDownload(String fileName, String mediaType, byte[] content) {

  public TimesheetDownload {
    Objects.requireNonNull(fileName, "fileName");
    Objects.requireNonNull(mediaType, "mediaType");
    Objects.requireNonNull(content, "content");
  }
}
