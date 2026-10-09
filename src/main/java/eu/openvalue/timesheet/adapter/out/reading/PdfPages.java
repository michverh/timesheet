package eu.openvalue.timesheet.adapter.out.reading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Renders the pages of a PDF document as PNG images, so a vision model can read them as they look. Reading the text
 * of a PDF loses which column a value stands in, which is exactly what matters in a timesheet.
 */
final class PdfPages {

  /** Keeps unexpectedly long documents from flooding the model; timesheets are a page or two. */
  private static final int MAXIMUM_PAGES = 4;

  private PdfPages() {
  }

  /**
   * @param dotsPerInch the resolution to render at; higher reads small print better, but takes the model longer
   * @throws IOException when the content is not a PDF document
   */
  static List<byte[]> asPngImages(byte[] pdfContent, float dotsPerInch) throws IOException {
    try (var document = Loader.loadPDF(pdfContent)) {
      var renderer = new PDFRenderer(document);
      var images = new ArrayList<byte[]>();
      for (int page = 0; page < Math.min(document.getNumberOfPages(), MAXIMUM_PAGES); page++) {
        var image = renderer.renderImageWithDPI(page, dotsPerInch, ImageType.RGB);
        var png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        images.add(png.toByteArray());
      }
      return images;
    }
  }
}
