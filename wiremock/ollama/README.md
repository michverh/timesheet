# Ollama stub

Stands in for Ollama while trying out the application, so reading a client's timesheet takes no time instead of a
minute or more. `compose.yaml` starts it on port 11435, where the application looks for Ollama by default.

It answers with what `qwen3-vl:8b-instruct` actually read from the documents in `examples/`, checked against the
documents themselves:

| Upload                    | Read                                                  |
|---------------------------|-------------------------------------------------------|
| `week-27-2026.png`        | 8 hours on 29 and 30 June and 2 and 3 July 2026       |
| `2026-08-jan-jansen.pdf`  | 8 hours on 16 days in August 2026, 128 in total       |
| `2026-08-jan-jansen.xlsx` | 8 hours on 16 days in August 2026, 128 in total       |

Any other document is answered with an error, so it ends up as a timesheet that could not be read.

To read any document with the model itself, point the application at Ollama, for instance in
`config/application.yaml`:

```yaml
timesheet:
  client-timesheet-reader:
    ollama:
      base-url: http://localhost:11434
```

## Recording readings again

`RecordedOllamaReadingsTest` fails when the requests for the examples no longer match the recordings, for instance
after changing how PDFs are rendered or spreadsheets transcribed. To record them again:

1. Run WireMock as a recording proxy in front of Ollama:
   `java -jar wiremock-standalone-3.13.2.jar --port 8091 --root-dir recordings --proxy-all http://localhost:11434 --record-mappings`
2. Start the application with `timesheet.client-timesheet-reader.ollama.base-url` set to `http://localhost:8091`.
3. Upload each example and check that what was read matches the table above.
4. Copy the `response` from each recording into the matching mapping in `mappings/`, and update its
   `matchesJsonPath` fragment when the request changed.
