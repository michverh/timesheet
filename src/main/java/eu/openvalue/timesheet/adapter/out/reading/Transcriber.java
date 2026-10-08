package eu.openvalue.timesheet.adapter.out.reading;

/**
 * A language model that transcribes a document into JSON of a given structure. Implementations only differ in how
 * they reach their model; what to ask and how to read the answer is the same for all of them.
 */
@FunctionalInterface
public interface Transcriber {

  /**
   * @return the transcription, as JSON conforming to the request's schema
   * @throws eu.openvalue.timesheet.application.port.out.ClientTimesheetUnreadableException when the model cannot be
   *     reached or does not answer
   */
  String transcribe(TranscriptionRequest request);
}
