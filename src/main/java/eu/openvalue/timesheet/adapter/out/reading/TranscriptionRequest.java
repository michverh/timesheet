package eu.openvalue.timesheet.adapter.out.reading;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What to ask a language model to transcribe.
 *
 * @param prompt the instructions, including any document written out as text
 * @param images the images to transcribe, if any
 * @param schema the JSON schema the transcription must conform to
 */
public record TranscriptionRequest(String prompt, List<Image> images, Map<String, Object> schema) {

  public TranscriptionRequest {
    Objects.requireNonNull(prompt, "prompt");
    images = List.copyOf(images);
    Objects.requireNonNull(schema, "schema");
  }

  public record Image(String mediaType, byte[] content) {

    public Image {
      Objects.requireNonNull(mediaType, "mediaType");
      Objects.requireNonNull(content, "content");
    }
  }
}
